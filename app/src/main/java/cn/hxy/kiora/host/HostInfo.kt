package cn.hxy.kiora.host

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build

@Suppress("DEPRECATION")
@SuppressLint("StaticFieldLeak")
object HostInfo {
    const val PACKAGE_NAME_QQ = HostTag.PACKAGE_QQ
    const val PACKAGE_NAME_TIM = HostTag.PACKAGE_TIM
    const val PACKAGE_NAME_WECHAT = HostTag.PACKAGE_WECHAT

    lateinit var hostContext: Context
        private set

    var packageName = ""
        private set

    var hostName: String = "Host App"
        private set

    lateinit var processName: String
        private set

    /**
     * 当前生效的宿主适配器。
     *
     * 由 [cn.hxy.kiora.common.ModuleLoader] 在宿主进程启动时装配；
     * 模块自身进程（设置页等）内为 null，因此访问前需判空。
     */
    var adapter: IHostAdapter? = null
        private set

    val isQQ: Boolean
        get() = packageName == PACKAGE_NAME_QQ
    val isTIM: Boolean
        get() = packageName == PACKAGE_NAME_TIM
    val isWeChat: Boolean
        get() = packageName == PACKAGE_NAME_WECHAT

    /** 是否处于任一受支持宿主的进程内。 */
    val isInHostProcess: Boolean
        get() = packageName in supportedPackages

    /** 全部已支持宿主的主包名，取值见 [HostTag]。 */
    val supportedPackages: Set<String>
        get() = HostTag.PACKAGES

    var versionCode: Long = 0
        private set
    var versionName: String = ""
        private set
    var moduleDataPath: String = ""
        private set

    /**
     * 装配宿主身份：包名 / 进程名 / 适配器。
     *
     * 由 [cn.hxy.kiora.common.ModuleLoader] 在宿主进程最早阶段一次性调用，
     * 三个字段合并成一次原子写入，避免漏配。模块自身进程（设置页等）不会调用，
     * 那些进程里 [packageName] 保持空串、[processName] 未初始化、[adapter] 为 null。
     */
    @JvmStatic
    fun bind(packageName: String, processName: String, adapter: IHostAdapter?) {
        this.packageName = packageName
        this.processName = processName
        this.adapter = adapter
    }

    /** 挂载宿主 Context 并读取版本信息；由 [cn.hxy.kiora.common.Startup] 在启动锚点命中后调用。 */
    @JvmStatic
    fun attachHostContext(context: Context) {
        hostContext = context

        runCatching {
            val pm = context.packageManager
            val info = pm.getPackageInfo(context.packageName, 0)
            versionCode =
                if (Build.VERSION.SDK_INT > 28)
                    info.longVersionCode
                else
                    info.versionCode.toLong()
            versionName = info.versionName.orEmpty()
            val appInfo = pm.getApplicationInfo(packageName, 0)
            hostName = pm.getApplicationLabel(appInfo).toString()
        }

        val externalDir = context.getExternalFilesDir(null)?.parentFile
        moduleDataPath = externalDir?.let { "${it.absolutePath}/Kiora/" }
            ?: "/storage/emulated/0/Android/data/${context.packageName}/Kiora/"
    }

}