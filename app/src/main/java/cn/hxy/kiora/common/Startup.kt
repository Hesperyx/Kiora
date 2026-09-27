package cn.hxy.kiora.common

import android.content.Context
import android.util.Log
import com.tencent.common.app.BaseApplicationImpl
import dalvik.system.BaseDexClassLoader
import cn.hxy.kiora.BuildConfig
import cn.hxy.kiora.hook.MainHook
import cn.hxy.kiora.lifecycle.Parasitics
import cn.hxy.kiora.loader.hookapi.HookEngineManager
import cn.hxy.kiora.loader.hookapi.Unhook
import cn.hxy.kiora.utils.dexkit.DexKitCache
import cn.hxy.kiora.utils.dexkit.DexKitFinder
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.qq.HostInfo
import cn.hxy.kiora.utils.reflect.ClassUtils
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean

object Startup {
    private var isInit = AtomicBoolean(false)
    private var hasCapturedTinker = AtomicBoolean(false)

    @JvmStatic
    fun init(initialLoader: ClassLoader) {
        runCatching {
            initialLoader.loadClass("com.tencent.common.app.QFixApplicationImplProxy")
                .getDeclaredMethod("attachBaseContext", Context::class.java)
        }.getOrNull()
            ?.let { hookQFixAttach(it) }
            ?: doRealStartup(initialLoader)
    }

    private fun hookQFixAttach(attach: Method) {
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

        try {
            BaseApplicationImpl::class.java.getDeclaredMethod("onCreate").hookAfter { param ->
                if (isInit.compareAndSet(false, true)) {
                    val hostContext = param.thisObject as Context
                    HostInfo.init(hostContext)
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

                    if (DexKitCache.initCache() && DexKitFinder.missingKeys().isEmpty()) {
                        MainHook.loadHook()
                    } else {
                        DexKitFinder.doFind()
                    }
                }
            }
        } catch (th: Throwable) {
            HookEngineManager.engine.log(Log.ERROR, "[Kiora]", "doRealStartup 发生异常", th)
        }
    }
}