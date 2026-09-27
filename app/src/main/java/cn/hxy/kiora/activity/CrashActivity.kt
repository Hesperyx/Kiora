package cn.hxy.kiora.activity

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import cn.hxy.kiora.ui.core.theme.KioraTheme
import cn.hxy.kiora.ui.pages.crash.CrashScreen
import cn.hxy.kiora.utils.qq.AppRestartUtils
import cn.hxy.kiora.utils.qq.HostInfo

class CrashActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                restartApp()
            }
        })

        val blamedModule = intent.getStringExtra("blamedModule") ?: "Unknown"
        val exceptionType = intent.getStringExtra("exceptionType") ?: "Error"
        val summary = intent.getStringExtra("summary") ?: ""
        val reportPath = intent.getStringExtra("reportPath") ?: ""
        val stackTrace = intent.getStringExtra("stackTrace") ?: ""
        val hostName = intent.getStringExtra("hostName") ?: HostInfo.hostName

        setContent {
            KioraTheme(isDarkTheme) {
                CrashScreen(
                    hostName = hostName,
                    blamedModule = blamedModule,
                    exceptionType = exceptionType,
                    summary = summary,
                    reportPath = reportPath,
                    stackTrace = stackTrace,
                    onRestart = ::restartApp,
                )
            }
        }
    }

    private fun restartApp() {
        AppRestartUtils.restartApp(this, "恢复中...")
    }

}
