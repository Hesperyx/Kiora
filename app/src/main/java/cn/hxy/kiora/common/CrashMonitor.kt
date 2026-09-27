package cn.hxy.kiora.common

import android.content.Intent
import android.util.Log
import cn.hxy.kiora.activity.CrashActivity
import cn.hxy.kiora.loader.hookapi.HookEngineManager
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.log.CrashReporter
import cn.hxy.kiora.utils.qq.HostInfo
import kotlin.system.exitProcess

object CrashMonitor {

    private var isInitialized = false

    fun init() {
        if (isInitialized) return
        isInitialized = true

        try {
            Thread::class.java.getDeclaredMethod(
                "setDefaultUncaughtExceptionHandler",
                Thread.UncaughtExceptionHandler::class.java
            ).hookBefore { param ->
                val originalHandler = param.args[0] as? Thread.UncaughtExceptionHandler
                if (originalHandler !is CrashHandler) {
                    param.args[0] = CrashHandler(originalHandler)
                }
            }

            val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (currentHandler !is CrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(CrashHandler(currentHandler))
            }
        } catch (t: Throwable) {
            HookEngineManager.engine.log(Log.ERROR, "[Kiora]", "CrashMonitor init failed: ", t)
        }
    }

    class CrashHandler(private val originalHandler: Thread.UncaughtExceptionHandler?) :
        Thread.UncaughtExceptionHandler {

        override fun uncaughtException(t: Thread, e: Throwable) {
            try {
                val result = CrashReporter.generateReport(t, e)

                val context = HostInfo.hostContext
                val intent = Intent(context, CrashActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra("blamedModule", result.blamedModule)
                    putExtra("exceptionType", result.exceptionType)
                    putExtra("summary", result.summary)
                    putExtra("reportPath", result.zipFile.absolutePath)
                    putExtra("stackTrace", result.stackTrace)
                    putExtra("hostName", HostInfo.hostName)
                }

                context.startActivity(intent)
                exitProcess(10)

            } catch (innerEx: Throwable) {
                innerEx.printStackTrace()
                originalHandler?.uncaughtException(t, e)
            }
        }
    }
}