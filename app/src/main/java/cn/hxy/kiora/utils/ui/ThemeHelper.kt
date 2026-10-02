package cn.hxy.kiora.utils.ui

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import cn.hxy.kiora.host.HostEnv
import cn.hxy.kiora.host.HostInfo

object ThemeHelper {
    private const val KEY_THEME_MODE = "theme_mode" 

    const val MODE_AUTO = 0
    const val MODE_LIGHT = 1
    const val MODE_DARK = 2

    fun getThemeMode(): Int {
        return HostEnv.globalPreference.getInt(KEY_THEME_MODE, MODE_AUTO)
    }

    fun setThemeMode(mode: Int) {
        HostEnv.globalPreference.edit { putInt(KEY_THEME_MODE, mode) }
    }

    /**
     * 当前是否夜间模式。
     *
     * AUTO 分支的约束：**宿主专属类一律不能在这里碰**。QQ 的 `ThemeUtil.isInNightMode(...)`
     * 会走到 `mqq.app.MobileQQ` —— 该包在微信进程里不存在，直接引用会抛
     * `NoClassDefFoundError`（Error，`catch Exception` 接不住），并沿 Compose 组合一路冒泡，
     * 把弹窗与兜底页一起拖崩，表现为「微信一开就闪退」。
     *
     * 所以这里只问适配器：QQ/TIM 由 `QQFamilyHostAdapter.hostNightMode` 走 `ThemeUtil`
     * （内部 `runCatching` 兜底），微信与模块自身进程返回 null → 退回系统 `uiMode`。
     */
    fun isNightMode(): Boolean {
        return when (getThemeMode()) {
            MODE_LIGHT -> false
            MODE_DARK -> true
            else -> queryHostNightMode()
        }
    }

    private fun queryHostNightMode(): Boolean =
        HostInfo.adapter?.hostNightMode() ?: systemNightMode()

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