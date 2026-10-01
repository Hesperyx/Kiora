package cn.hxy.kiora.utils.qq

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import cn.hxy.kiora.host.HostTag
import cn.hxy.kiora.host.IHostAdapter

@Suppress("DEPRECATION")
@SuppressLint("StaticFieldLeak")
object HostInfo {
    const val PACKAGE_NAME_QQ = HostTag.PACKAGE_QQ
    const val PACKAGE_NAME_TIM = HostTag.PACKAGE_TIM
    const val PACKAGE_NAME_WECHAT = HostTag.PACKAGE_WECHAT

    lateinit var hostContext: Context
        private set

    var packageName = ""

    var hostName: String = "Host App"
        private set

    lateinit var processName: String

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

    /** 装配宿主适配器；由 ModuleLoader 在初始化早期调用。 */
    @JvmStatic
    fun installAdapter(a: IHostAdapter?) {
        adapter = a
    }

    @JvmStatic
    fun init(context: Context) {
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