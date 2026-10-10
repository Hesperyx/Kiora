package dev.ujhhgtg.wekit.features

import cn.hxy.kiora.hook.MainHook
import cn.hxy.kiora.host.HostEnv
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
import java.io.File
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

    /**
     * 宿主日志的进度步长：完整审计每项都写 `feature_start_diag.log`，但宿主日志只在
     * 启用项与每 [LOG_STRIDE] 项时输出一行 —— 见 [startFeatures] 的实测说明。
     */
    private const val LOG_STRIDE = 25

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
                // Startup 已在解析就绪时调用过；这里再调一次是幂等的（MainHook 内部有 hookLoaded 闸门），
                // 只用来兜底「Startup 因异常没走到 loadHook」的极端情况。
                MainHook.loadHook()
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
                    onNeutral = { proceedWithoutScan(features, resolveMain) },
                    onNoActivity = {
                        WeLogger.w(TAG, "选择框无法弹出（Activity 销毁），直接启动已就绪的功能")
                        proceedWithoutScan(features, resolveMain)
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
                onNeutral = { proceedWithoutScan(features, resolveMain) },
                onNoActivity = {
                    WeLogger.w(TAG, "选择框无法弹出（Activity 销毁），直接启动已就绪的功能")
                    proceedWithoutScan(features, resolveMain)
                },
            )
        }.apply {
            name = "WxFeatureResolve"
            isDaemon = true
            start()
        }
    }

    /**
     * 用户选择「暂不查找」时仍然把**已经缓存好**的功能跑起来。
     *
     * 「暂不查找」只应表示「现在不扫描」，不能等同于「把整个微信端功能层关掉」——
     * 旧实现直接 return，于是只要主框架还有一条锚点没解析（例如升级后多出一项），
     * 用户点了「暂不查找」就整场会话都没有任何功能可用，表现就是「很多功能突然失效」。
     *
     * 这里改为挂载主框架并启动功能：每条 hook 在各自 onInit() 里判断锚点是否取到，
     * 取不到的那一项自己置为「当前环境不可用」，已经解析好的项照常工作。
     */
    private fun proceedWithoutScan(features: List<BaseFeature>, resolveMain: Boolean) {
        WeLogger.i(TAG, "用户选择暂不查找：跳过 DexKit 扫描，直接启动已就绪的功能")
        runCatching {
            if (resolveMain) MainHook.loadHook()
            startFeatures(features)
        }.onFailure { WeLogger.e(TAG, "暂不查找后启动功能失败", it) }
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
            // 只要尝试过解析就挂载主框架：每个 hook 项在各自的 onInit() 里判断锚点是否取到，
            // 单条锚点解析失败时那项自己会置为「当前环境不可用」，不会连累其余功能。
            // 旧逻辑用 all-or-nothing 闸门（任一键没解析就整层跳过 loadHook），
            // 会导致一条失效锚点把整个原生功能层一起拖死。
            if (resolveMain) MainHook.loadHook()
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
        // 导出集合必须覆盖 API 层：它们同样参与本次解析（见 load() 里的 collectMissing(apiFeatures + relevant)），
        // 但 activeFeatures 只含 items + startupBacked，漏掉 dexBacked 会让云端报告整层缺失服务层描述符。
        val exportPayload = (
            features +
                WeApiRegistry.dexBacked.filter { TargetProcesses.currentType in it.targetProcesses } +
                WeApiRegistry.startupBacked
            ).distinct().filterIsInstance<IResolveDex>()
        val exported = CloudDexResolver.exportLocalReport(exportPayload)
        if (exported != null) {
            progress.update("本地解析完成，报告已导出：\n$exported")
        }
        progress.dismiss()
        if (resolveMain) {
            if (!mainResolved) {
                WeLogger.w(
                    TAG,
                    "主框架 DexKit 未完全解析，仍挂载可用项：未解析的键将由对应 hook 的 onInit() 置为不可用",
                )
            }
            MainHook.loadHook()
        }
        startFeatures(features)
    }

    /**
     * 逐个启动功能。
     *
     * 真机（微信 8.0.78 / 3180）实测结论：**功能本身一切正常**。模块目录下的
     * `feature_start_diag.log` 记录 `startFeatures: 236 features` → `done: failed=0`，全程 69 ms；
     * 但同一次冷启动里 LSPosed 的日志子系统被这批瞬时爆发打死 —— 日志文件
     * `/data/adb/lspd/log/modules_*.log` 与 logcat **同时**停在爆发中的第 138 行，此后
     * 8,619 行窗口内任何进程都不再出现 `LSPosedFramework` 行。先前几轮看到的
     * 「循环停在第 139 项」「汇总行缺失」全是日志丢尾，不是代码缺陷。
     *
     * 所以这里的策略是「日志要少、落盘要全」：
     * 1. 单项的 `loadPersistedState()` + `startup()` 全部包在 [runCatching]；
     * 2. 每项的完整审计（index / technicalPath / isActive）只写 `feature_start_diag.log`，
     *    宿主日志只留「启用项 + 每 [LOG_STRIDE] 项 + 失败 + 汇总」，把突发压到几十行；
     * 3. 诊断落盘与宿主日志解耦 —— 宿主日志调用本身出问题也留得下现场。
     *
     * 上游 `FeaturesLoader.loadFeatures` 是裸调 `startup()`，一个功能出问题就会拖垮其余功能。
     */
    private fun startFeatures(features: List<BaseFeature>) {
        val thread = Thread.currentThread()
        val previousHandler = thread.uncaughtExceptionHandler
        thread.setUncaughtExceptionHandler { t, e ->
            diag("uncaught on ${t.name}: ${e.stackTraceToString()}")
            runCatching { previousHandler?.uncaughtException(t, e) }
        }
        diag("startFeatures: ${features.size} features", reset = true)
        WeLogger.i(TAG, "开始启动 ${features.size} 个 WeKit 功能（进程 ${TargetProcesses.currentName}）")

        var failed = 0
        var active = 0
        features.forEachIndexed { index, feature ->
            try {
                (feature as? SwitchFeature)?.loadPersistedState()
                runCatching { feature.startup() }.onFailure {
                    failed++
                    diag("startup failed at $index (${feature.javaClass.name}): ${it.stackTraceToString()}")
                    WeLogger.e(TAG, "启动失败（第 $index 个）：${feature.technicalPath}", it)
                }
                if (feature.isActive) active++
                logStarted(index, feature)
            } catch (t: Throwable) {
                diag("iteration $index (${feature.javaClass.name}) threw: ${t.stackTraceToString()}")
                runCatching { WeLogger.e(TAG, "启动第 $index 个功能时异常（${feature.javaClass.name}）", t) }
            }
        }
        diag("startFeatures done: failed=$failed, active=$active")
        WeLogger.i(
            TAG,
            "已加载 ${features.size} 个 WeKit 功能（进程 ${TargetProcesses.currentName}），启用 $active 个，启动失败 $failed 个",
        )
    }

    /**
     * 单项启动结果：完整审计落盘；宿主日志只在「已启用」或每 [LOG_STRIDE] 项时输出一行，
     * 避免冷启动瞬间几百行日志把 LSPosed 日志子系统打崩（真机已实测到该现象）。
     */
    private fun logStarted(index: Int, feature: BaseFeature) {
        val active = runCatching { feature.isActive }.getOrDefault(false)
        val path = runCatching { feature.technicalPath }.getOrDefault(feature.javaClass.simpleName)
        diag("  [$index] $path -> isActive=$active")
        if (active || index % LOG_STRIDE == 0) {
            runCatching { WeLogger.i(TAG, "  [$index] $path -> isActive=$active") }
                .onFailure { diag("log failed at $index (${feature.javaClass.name}): ${it.stackTraceToString()}") }
        }
    }

    /**
     * 与宿主日志解耦的诊断落盘。宿主日志调用本身可能就是循环静默中断的原因，所以
     * 审计内容一律先落模块目录下的 `feature_start_diag.log`；`reset = true` 用于每次
     * 启动循环开头截断上一轮的记录，避免文件无限增长。
     */
    private fun diag(line: String, reset: Boolean = false) {
        runCatching {
            val file = File(HostEnv.currentDir, "feature_start_diag.log")
            val text = "${System.currentTimeMillis()} $line\n"
            if (reset) file.writeText(text) else file.appendText(text)
        }
    }
}
