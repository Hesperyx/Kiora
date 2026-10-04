package dev.ujhhgtg.wekit.features

import cn.hxy.kiora.host.HostInfo
import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
import dev.ujhhgtg.wekit.dexkit.cache.CloudDexResolver
import dev.ujhhgtg.wekit.dexkit.cache.WxDexCache
import dev.ujhhgtg.wekit.dexkit.resolution.DexResolutionContext
import dev.ujhhgtg.wekit.features.core.BaseFeature
import dev.ujhhgtg.wekit.features.core.SwitchFeature
import dev.ujhhgtg.wekit.utils.TargetProcesses
import dev.ujhhgtg.wekit.utils.WeLogger
import dev.ujhhgtg.wekit.utils.android.showChoiceDialog
import dev.ujhhgtg.wekit.utils.android.showProgressDialog
import org.luckypray.dexkit.DexKitBridge
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WeKit 血统功能子系统的加载入口。
 *
 * 职责：只在微信宿主进程里跑；按当前进程收窄功能集合 → 解析 DexKit（优先命中缓存）→ 启动功能。
 *
 * **缓存策略**：DexKit 结果落在 [WxDexCache]（JSON 文件，文件名含宿主+模块版本）。
 * 命中则跳过扫描；有缺失才对缺失的 feature 单独补查。
 *
 * **解析链路（全异步，不阻塞 IO 线程）**：
 * 1. 读缓存，命中则直接 startFeatures
 * 2. 缓存缺失 → 延迟等 Activity 就绪后弹「云端拉取 / 本地扫描」选择框
 * 3. 用户选择（或超时）后执行云端/本地，完成后补 startFeatures
 */
object WxFeatureLoader {

    private const val TAG = "WxFeatureLoader"

    private val loaded = AtomicBoolean(false)

    fun load() {
        if (!HostInfo.isWeChat) return
        if (!loaded.compareAndSet(false, true)) return

        val currentProcess = TargetProcesses.currentType
        val relevant = WxFeatureRegistry.all.filter { currentProcess in it.targetProcesses }
        if (relevant.isEmpty()) return

        runCatching {
            System.loadLibrary("dexkit")
            WxDexCache.initCache()

            val toRescan = collectMissing(relevant)
            if (toRescan.isEmpty()) {
                // 缓存全部命中，直接启动
                WeLogger.i(TAG, "DexKit 缓存全部命中，跳过扫描")
                startFeatures(relevant)
            } else if (!TargetProcesses.isInMain) {
                // 非主进程：缓存缺失跳过，等主进程写好缓存下次再读
                WeLogger.i(TAG, "非主进程（${TargetProcesses.currentName}），缓存缺失 ${toRescan.size} 项，跳过解析")
                startFeatures(relevant)
            } else {
                // 主进程缓存缺失 → 异步弹选择框（不阻塞，等 Activity 就绪）
                promptAndResolve(relevant, toRescan)
            }
        }.onFailure { WeLogger.e(TAG, "WeKit 功能子系统加载失败", it) }
    }

    /** 读缓存，返回「缓存缺失、需要解析」的功能列表。 */
    private fun collectMissing(features: List<BaseFeature>): List<IResolveDex> {
        val resolvables = features.filterIsInstance<IResolveDex>()
        val toRescan = mutableListOf<IResolveDex>()

        resolvables.forEach { resolvable ->
            val feature = resolvable as BaseFeature
            val prefix = "${feature.technicalId}->"
            val slice = WxDexCache.cacheMap
                .filterKeys { it.startsWith(prefix) }
                .mapKeys { it.key.removePrefix(prefix) }

            val missing = runCatching { resolvable.loadFromCache(slice) }.getOrElse {
                WeLogger.e(TAG, "缓存恢复失败：${feature.technicalPath}", it)
                return@forEach
            }

            if (missing.isNotEmpty()) {
                toRescan += resolvable
                WeLogger.d(TAG, "缓存未命中（${missing.size} 项）：${feature.technicalPath}")
            }
        }
        return toRescan
    }

