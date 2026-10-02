package cn.hxy.kiora.wx.util

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Process
import android.widget.Toast
import cn.hxy.kiora.common.ModuleScope
import cn.hxy.kiora.host.HostInfo
import kotlin.system.exitProcess

/**
 * 微信宿主重启。
 *
 * 不复用 `utils/qq/AppRestartUtils`：那套依赖 QQ 专有的重启加载页，微信清单里
 * 没有对应 Activity（8.0.78 的 2097 个 Activity 中 `restart` / `relaunch` 零命中）。
 * 重启本身必须做：DexKit 首轮只写缓存，得让宿主重走一遍启动链才会加载 hook。
 */
object WxRestartUtils {

    /** 拉起入口的延时，留一点时间让 Toast 先入队。 */
    private const val RELAUNCH_DELAY_MS = 100L

    /** 结束自己的延时，排在拉起之后，避免进程先死、闹钟还没登记。 */
    private const val EXIT_DELAY_MS = 200L

    fun restartApp(context: Context, tipText: String = "重启中...") {
        if (tipText.isNotEmpty()) {
            // 微信没有重启加载页，提示只能自己弹
            runCatching { Toast.makeText(context, tipText, Toast.LENGTH_SHORT).show() }
        }
        killSiblingProcesses(context)
        relaunchViaAlarm(context)
    }

    /** 结束同 uid 的其它进程；微信侧有 hook 以 `:appbrand0` 为目标进程，只重启主进程会漏掉。 */
    private fun killSiblingProcesses(context: Context) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
        val myPid = Process.myPid()
        val myUid = Process.myUid()
        am.runningAppProcesses.orEmpty().forEach { info ->
            if (info.uid == myUid && info.pid != myPid) {
                Process.killProcess(info.pid)
            }
        }
    }

    /** 用闹钟拉起入口再结束自己：本进程马上要退出，直接 startActivity 的界面会随之消失。 */
    private fun relaunchViaAlarm(context: Context) {
        val pkg = HostInfo.packageName.ifEmpty { HostInfo.PACKAGE_NAME_WECHAT }
        val launch = context.packageManager.getLaunchIntentForPackage(pkg) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val pending = PendingIntent.getActivity(
            context,
            0,
            launch,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarm.set(AlarmManager.RTC, System.currentTimeMillis() + RELAUNCH_DELAY_MS, pending)

        ModuleScope.launchDelayed(EXIT_DELAY_MS) { exitProcess(0) }
    }
}
