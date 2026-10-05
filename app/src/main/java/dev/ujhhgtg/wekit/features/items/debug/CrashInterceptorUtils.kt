package dev.ujhhgtg.wekit.features.items.debug

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.annotation.StringRes
import com.tencent.mm.ui.LauncherUI
import dev.ujhhgtg.wekit.R
import dev.ujhhgtg.wekit.utils.WeLogger
import dev.ujhhgtg.wekit.utils.android.copyToClipboard
import dev.ujhhgtg.wekit.utils.crash.CrashLogsManager
import java.io.File
import java.nio.file.Path

object CrashInterceptorUtils {

    fun isMainProcess(appContext: Context): Boolean {
        return runCatching {
            getProcessName() == appContext.packageName
        }.getOrDefault(false)
    }

    fun getProcessName(): String {
        return try {
            File("/proc/${Process.myPid()}/cmdline").readText().trim('\u0000')
        } catch (_: Throwable) {
            ""
        }
    }

    fun startActivityPolling(
        tag: String,
        maxRetries: Int = 20,
        initialDelayMs: Long = 1000L,
        onReady: (Activity) -> Unit,
    ) {
        val handler = Handler(Looper.getMainLooper())
        var retryCount = 0

        val runnable = object : Runnable {
            override fun run() {
                try {
                    val activity = LauncherUI.getInstance()
                    if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                        WeLogger.i(tag, "activity is ready")
                        onReady(activity)
                        return
                    }
                    retryCount++
                    if (retryCount < maxRetries) {
                        handler.postDelayed(this, 500)
                    } else {
                        WeLogger.w(tag, "max retries reached, giving up on showing dialog")
                    }
                } catch (e: Throwable) {
                    WeLogger.e(tag, "error in activity polling", e)
                }
            }
        }

        handler.postDelayed(runnable, initialDelayMs)
    }

    fun buildDisplayCrashInfo(
        crashInfo: String,
        overflowNotice: String,
        maxLength: Int = 15 * 1024,
    ): String {
        return if (crashInfo.length > maxLength) {
            crashInfo.take(maxLength) +
                    "\n\n=============================\n" +
                    overflowNotice + "\n" +
                    "============================="
        } else {
            crashInfo
        }
    }

    fun showPendingCrashDialog(
        activity: Activity,
        crashLogFile: Path,
        @StringRes titleSummaryRes: Int,
        @StringRes titleDetailRes: Int,
        clearPendingFlag: () -> Unit,
        extractSummary: (String) -> String
    ) {
        val crashInfo = CrashLogsManager.readCrashLog(crashLogFile) ?: return
        val summary = extractSummary(crashInfo)

        AlertDialog.Builder(activity)
            .setTitle(activity.getString(titleSummaryRes))
            .setMessage(summary)
            .setPositiveButton(activity.getString(R.string.debug_crash_view_details)) { _, _ ->
                val overflowNotice = activity.getString(R.string.debug_crash_log_truncated_notice)
                val displayInfo = buildDisplayCrashInfo(crashInfo, overflowNotice)
                AlertDialog.Builder(activity)
                    .setTitle(activity.getString(titleDetailRes))
                    .setMessage(displayInfo)
                    .setPositiveButton(activity.getString(R.string.debug_crash_copy_full_log)) { _, _ ->
                        val fullCrashInfo = CrashLogsManager.readFullCrashLog(crashLogFile) ?: crashInfo
                        copyToClipboard(activity, fullCrashInfo)
                        clearPendingFlag()
                    }
                    .setNegativeButton(activity.getString(R.string.debug_crash_ignore)) { _, _ ->
                        clearPendingFlag()
                    }
                    .show()
            }
            .setNegativeButton(activity.getString(R.string.debug_crash_ignore)) { _, _ ->
                clearPendingFlag()
            }
            .show()
    }
}