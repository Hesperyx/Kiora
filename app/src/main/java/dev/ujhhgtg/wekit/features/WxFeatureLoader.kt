package dev.ujhhgtg.wekit.features

import cn.hxy.kiora.hook.MainHook
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.utils.dexkit.DexKitFinder
import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
import dev.ujhhgtg.wekit.features.api.core.WeApiRegistry
import dev.ujhhgtg.wekit.dexkit.cache.CloudDexResolver
import dev.ujhhgtg.wekit.dexkit.cache.WxDexCache
import dev.ujhhgtg.wekit.dexkit.resolution.DexHostMetadata
import dev.ujhhgtg.wekit.dexkit.resolution.DexResolutionContext
import dev.ujhhgtg.wekit.features.core.ApiFeature
import dev.ujhhgtg.wekit.features.core.BaseFeature
import dev.ujhhgtg.wekit.features.core.SwitchFeature
import dev.ujhhgtg.wekit.features.items.beautify.BeautifyConversationList
import dev.ujhhgtg.wekit.features.items.chat.ConversationGrouping
import dev.ujhhgtg.wekit.features.items.system.SafeMode
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

        // 上游 FeaturesLoader.loadFeatures() 的同款迁移：把「布局美化」时代的浮动标签页选择
        // 落成独立的 tab 风格偏好。即使分组功能关闭、或它的 DexKit 缓存待重建也必须执行 ——
        // 否则事后读旧值会把新引入的美化开关误当成升级前用户的真实选择。
        if (TargetProcesses.isInMain) {
            ConversationGrouping.migrateTabStyle(BeautifyConversationList.isLayoutBeautificationEnabled)
        }

        val currentProcess = TargetProcesses.currentType
        val registered = WxFeatureRegistry.all.filter { currentProcess in it.targetProcesses }
        val startupApis = WeApiRegistry.startupBacked.filter { currentProcess in it.targetProcesses }

        // 安全模式（模块根目录存在 safe_mode.flag）：只启动 API 层，跳过全部 items 功能。
        // 用于某个功能把微信搞挂后的自救 —— 用来关开关的入口本身不能是被关掉的那些功能。
        // 与上游 FeaturesLoader.loadFeatures() 的语义一致。
        val safeMode = SafeMode.isEnabled
        val relevant = if (safeMode) registered.filterIsInstance<ApiFeature>() else registered
        if (safeMode) {
            WeLogger.i(
                TAG,
                "安全模式已启用：跳过 ${registered.size - relevant.size} 个功能，仅加载 ${relevant.size} 个 API 项",
            )
        }
        val activeFeatures = relevant + startupApis

        runCatching {
            System.loadLibrary("dexkit")
            // 缓存已并入主框架 DexKitCache，Startup 里已 initCache；这里不再重复 load，
            // 避免覆盖主框架异步扫描中途写入的 cacheMap 实例。

            // 服务层（API 功能）必须一起进解析管线：
            // items 功能的 matcher 会跨对象引用 API 委托（如 WeMessageApi.classChattingDataAdapter.data），
            // 服务层委托没解析过的话 `.data` 直接 NPE。API 层永远参与 loadFromCache（恢复描述符）。
            val apiFeatures = WeApiRegistry.dexBacked + startupApis
            val toRescan = collectMissing(apiFeatures + relevant)
            val resolveMain = DexKitFinder.unresolvedKeys().isNotEmpty()

            if (activeFeatures.isEmpty() && !resolveMain) return@runCatching

            if (toRescan.isEmpty() && !resolveMain) {
                // 缓存全部命中，直接启动（主框架 hook 已由 Startup 挂载）
                WeLogger.i(TAG, "DexKit 缓存全部命中，跳过扫描")
                startFeatures(activeFeatures)
            } else if (!TargetProcesses.isInMain) {
                // 非主进程：缓存缺失时不能直接 startFeatures。APPBRAND 等进程里 DexKit
                // 委托还没恢复，启动功能只会让 enable() 抛错后被 runCatching 吞掉。
                // 这里直接返回，等主进程写好缓存后下次再读。
                WeLogger.i(TAG, "非主进程（${TargetProcesses.currentName}），缓存缺失 ${toRescan.size} 项，等待主进程解析")
                return@runCatching
            } else {
                // 主进程缓存缺失 → 异步弹选择框（不阻塞，等 Activity 就绪）
                promptAndResolve(activeFeatures, toRescan, resolveMain)
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
    private fun promptAndResolve(features: List<BaseFeature>, toRescan: List<IResolveDex>, resolveMain: Boolean) {
        Thread {
            // 等「稳定」的前台 Activity（最多 20 秒）。
            // 只判非 null 不够：微信冷启动先出 WeChatSplashActivity，弹框挂上去
            // 立刻就 WindowLeaked（闪屏销毁弹窗跟着死）。要求同一实例连续 5 次
            // （约 1 秒）存活才算稳定，跳过一闪而过的闪屏。
            val deadline = System.currentTimeMillis() + 20_000
            var candidate: android.app.Activity? = null
            var stableCount = 0
            while (System.currentTimeMillis() < deadline && stableCount < 5) {
                val current = cn.hxy.kiora.host.HostEnv.activity
                if (current != null && !current.isFinishing && !current.isDestroyed && current === candidate) {
                    stableCount++
                } else {
                    candidate = current
                    stableCount = 0
                }
                Thread.sleep(200)
            }

            val activity = candidate?.takeIf { !it.isFinishing && !it.isDestroyed }
            if (activity == null || stableCount < 5) {
                // Activity 一直未就绪 → 弹不出选择框。先延时重试一次；仍失败就本次跳过，
                // 不静默自动扫描或自动云端。
                WeLogger.w(TAG, "Activity 未稳定（candidate=${candidate?.javaClass?.simpleName}），3 秒后重试")
                Thread.sleep(3_000)
                val retry = cn.hxy.kiora.host.HostEnv.activity
                if (retry == null || retry.isFinishing || retry.isDestroyed) {
                    // 没拿到稳定 Activity 就无法弹「云端/本地」选择框。不静默自动扫描或自动
                    // 云端，避免又出现用户没点却开始解析的行为；本次跳过，下次启动再弹。
                    WeLogger.w(TAG, "Activity 仍未就绪，本次跳过 DexKit 解析，等待下次启动")
                    return@Thread
                }
                showChoiceDialog(
                    title = "Kiora DexKit",
                    message = "检测到 DexKit 结果缺失。\n" +
                        "云端拉取命中可跳过本地扫描（更快），本地扫描则完整解析，暂不查找本次跳过。",
                    positiveText = "云端拉取",
                    negativeText = "本地扫描",
                    neutralText = "暂不查找",
                    activity = retry,
                    onChoice = { cloud -> runResolve(features, toRescan, resolveMain, cloud, retry) },
                    onNeutral = {
                        WeLogger.i(TAG, "用户选择暂不查找，本次跳过 DexKit 解析")
                    },
                    onNoActivity = {
                        WeLogger.w(TAG, "选择框无法弹出（Activity 销毁），本次跳过 DexKit 解析")
                    },
                )
                return@Thread
            }

            // 弹选择框，用户选择后回调执行
            WeLogger.i(TAG, "Activity 就绪（${activity.javaClass.simpleName}），弹出「云端拉取 / 本地扫描」选择框")
            showChoiceDialog(
                title = "Kiora DexKit",
                message = "检测到 DexKit 结果缺失。\n" +
                    "云端拉取命中可跳过本地扫描（更快），本地扫描则完整解析，暂不查找本次跳过。",
                positiveText = "云端拉取",
                negativeText = "本地扫描",
                neutralText = "暂不查找",
                activity = activity,
                onChoice = { cloud -> runResolve(features, toRescan, resolveMain, cloud, activity) },
                onNeutral = {
                    WeLogger.i(TAG, "用户选择暂不查找，本次跳过 DexKit 解析")
                },
                onNoActivity = {
                    WeLogger.w(TAG, "选择框无法弹出（Activity 销毁），本次跳过 DexKit 解析")
                },
            )
        }.apply {
            name = "WxFeatureResolve"
            isDaemon = true
            start()
        }
    }

    /** 用户选择后，切后台线程执行解析（含网络 + DexKit 扫描，不能阻塞主线程）。 */
    private fun runResolve(
        features: List<BaseFeature>,
        toRescan: List<IResolveDex>,
        resolveMain: Boolean,
        useCloud: Boolean,
        activity: android.app.Activity?,
    ) {
        WeLogger.i(TAG, "用户选择：${if (useCloud) "云端拉取" else "本地扫描"}")
        Thread {
            resolveAndStart(features, toRescan, resolveMain, useCloud, activity)
        }.apply {
            name = "WxFeatureResolveWork"
            isDaemon = true
            start()
        }
    }

    /** 执行云端/本地解析，完成后 startFeatures。 */
    private fun resolveAndStart(
        features: List<BaseFeature>,
        toRescanInput: List<IResolveDex>,
        resolveMain: Boolean,
        useCloud: Boolean,
        activity: android.app.Activity?,
    ) {
        val toRescan = toRescanInput.toMutableList()

        val progress = showProgressDialog(
            "Kiora DexKit",
            if (useCloud) "正在从云端拉取 DexKit 结果…" else "正在本地扫描 DexKit 结果…",
            activity,
        )

        var mainResolved = !resolveMain
        var remaining = toRescan

        if (useCloud) {
            // 云端报告只拉取一次：同时恢复主框架键与 WeKit feature。失败或未覆盖都由
            // resolveAll 返回剩余列表，下面统一走本地扫描。
            val result = CloudDexResolver.resolveAll(toRescan, includeMain = resolveMain) { message, current, total ->
                progress.updateProgress(message, current, total)
            }
            remaining = result.remainingFeatures.toMutableList()
            mainResolved = result.mainResolved
            if (result.cloudFailed) {
                progress.update("云端拉取失败，转为本地扫描…")
            } else if (remaining.isEmpty() && mainResolved) {
                progress.update("云端拉取成功，DexKit 结果已补齐")
            } else {
                progress.update("云端拉取完成，剩余 ${remaining.size} 项转本地扫描")
            }
        }

        // 用户选本地、或云端失败/未覆盖主框架时，先补主框架键。
        if (resolveMain && !mainResolved) {
            progress.update("正在本地扫描主框架 DexKit 结果…")
            DexKitFinder.runFindUnresolved(HostInfo.hostContext) { key ->
                progress.update("本地扫描主框架：$key")
            }
            mainResolved = DexKitFinder.unresolvedKeys().isEmpty()
        }

        if (remaining.isEmpty()) {
            if (!mainResolved) {
                val unresolved = DexKitFinder.unresolvedKeys()
                WeLogger.e(TAG, "DexKit 解析结束，主框架仍有 ${unresolved.size} 个键未找到")
                progress.update("DexKit 解析结束，主框架仍有 ${unresolved.size} 个键未找到")
            } else {
                progress.update("DexKit 解析完成")
            }
            progress.dismiss()
            if (resolveMain && mainResolved) MainHook.loadHook()
            startFeatures(features)
            return
        }

        val sourceDir = HostInfo.hostContext.applicationInfo.sourceDir
        var done = 0
        // 解析顺序：服务层（API）按 WeApiRegistry 的依赖顺序在前，items 功能在后。
        // items 的 matcher 会跨对象引用 API 委托（.data），顺序错了就是 NPE。
        val apiRank = (WeApiRegistry.dexBacked + WeApiRegistry.startupBacked).withIndex().associate { (i, f) -> f to i }
        val ordered = remaining.sortedBy { resolvable ->
            (resolvable as? BaseFeature)?.let { apiRank[it] } ?: Int.MAX_VALUE
        }

        val phaseText = when {
            !useCloud -> "本地扫描"
            CloudDexResolver.lastAttemptFailed -> "云端拉取失败，转为本地解析"
            else -> "云端未覆盖 ${ordered.size} 项，转为本地解析"
        }
        progress.update("$phaseText（0/${ordered.size}）…")
        DexKitBridge.create(sourceDir).use { bridge ->
            DexResolutionContext.withResolutionContext(bridge, DexHostMetadata.currentAndroidHost()) {
                ordered.forEach { resolvable ->
                    val feature = resolvable as BaseFeature
                    val resolved = runCatching { DexResolutionContext.resolve(resolvable) }
                    // 无论成败都收集已解析出的描述符 —— 半截结果进缓存，
                    // 下次只补查失败的委托，不用整功能重扫。
                    resolvable.collectDescriptors().forEach { (key, value) ->
                        if (value.isNotEmpty()) {
                            WxDexCache.cacheMap["${feature.technicalId}->$key"] = value
                        }
                    }
                    resolved.onFailure {
                        WeLogger.e(TAG, "DexKit 解析失败：${feature.technicalPath}", it)
                        return@forEach
                    }
                    done++
                    progress.updateProgress(
                        "$phaseText（$done/${ordered.size}）：${feature.technicalPath}",
                        done,
                        ordered.size,
                    )
                }
            }
        }
        WxDexCache.saveCache()
        progress.update("DexKit 本地解析完成（成功 $done/${ordered.size}）")
        val exported = CloudDexResolver.exportLocalReport()
        if (exported != null) {
            progress.update("本地解析完成，报告已导出：\n$exported")
        }
        progress.dismiss()
        if (resolveMain && mainResolved) {
            MainHook.loadHook()
        } else if (resolveMain) {
            WeLogger.e(TAG, "主框架 DexKit 未完全解析，跳过 MainHook.loadHook")
        }
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
