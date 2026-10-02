package cn.hxy.kiora.lifecycle

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Resources
import android.content.res.loader.ResourcesLoader
import android.content.res.loader.ResourcesProvider
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.PersistableBundle
import android.util.Log
import cn.hxy.kiora.BuildConfig
import cn.hxy.kiora.R
import cn.hxy.kiora.activity.BaseComposeActivity
import cn.hxy.kiora.common.ModuleLoader
import cn.hxy.kiora.loader.hookapi.HookEngineManager
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.utils.reflect.ClassUtils
import cn.hxy.kiora.utils.reflect.callMethod
import cn.hxy.kiora.utils.reflect.callStaticMethod
import cn.hxy.kiora.utils.reflect.findMethod
import cn.hxy.kiora.utils.reflect.getObject
import cn.hxy.kiora.utils.reflect.getObjectByTypeOrNull
import cn.hxy.kiora.utils.reflect.getStaticObject
import cn.hxy.kiora.utils.reflect.setObject
import cn.hxy.kiora.utils.reflect.toClass
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

@SuppressLint("DiscouragedPrivateApi", "PrivateApi")
@Suppress("DEPRECATION")
object Parasitics {
    private const val ACTIVITY_PROXY_INTENT = "ACTIVITY_PROXY_INTENT"

    /** 同一个 [Resources] 实例最多尝试注入几次（成功后置 -1 不再进入）。 */
    private const val MAX_INJECT_ATTEMPTS = 3

    /**
     * 每个 [Resources] 实例的注入状态：`>0` = 已尝试次数，`-1` = 已确认可解析。
     *
     * 必须**按实例**记账而不是用一个全局布尔：微信进程里同时存在多个 `Resources`
     * （Application 的、各 Activity 的），给 A 注入成功不代表 B 也能取到 0x44 包。
     */
    private val resState = java.util.WeakHashMap<Resources, IntArray>()

    /**
     * 占位 Activity（宿主包内真实声明的 Activity）。
     *
     * 由 [resolveStubActivity] 在 [initForStubActivity] 早期、**安装 PMS 代理之前**
     * 解析一次并缓存。不再写死 QQ 的 `CameraPreviewActivity` —— 微信下该类不存在，
     * 写死会让寄生启动直接失败。
     */
    @Volatile
    private var stubDefaultActivity: String? = null

    private val moduleLoader by lazy { ClassUtils.moduleClassLoader }
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    /**
     * 按宿主适配器给的优先级选出可用的占位 Activity。
     *
     * 取第一个能被 PMS 解析到的候选；全部解析失败时退回第一个候选并告警
     * （保持改造前「总是有一个占位值」的行为，避免 QQ 侧出现回退）。
     */
    private fun resolveStubActivity(ctx: Context): String? {
        val candidates = HostInfo.adapter?.stubActivityCandidates.orEmpty()
        if (candidates.isEmpty()) {
            HookEngineManager.engine.log(
                Log.WARN, "[Kiora]",
                "当前宿主未提供占位 Activity 候选，寄生 Activity 启动将被跳过"
            )
            return null
        }

        val pm = ctx.packageManager
        val resolved = candidates.firstOrNull { name ->
            runCatching {
                pm.getActivityInfo(ComponentName(ctx.packageName, name), 0)
            }.isSuccess
        }

        if (resolved == null) {
            HookEngineManager.engine.log(
                Log.WARN, "[Kiora]",
                "占位 Activity 候选均无法解析: $candidates，回退使用第一个"
            )
        }
        return resolved ?: candidates.first()
    }

    private object ResourcesLoaderHolderApi30 {
        var sResourcesLoader: ResourcesLoader? = null
    }

    private fun isTargetActivity(className: String?): Boolean {
        if (className == null) return false
        if (DynamicActivityRegistry.contains(className)) return true
        if (!className.startsWith(BuildConfig.APPLICATION_ID)) return false

        return runCatching {
            val targetClass = moduleLoader.loadClass(className)
            BaseComposeActivity::class.java.isAssignableFrom(targetClass)
        }.getOrElse { false }
    }

