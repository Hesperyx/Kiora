package dev.ujhhgtg.wekit.features

import cn.hxy.kiora.host.HostInfo
import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
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
 * 职责：只在微信宿主进程里跑；按当前进程收窄功能集合 → 解析 DexKit → 启动功能。
 *
 * **已知简化（与 WeKit 原版的差异）**：
 * 1. **没有 DexKit 结果缓存**。WeKit 用 `DexCacheManager` 把「key → 描述符」落盘，
 *    命中就跳过扫描；这里每次启动都重新扫。功能数量少时只是启动慢一点，
 *    上量之前必须补缓存（可参考 Kiora 的 `DexKitCache`）。
 * 2. **没有「新功能时间戳」/ 过期判定**，也没有「DexKit 破损时弹窗重扫」。
 * 3. 解析失败只记日志，不做用户可见提示。
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

            val sourceDir = HostInfo.hostContext.applicationInfo.sourceDir
            DexKitBridge.create(sourceDir).use { bridge ->
                DexResolutionContext.withResolutionContext(bridge) {
                    resolveDex(relevant)
                    startFeatures(relevant)
                }
            }
        }.onFailure { WeLogger.e(TAG, "WeKit 功能子系统加载失败", it) }
    }

    private fun resolveDex(features: List<BaseFeature>) {
        features.filterIsInstance<IResolveDex>().forEach { resolvable ->
            runCatching { DexResolutionContext.resolve(resolvable) }
                .onFailure {
                    WeLogger.e(
                        TAG,
                        "DexKit 解析失败：${(resolvable as BaseFeature).technicalPath}",
                        it
                    )
                }
        }
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
