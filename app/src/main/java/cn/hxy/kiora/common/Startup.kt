package cn.hxy.kiora.common

import android.content.Context
import android.util.Log
import dalvik.system.BaseDexClassLoader
import cn.hxy.kiora.BuildConfig
import cn.hxy.kiora.bootstrap.DexKitBootstrap
import cn.hxy.kiora.hook.MainHook
import cn.hxy.kiora.lifecycle.Parasitics
import cn.hxy.kiora.loader.hookapi.HookEngineManager
import cn.hxy.kiora.loader.hookapi.Unhook
import cn.hxy.kiora.utils.dexkit.DexKitCache
import cn.hxy.kiora.utils.dexkit.DexKitFinder
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.utils.reflect.ClassUtils
import dev.ujhhgtg.wekit.features.WxFeatureLoader
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean

object Startup {
    private var isInit = AtomicBoolean(false)
    private var hasCapturedTinker = AtomicBoolean(false)

    @JvmStatic
    fun init(initialLoader: ClassLoader) {
        // 热更锚点从适配器取，不再写死 QFix 的类名：
        // QQ 系 -> QFixApplicationImplProxy.attachBaseContext
        // 微信   -> com.tencent.mm.app.Application.attachBaseContext (Tinker)
        val hotfix = HostInfo.adapter?.hotfixAnchor
        val hotfixName = hotfix?.methodName

        val hotfixMethod = if (hotfix == null || hotfixName == null) {
            null
        } else {
            // 仍在 initialLoader 上查找：此刻 ClassUtils.hostClassLoader 尚未赋值，
            // 不能用 toClass。
            runCatching {
                initialLoader.loadClass(hotfix.className)
                    .getDeclaredMethod(hotfixName, *hotfix.paramTypes.toTypedArray())
            }.getOrNull()
        }

        if (hotfixMethod != null) {
            hookHotfixAttach(hotfixMethod)
        } else {
            // 宿主无热更通路（或解析失败）：直接用初始 Loader 启动。
            doRealStartup(initialLoader)
        }
    }

    private fun hookHotfixAttach(attach: Method) {
        val constructorUnhooks = mutableListOf<Unhook>()

        attach.hookBefore {
            BaseDexClassLoader::class.java.declaredConstructors.forEach { ctor ->
                val unhook = ctor.hookAfter { param ->
                    val loader = param.thisObject as ClassLoader
                    val loaderStr = loader.toString()
                    if (loaderStr.contains(BuildConfig.APPLICATION_ID)) return@hookAfter

                    if ((loaderStr.contains("com.tencent.") ||
                                loaderStr.contains("TinkerClassLoader") ||
                                loaderStr.contains("DelegateLastClassLoader"))
                        && !hasCapturedTinker.get()
                    ) {
                        hasCapturedTinker.set(true)

                        HookEngineManager.engine.log(
                            Log.INFO,
                            "[Kiora]",
                            "捕获到热更 ClassLoader: $loader"
                        )
                        doRealStartup(loader)
                    }
                }
                constructorUnhooks.add(unhook)
            }
        }

        attach.hookAfter { param ->
            constructorUnhooks.forEach { it.unhook() }
            constructorUnhooks.clear()

            if (!hasCapturedTinker.get()) {
                val context = param.args[0] as Context
                doRealStartup(context.classLoader)
            }
        }
    }

    @Synchronized
    private fun doRealStartup(realClassLoader: ClassLoader) {
        if (isInit.get()) return
        ClassUtils.hostClassLoader = realClassLoader
        ModuleLoader.injectClassLoader(realClassLoader)
        CrashMonitor.init()

        val anchor = HostInfo.adapter?.startupAnchor
        if (anchor == null) {
            HookEngineManager.engine.log(
                Log.WARN,
                "[Kiora]",
                "宿主 ${HostInfo.packageName} 未适配启动锚点，模块在此进程不生效"
            )
            return
        }

        val startupMethod = anchor.resolveMethod()
        if (startupMethod == null) {
            HookEngineManager.engine.log(
                Log.ERROR,
                "[Kiora]",
                "启动锚点未命中: ${anchor.className}#${anchor.methodName}，" +
                        "模块在当前宿主不生效"
            )
            return
        }

        try {
            startupMethod.hookAfter { param ->
                if (isInit.compareAndSet(false, true)) {
                    val hostContext = param.thisObject as Context
                    HostInfo.attachHostContext(hostContext)
                    ModuleUpdateMonitor.init(hostContext)

                    if (HostInfo.processName == HostInfo.packageName) {

                        HookEngineManager.engine.log(
                            Log.INFO,
                            "[Kiora]",
                            "宿主启动 (Loader: $realClassLoader)"
                        )
                        LogUtils.logEnvironment()
                    }

                    Parasitics.initForStubActivity(hostContext)
                    Parasitics.injectModuleResources(hostContext.resources)

                    // initCache 的返回值只表示「有没有读出一份缓存」，不能当作
                    // 「能否加载 hook」的前提。宿主若一个 DexKit 任务都没有，
                    // 缓存文件永远不会生成；照旧写法会一直走 doFind() 分支，
                    // 而 doFind() 在「无缺失」时立刻返回 —— 结果是
                    // MainHook.loadHook() 永远不被调用，模块静默失效。
                    DexKitCache.initCache()
                    if (DexKitFinder.missingKeys().isEmpty()) {
                        MainHook.loadHook()
                    } else {
                        DexKitBootstrap.doFind()
                    }

                    // 微信侧的 WeKit 血统功能子系统。DexKit 扫描较慢，放 IO 线程避免阻塞
                    // 启动锚点回调；内部自行判断宿主与进程，非微信宿主直接返回。
                    ModuleScope.launchIO("WxFeatureLoader") { WxFeatureLoader.load() }
                }
            }
        } catch (th: Throwable) {
            HookEngineManager.engine.log(Log.ERROR, "[Kiora]", "doRealStartup 发生异常", th)
        }
    }
}