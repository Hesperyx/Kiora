package cn.hxy.kiora.utils.log

import cn.hxy.kiora.host.HostEnv
import android.os.Build
import android.util.Log
import cn.hxy.kiora.BuildConfig
import cn.hxy.kiora.common.ModuleScope
import cn.hxy.kiora.hook.base.BaseHookItem
import cn.hxy.kiora.loader.hookapi.HookEngineManager
import cn.hxy.kiora.utils.io.FileUtils
import cn.hxy.kiora.host.HostInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogUtils {

    private const val TAG = "Kiora"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    /** 调试信息：仅进框架日志，不落盘。 */
    fun d(msg: String) {
        HookEngineManager.engine.log(Log.DEBUG, TAG, msg)
    }

    /** 一般信息：仅进框架日志，不落盘。 */
    fun i(msg: String) {
        HookEngineManager.engine.log(Log.INFO, TAG, msg)
    }

    /** 警告：仅进框架日志，不落盘。 */
    fun w(msg: String, t: Throwable? = null) {
        HookEngineManager.engine.log(Log.WARN, TAG, msg, t)
    }

    fun e(tag: String, t: Throwable) {
        e(tag, "Error occurred:", t)
    }

    /** 带自定义消息的错误日志（消息进框架日志与崩溃落盘，不再只剩 "Error occurred:"）。 */
    fun e(tag: String, msg: String, t: Throwable) {
        val stackTrace = Log.getStackTraceString(t)
        val saved = "[$tag] $msg\n$stackTrace"
        HookEngineManager.engine.log(Log.ERROR, "[$TAG] [$tag]", msg, t)

        saveCrashLog(tag, saved)
    }

    fun e(hookItem: BaseHookItem, t: Throwable) {
        e(hookItem.name, t)
    }

    fun getEnvironmentInfo(): String {
        val engine = HookEngineManager.engine
        return """
                === Environment Information ===
                Record Time: ${dateFormat.format(Date())}

                --- Device Information ---
                Brand: ${Build.BRAND}
                Model: ${Build.MODEL}
                Device: ${Build.DEVICE}
                Product: ${Build.PRODUCT}
                Manufacturer: ${Build.MANUFACTURER}
                Android Version: ${Build.VERSION.RELEASE}
                API Level: ${Build.VERSION.SDK_INT}
                Build ID: ${Build.ID}
                Fingerprint: ${Build.FINGERPRINT}
                Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}

                --- Xposed Framework Information ---
                Framework Name: ${engine.frameworkName}
                Framework Version: ${engine.frameworkVersion}
                Framework Version Code: ${engine.frameworkVersionCode}
                API Version: ${engine.apiLevel}

                --- Host Application Information ---
                Package Name: ${HostInfo.packageName}
                Version Name: ${HostInfo.versionName}
                Version Code: ${HostInfo.versionCode}

                --- Module Information ---
                Module Version Name: ${BuildConfig.VERSION_NAME}
                Module Version Code: ${BuildConfig.VERSION_CODE}

                ======================================
            """.trimIndent()
    }

    fun logEnvironment() {
        ModuleScope.launchIO("EnvLog") {
            val info = getEnvironmentInfo()
            val file = File("${HostInfo.moduleDataPath}global/log", "environment_info.txt")
            FileUtils.writeText(file, info)
        }
    }

    private fun saveCrashLog(tag: String, content: String) {
        val dir = HostEnv.currentDir
        ModuleScope.launchIO("WriteLog") {
            val time = dateFormat.format(Date())
            val logContent = "\n=== $time [$tag] ===\n$content\n"
            val file = File("${dir}log", "error_log.txt")
            FileUtils.writeText(file, logContent, true)
        }
    }
}
