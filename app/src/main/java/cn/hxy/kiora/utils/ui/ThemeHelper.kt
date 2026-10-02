package cn.hxy.kiora.utils.ui

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import com.tencent.mobileqq.vas.theme.api.ThemeUtil
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.utils.qq.QQCurrentEnv

object ThemeHelper {
    private const val KEY_THEME_MODE = "theme_mode" 

    const val MODE_AUTO = 0
    const val MODE_LIGHT = 1
    const val MODE_DARK = 2

    fun getThemeMode(): Int {
        return QQCurrentEnv.globalPreference.getInt(KEY_THEME_MODE, MODE_AUTO)
    }

    fun setThemeMode(mode: Int) {
        QQCurrentEnv.globalPreference.edit { putInt(KEY_THEME_MODE, mode) }
    }

    /**
     * 当前是否夜间模式。
     *
     * AUTO 分支的关键约束：**QQ 专属类一律不能碰**。
     * `ThemeUtil.isInNightMode(...)` 的入参 `QQCurrentEnv.qQAppInterface` 会走到
     * `mqq.app.MobileQQ` —— 该包在微信出现之前不存在于进程里，微信侧调用会抛
     * `NoClassDefFoundError`。该异常会沿 Compose 组合一路冒泡，把弹窗（
     * [cn.hxy.kiora.ui.core.compatibility.XposedComposeDialog]）和兜底页
     * （`CrashActivity` 亦继承 `BaseComposeActivity`）一起拖崩，形成
     * 「微信一开就闪退、反复重启」。
     *
     * 因此按宿主分流：微信宿主直接退回系统 `uiMode`；QQ/TIM 才走 `ThemeUtil`，
     * 且用 `runCatching` 兜底（`NoClassDefFoundError` 是 Error，只有
     * `runCatching`/catch Throwable 能接到）。
     */
    fun isNightMode(): Boolean {
        return when (getThemeMode()) {
            MODE_LIGHT -> false
            MODE_DARK -> true
            else -> queryHostNightMode()
        }
    }

    private fun queryHostNightMode(): Boolean {
        if (HostInfo.isWeChat || !HostInfo.isInHostProcess) return systemNightMode()
        return runCatching {
            ThemeUtil.isInNightMode(QQCurrentEnv.qQAppInterface)
        }.getOrElse { systemNightMode() }
    }

    /** 读取宿主（或模块进程）当前系统深浅色。取不到时按浅色处理。 */
    private fun systemNightMode(): Boolean = runCatching {
        val mode = HostInfo.hostContext.resources.configuration.uiMode
        (mode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }.getOrElse { false }

    @Suppress("DEPRECATION")
    fun applyTheme(context: Context) {
        val isNight = isNightMode()
        val res = context.resources
        val config = Configuration(res.configuration)
        val mode = if (isNight) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        config.uiMode = mode or (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
        res.updateConfiguration(config, null)
    }
}