    fun initForStubActivity(ctx: Context) {
        runCatching {
            // 必须在安装 PMS 代理之前解析，否则查询会绕回我们自己的代理。
            stubDefaultActivity = resolveStubActivity(ctx)

            val activityThreadClass = "android.app.ActivityThread".toClass
            val currentActivityThread =
                activityThreadClass.callStaticMethod("currentActivityThread") ?: return

            // Hook Instrumentation (负责实例化 Activity)
            val instrumentation =
                currentActivityThread.getObject("mInstrumentation") as Instrumentation
            if (instrumentation !is ProxyInstrumentation) {
                currentActivityThread.setObject(
                    "mInstrumentation",
                    ProxyInstrumentation(instrumentation)
                )
            }

            // Hook H (主线程 Handler，负责消息分发)
            val mH = currentActivityThread.getObject("mH") as Handler
            val originalCallback = mH.getObjectByTypeOrNull<Handler.Callback>(Handler::class.java)

            mH.setObject("mCallback", Handler.Callback { msg ->
                val handledObj = when (msg.what) {
                    100 -> msg.obj // LAUNCH_ACTIVITY
                    159 -> msg.obj // EXECUTE_TRANSACTION
                    else -> null
                }
                handledObj?.let { handleLaunchMessage(it, msg.what) }
                originalCallback?.handleMessage(msg) ?: false
            }, Handler::class.java)

            hookIActivityManager()
            hookIPackageManager(ctx, currentActivityThread)
        }
    }

    private fun hookIActivityManager() {
        val singletonInstance = resolveActivityManagerSingleton() ?: return
        val mInstance = singletonInstance.getObject("mInstance", "android.util.Singleton".toClass)

        val iamInterface = if (Build.VERSION.SDK_INT >= 29) {
            "android.app.IActivityTaskManager".toClass
        } else {
            "android.app.IActivityManager".toClass
        }

        val proxy = Proxy.newProxyInstance(moduleLoader, arrayOf(iamInterface)) { _, method, args ->
            // 拦截 startActivity
            if ("startActivity" == method.name) {
                args?.indexOfFirst { it is Intent }?.takeIf { it != -1 }?.let { index ->
                    val raw = args[index] as Intent
                    val component = raw.component
                    if (component != null && HostInfo.packageName == component.packageName &&
                        isTargetActivity(component.className)
                    ) {
                        stubDefaultActivity?.let { stub ->
                            args[index] = Intent().apply {
                                setClassName(component.packageName, stub)
                                putExtra(ACTIVITY_PROXY_INTENT, raw)
                                flags = raw.flags
                            }
                        }
                    }
                }
            }
            invokeOriginal(mInstance, method, args)
        }

        singletonInstance.setObject("mInstance", proxy, "android.util.Singleton".toClass)
    }

    private fun hookIPackageManager(ctx: Context, currentActivityThread: Any) {
        val sPackageManager = currentActivityThread.getObject("sPackageManager")
        val ipmInterface = "android.content.pm.IPackageManager".toClass

        val proxy = Proxy.newProxyInstance(
            ipmInterface.classLoader,
            arrayOf(ipmInterface)
        ) { _, method, args ->
            // 拦截 getActivityInfo，伪造信息防止 PMS 报错
            if ("getActivityInfo" == method.name && args?.isNotEmpty() == true) {
                val component = args[0] as? ComponentName
                if (component != null && HostInfo.packageName == component.packageName &&
                    isTargetActivity(component.className)
                ) {
                    val flags = (args[1] as? Number)?.toLong() ?: 0L
                    return@newProxyInstance CounterfeitActivityInfoFactory.makeProxyActivityInfo(
                        component.className,
                        flags
                    )
                }
            }
            invokeOriginal(sPackageManager, method, args)
        }

        currentActivityThread.setObject("sPackageManager", proxy)
        ctx.packageManager.setObject("mPM", proxy)
    }

