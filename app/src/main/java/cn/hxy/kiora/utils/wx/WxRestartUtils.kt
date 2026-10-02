package cn.hxy.kiora.utils.wx

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Process
import android.widget.Toast
import cn.hxy.kiora.common.ModuleScope
import cn.hxy.kiora.utils.qq.HostInfo
import kotlin.system.exitProcess

/**
 * 微信宿主重启。
 *
 * ## 为什么不复用 `utils/qq/AppRestartUtils`
 *
 * 两者只是名字像，通路完全不通用 —— 沿用设计文档 §11.5 的约定：
 * `utils/qq` 下的内部实现不动，微信侧新建 `utils/wx` 一套，两套并存、靠 adapter 分发。
 *
 * - QQ 那套的核心是**宿主自带的重启加载页**
 *   `com.tencent.mobileqq.login.restart.MainProcessRestartLoadingActivity`：
 *   把当前界面截图经 Binder 塞进去当加载背景，重启对用户是连续的。
 * - 微信**没有对应的 Activity**。对 8.0.78 清单做全量 `aapt2 dump xmltree` 后确认：
 *   2097 个 Activity 里名字含 `restart` / `relaunch` 的**零命中**
 *   （`Loading` 命中的全是 `AppBrandPreLoadingUI` 一类小程序项，与重启无关）。
 *   所以微信侧只能「拉起入口 + 结束进程」，提示也得自己弹。
 *
 * 把 QQ 的实现泛化成"通用重启"再让微信用，等于让微信去 `startActivity` 一个
 * 别的包里的类；反过来在 QQ 实现里塞微信分支，也是把两套不通用的东西捆死。
 *
 * ## 为什么必须重启
 *
 * DexKit 首轮查找时 hook 还没挂上，找到的类/方法只是写进了缓存文件；
 * 只有让宿主**重新走一遍启动链**，`DexKitCache.initCache()` 才能读到这份缓存、
 * `DexKitFinder.missingKeys()` 才会为空、`MainHook.loadHook()` 才会真正执行。
 */
object WxRestartUtils {

    /** 拉起入口的延时，留一点时间让 Toast 先入队。 */
    private const val RELAUNCH_DELAY_MS = 100L

    /** 结束自己的延时，排在拉起之后，避免进程先死、闹钟还没登记。 */
    private const val EXIT_DELAY_MS = 200L

    fun restartApp(context: Context, tipText: String = "重启中...") {
        if (tipText.isNotEmpty()) {
            // 微信没有重启加载页，提示只能自己弹。Toast 由系统渲染，
            // 进程随后退出不影响它继续显示。
            runCatching { Toast.makeText(context, tipText, Toast.LENGTH_SHORT).show() }
        }
        killSiblingProcesses(context)
        relaunchViaAlarm(context)
    }

    /**
     * 结束同 uid 的其它进程（`com.tencent.mm:push` / `:appbrand0` 等）。
     *
     * 这些进程在缓存落盘前就已启动，不重启它们会一直停在「没有锚点」的状态。
     * 这不是可选项：微信侧有 hook 明确以 `:appbrand0` 为目标进程
     * （对标 WA 的 `targetProcess = [MAIN_PROCESS, APP_BRAND_0]`），
     * 只重启主进程会漏掉它们。
     *
     * 同 uid 才能 kill 成功 —— 宿主进程内 `Process.myUid()` 就是微信的 uid，
     * 所以这里列出来的必然全是微信自己的进程，不会误伤别的应用。
     */
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

    /**
     * 用 AlarmManager 拉起入口，随后结束自己。
     *
     * 不能直接 `startActivity`：本进程紧接着就要退出，直接拉起的 Activity 会随
     * 进程一起消失。挂成系统闹钟后，即便本进程已死，AMS 仍会按点把微信重新拉起。
     *
     * 入口交给 `getLaunchIntentForPackage` 解析 —— 8.0.78 清单里
     * `com.tencent.mm.ui.LauncherUI` 带 `MAIN` / `LAUNCHER`、`exported=true`、
     * `launchMode=1`，正是该 API 的目标。
     */
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
