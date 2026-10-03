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
import org.luckypray.dexkit.DexKitBridge
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WeKit 血统功能子系统的加载入口。
 *
 * 职责：只在微信宿主进程里跑；按当前进程收窄功能集合 → 解析 DexKit（优先命中缓存）→ 启动功能。
 *
 * **缓存策略**：DexKit 结果落在 [WxDexCache]（JSON 文件，文件名含宿主+模块版本）。
 * 命中则跳过扫描；有缺失才对缺失的 feature 单独补查。改某个 feature 的 matcher 后
 * 旧缓存不会自动失效（未复刻 WeKit 的 methodHash），删缓存文件或升模块版本即可。
 *
 * **已知简化（与 WeKit 原版的差异）**：
 * 1. 无 methodHash 精确失效，无「破损时弹窗重扫」。
 * 2. 解析失败只记日志，不做用户可见提示。
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

            resolveDex(relevant)
            startFeatures(relevant)
        }.onFailure { WeLogger.e(TAG, "WeKit 功能子系统加载失败", it) }
    }

    /**
     * 读缓存恢复委托，只对「缓存缺失」的 feature 建桥补查并写回。
     *
     * 缓存全局键 = `technicalId->propertyName`（委托名在各 feature 内不唯一，必须加前缀）。
     */
    private fun resolveDex(features: List<BaseFeature>) {
        val resolvables = features.filterIsInstance<IResolveDex>()
        val toRescan = mutableListOf<IResolveDex>()

        resolvables.forEach { resolvable ->
            val feature = resolvable as BaseFeature
            val prefix = "${feature.technicalId}->"
            val slice = WxDexCache.cacheMap
                .filterKeys { it.startsWith(prefix) }
                .mapKeys { it.key.removePrefix(prefix) }

            val missing = runCatching { resolvable.loadFromCache(slice) }.getOrElse {
                // 单个 feature 恢复失败不应拖垮整批：当作全缺失，交由下方补查
                WeLogger.e(TAG, "缓存恢复失败：${feature.technicalPath}", it)
                return@forEach
            }

            if (missing.isNotEmpty()) {
                toRescan += resolvable
                WeLogger.d(TAG, "缓存未命中（${missing.size} 项），需要重扫：${feature.technicalPath}")
            }
        }

        if (toRescan.isEmpty()) {
            WeLogger.i(TAG, "DexKit 缓存全部命中，跳过扫描")
            return
        }

        // 本地缓存有缺失 → 先试云端拉取（GitHub Release 的预扫描报告），
        // 命中则跳过本地扫描；云端也缺的才落到本地 DexKit 扫描。
        val stillMissing = CloudDexResolver.resolve(toRescan)
        toRescan.retainAll(stillMissing.toSet())

        if (toRescan.isEmpty()) {
            WeLogger.i(TAG, "云端报告已补齐缺失项，跳过本地扫描")
            return
        }

        val sourceDir = HostInfo.hostContext.applicationInfo.sourceDir
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
                }
            }
        }
        WxDexCache.saveCache()
    }

    private fun startFeatures(features: List<BaseFeature>) {
        features.forEach { feature ->
            (feature as? SwitchFeature)?.loadPersistedState()
            runCatching { feature.startup() }
                .onFailure { WeLogger.e(TAG, "启动失败：${feature.technicalPath}", it) }
            // 逐条打状态：开关默认关闭时 isActive 为 false 属正常，不是失败
            WeLogger.i(TAG, "  ${feature.technicalPath} -> isActive=${feature.isActive}")
        }
        WeLogger.i(TAG, "已加载 ${features.size} 个 WeKit 功能（进程 ${TargetProcesses.currentName}）")
    }
}