    /**
     * 弹选择框（异步，等 Activity 就绪后弹），用户选择后执行解析，完成后补 startFeatures。
     *
     * 用后台线程轮询等待 Activity 就绪（最长 20 秒），就绪后弹选择框；
     * 用户选择云端/本地，执行解析，最后 startFeatures（此时功能才真正 enable）。
     */
    private fun promptAndResolve(features: List<BaseFeature>, toRescan: List<IResolveDex>) {
        Thread {
            // 等 Activity 就绪（最多 20 秒）
            val deadline = System.currentTimeMillis() + 20_000
            while (System.currentTimeMillis() < deadline &&
                cn.hxy.kiora.host.HostEnv.activity == null
            ) {
                Thread.sleep(200)
            }

            // Activity 一直未就绪 → 默认云端拉取（弹不出选择框时仍尽力解析）
            val activityReady = cn.hxy.kiora.host.HostEnv.activity != null
            if (!activityReady) {
                WeLogger.w(TAG, "Activity 未就绪，默认云端拉取")
                resolveAndStart(features, toRescan, useCloud = true)
                return@Thread
            }

            // 弹选择框，用户选择后回调执行
            showChoiceDialog(
                title = "Kiora DexKit",
                message = "检测到 ${toRescan.size} 个功能缺少 DexKit 结果。\n" +
                    "云端拉取命中可跳过本地扫描（更快），本地扫描则完整解析。",
                positiveText = "云端拉取",
                negativeText = "本地扫描",
                onChoice = { cloud ->
                    // 回调在主线程，解析含网络 + DexKit 扫描，必须切后台线程
                    Thread {
                        resolveAndStart(features, toRescan, cloud)
                    }.apply {
                        name = "WxFeatureResolveWork"
                        isDaemon = true
                        start()
                    }
                },
            )
        }.apply {
            name = "WxFeatureResolve"
            isDaemon = true
            start()
        }
    }

    /** 执行云端/本地解析，完成后 startFeatures。 */
    private fun resolveAndStart(
        features: List<BaseFeature>,
        toRescanInput: List<IResolveDex>,
        useCloud: Boolean,
    ) {
        val toRescan = toRescanInput.toMutableList()

        val progress = showProgressDialog("Kiora DexKit", "正在从云端拉取 DexKit 结果…")
        val stillMissing = if (useCloud) {
            CloudDexResolver.resolve(toRescan)
        } else {
            toRescan // 用户选本地：跳过云端
        }
        toRescan.retainAll(stillMissing.toSet())

        if (toRescan.isEmpty()) {
            WeLogger.i(TAG, "云端报告已补齐缺失项，跳过本地扫描")
            progress.update("已从云端拉取 DexKit 结果，跳过本地扫描")
            progress.dismiss()
            startFeatures(features)
            return
        }

        val sourceDir = HostInfo.hostContext.applicationInfo.sourceDir
        var done = 0
        val phaseText = when {
            !useCloud -> "本地扫描"
            CloudDexResolver.lastAttemptFailed -> "云端拉取失败，转为本地解析"
            else -> "云端未覆盖 ${toRescan.size} 项，转为本地解析"
        }
        progress.update("$phaseText（0/${toRescan.size}）…")
        DexKitBridge.create(sourceDir).use { bridge ->
            DexResolutionContext.withResolutionContext(bridge) {
                toRescan.forEach { resolvable ->
                    val feature = resolvable as BaseFeature
                    runCatching { DexResolutionContext.resolve(resolvable) }
                        .onFailure {
                            WeLogger.e(TAG, "DexKit 解析失败：${feature.technicalPath}", it)
                            return@forEach
                        }
                    resolvable.collectDescriptors().forEach { (key, value) ->
                        WxDexCache.cacheMap["${feature.technicalId}->$key"] = value
                    }
                    done++
                    progress.updateProgress(
                        "$phaseText（$done/${toRescan.size}）：${feature.technicalPath}",
                        done,
                        toRescan.size,
                    )
                }
            }
        }
        WxDexCache.saveCache()
        progress.update("DexKit 本地解析完成（$done/${toRescan.size}）")
        val exported = CloudDexResolver.exportLocalReport()
        if (exported != null) {
            progress.update("本地解析完成，报告已导出：\n$exported")
        }
        progress.dismiss()
        startFeatures(features)
    }

    private fun startFeatures(features: List<BaseFeature>) {
        features.forEach { feature ->
            (feature as? SwitchFeature)?.loadPersistedState()
            runCatching { feature.startup() }
                .onFailure { WeLogger.e(TAG, "启动失败：${feature.technicalPath}", it) }
            WeLogger.i(TAG, "  ${feature.technicalPath} -> isActive=${feature.isActive}")
        }
        WeLogger.i(TAG, "已加载 ${features.size} 个 WeKit 功能（进程 ${TargetProcesses.currentName}）")
    }
}
