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
            // 重入保护：若崩溃发生在「崩溃处理链路」自身（最典型是 CrashActivity
            // 渲染时又崩），绝不能再拉一次 CrashActivity —— 那会形成
            // 「拉起 CrashActivity → 崩 → 再拉起 → 再崩」的无限循环，宿主表现为
            // 反复闪退、完全打不开。此时直接把异常交还原处理器走系统崩溃流程。
            if (!handling.compareAndSet(false, true)) {
                originalHandler?.uncaughtException(t, e) ?: exitProcess(10)
                return
            }

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

        companion object {
            /**
             * 「正在处理崩溃」标记，跨实例共享。
             *
             * [CrashMonitor.init] 会按需重新包装默认处理器，因此不能用实例字段；
             * 必须是静态的，才能在「CrashActivity 自身又崩」时命中重入判断。
             */
            private val handling = java.util.concurrent.atomic.AtomicBoolean(false)
        }
    }
}