    // 统一处理反射调用异常
    private fun invokeOriginal(target: Any, method: Method, args: Array<Any?>?): Any? {
        return try {
            method.invoke(target, *args.orEmpty())
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    private fun resolveActivityManagerSingleton(): Any? {
        // 依次尝试不同 Android 版本的 AMS 单例位置 (静态字段)
        return runCatching {
            // Android 10+
            val atmClass = "android.app.ActivityTaskManager".toClass
            val singleton = atmClass.getStaticObject("IActivityTaskManagerSingleton")
            "android.util.Singleton".toClass.findMethod { name = "get" }.invoke(singleton)
            singleton
        }.recoverCatching {
            // Android 8.0 - 9.0
            "android.app.ActivityManager".toClass.getStaticObject("IActivityManagerSingleton")
        }.recoverCatching {
            // Android 8.0 以下
            "android.app.ActivityManagerNative".toClass.getStaticObject("gDefault")
        }.getOrNull()
    }

    // ================= Intent 还原逻辑 =================

    private fun handleLaunchMessage(obj: Any, what: Int) {
        if (what == 100) {
            // ActivityClientRecord
            val intent = obj.getObject("intent") as? Intent ?: return
            unwrapIntent(intent)?.let { obj.setObject("intent", it) }
        } else if (what == 159) {
            // ClientTransaction
            val callbacks =
                runCatching { obj.callMethod("getCallbacks") as? List<*> }.getOrNull() ?: return
            callbacks.forEach { item ->
                if (item != null && item.javaClass.name.contains("LaunchActivityItem")) {
                    val intent = item.getObject("mIntent") as? Intent ?: return
                    val original = unwrapIntent(intent) ?: return

                    item.setObject("mIntent", original)

                    // Android 12+ 修复 ActivityThread 内部缓存
                    if (Build.VERSION.SDK_INT >= 31) {
                        fixActivityClientRecordForApi31(obj, original)
                    }
                }
            }
        }
    }

    private fun fixActivityClientRecordForApi31(transaction: Any, originalIntent: Intent) {
        runCatching {
            val token = transaction.callMethod("getActivityToken") as? IBinder ?: return
            val activityThread =
                "android.app.ActivityThread".toClass.callStaticMethod("currentActivityThread")
            val acr = activityThread?.callMethod("getLaunchingActivity", token) ?: return
            acr.setObject("intent", originalIntent)
        }
    }

    private fun unwrapIntent(intent: Intent): Intent? {
        return runCatching {
            val clone = intent.clone() as Intent
            clone.extras?.classLoader = ClassUtils.hostClassLoader
            if (clone.hasExtra(ACTIVITY_PROXY_INTENT)) {
                clone.getParcelableExtra<Intent>(ACTIVITY_PROXY_INTENT)?.apply {
                    extras?.classLoader = moduleLoader
                }
            } else {
                null
            }
        }.getOrNull()
    }

    // ================= 资源注入逻辑 =================

    fun injectModuleResources(res: Resources?) {
        if (res == null) return
        // 记账：value 为剩余可尝试次数；成功后置 -1 永不再试。
        val state = resState.getOrPut(res) { intArrayOf(0) }
        if (state[0] < 0 || state[0] >= MAX_INJECT_ATTEMPTS) return
        state[0]++

        val path = ModuleLoader.getMODULE_PATH()

        // 模块资源包 id 是 **0x44**（见 :app:processDebugResources 产出的 R.txt，
        // 如 drawable/ic_launcher = 0x44050020），不是宿主的 0x7F —— 这样才有意避开撞号。
        //
        // 但 API 30+ 的 ResourcesLoader 只把加载的 APK 当「同包叠加层」：包 id 与宿主对不上时
        // 这个包根本不会出现在宿主的 ResTable 里，宿主侧查资源会直接抛
        //   `No package ID 44 found for resource ID 0x44050020`
        // （实测 logcat 硬证据；微信设置页会因此在绑定期抛异常、整片列表不渲染）。
        // 所以这里**不分版本**都补一次 addAssetPath，把模块的 resources.arsc 真正注册进宿主
        // ResTable。两个版本分支都需要它，ResourcesLoader 只是 API 30+ 的额外补充。
        addModuleAssetPath(res, path)

        if (Build.VERSION.SDK_INT >= 30) {
            val loader = ResourcesLoaderHolderApi30.sResourcesLoader ?: run {
                runCatching {
                    val pfd =
                        ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
                    val provider = ResourcesProvider.loadFromApk(pfd)
                    ResourcesLoader().apply { addProvider(provider) }
                }.getOrNull()?.also { ResourcesLoaderHolderApi30.sResourcesLoader = it }
            }

            val task = Runnable {
                try {
                    loader?.let { res.addLoaders(it) }
                } catch (e: IllegalArgumentException) {
                    if (e.message?.contains("Cannot modify resource loaders") == true) addModuleAssetPath(
                        res,
                        path
                    )
                }
            }
            if (Looper.myLooper() == Looper.getMainLooper()) task.run() else mainHandler.post(task)
        }

        // 判据必须落在「图标能不能真的取到」上：`getString(R.string.app_name)` **不能**当判据 ——
        // 原生 ResTable 缺包时只打一条 warning 就返回 TYPE_NULL，Java 侧 getString 仍可能成功
        // （实测返回 ""/"Kiora"），用它做「已注入」守卫会永远误判早退，addAssetPath 一次都跑不到。
        // 取到图标即认为本实例已完成注册，记账置 -1 不再重复尝试。
        if (runCatching { res.getDrawable(R.drawable.ic_launcher, null) }.isSuccess) state[0] = -1
    }

    /**
     * 把模块 APK 作为一个**新的资源包**加进给定的 [Resources]（其内部 AssetManager）。
     *
     * 模块包 id 是 0x44，与宿主 0x7F 不冲突，所以宿主里 `getDrawable(0x44050020)` /
     * `setImageResource(0x44050020)` 都能取到模块自己的图 —— 这也是 QQ 侧
     * [cn.hxy.kiora.qq.hook.entry.QQSettingInject] 直接把 `R.drawable.ic_launcher`
     * 交给宿主处理器后能显示出图标的前提。
     */
    private fun addModuleAssetPath(res: Resources, path: String) {
        runCatching {
            val am = res.assets
            val candidates = AssetManager::class.java.declaredMethods
                .filter { it.name.startsWith("addAssetPath") }
            val m = candidates.firstOrNull {
                it.name == "addAssetPath" && it.parameterTypes.size == 1 &&
                        it.parameterTypes[0] == String::class.java
            } ?: candidates.firstOrNull {
                it.parameterTypes.size == 1 && it.parameterTypes[0] == String::class.java
            } ?: run {
                LogUtils.w("宿主 AssetManager 没有可用的 addAssetPath(String)，模块资源无法注册")
                return@runCatching
            }
            m.isAccessible = true
            m.invoke(am, path)
        }.onFailure {
            LogUtils.w("向宿主 Resources 注册模块资源失败: ${it.javaClass.name}: ${it.message}")
        }
    }

    // ================= Instrumentation 代理类 =================

    private class ProxyInstrumentation(private val mBase: Instrumentation) : Instrumentation() {
        override fun newActivity(cl: ClassLoader, className: String, intent: Intent): Activity {
            if (DynamicActivityRegistry.contains(className)) {
                DynamicActivityRegistry.getActivityClass(className)?.let { return it.newInstance() }
            }
            return try {
                mBase.newActivity(cl, className, intent)
            } catch (_: Exception) {
                // 宿主加载失败，切换到模块 ClassLoader
                Parasitics::class.java.classLoader!!.loadClass(className).newInstance() as Activity
            }
        }

        override fun callActivityOnCreate(activity: Activity, icicle: Bundle?) {
            injectModuleResources(activity.resources)
            if (icicle != null && isTargetActivity(activity.javaClass.name)) icicle.classLoader =
                moduleLoader
            mBase.callActivityOnCreate(activity, icicle)
        }

        override fun callActivityOnCreate(
            activity: Activity,
            icicle: Bundle?,
            persistentState: PersistableBundle?
        ) {
            injectModuleResources(activity.resources)
            if (icicle != null && isTargetActivity(activity.javaClass.name)) icicle.classLoader =
                moduleLoader
            mBase.callActivityOnCreate(activity, icicle, persistentState)
        }
    }
}