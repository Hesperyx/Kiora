package dev.ujhhgtg.wekit.features.core

import android.content.Context
import androidx.core.content.edit
import cn.hxy.kiora.host.HostEnv
import dev.ujhhgtg.wekit.data.KvStore
import dev.ujhhgtg.wekit.utils.WeLogger

/**
 * 带开关的功能基类。与 WeKit 原版逐字一致（存储换成了 [KvStore] 的切片实现）。
 */
abstract class SwitchFeature : BaseFeature() {

    /** Optional order override within each concrete settings category. Lower values appear first. */
    open val displayOrder: Int? = null

    /**
     * Default state when the user has never toggled this feature.
     */
    open val defaultEnabled: Boolean = false

    /** Whether the feature should be active at startup, given the cached preference. */
    protected open val shouldEnableOnStartup: Boolean
        get() = _isEnabled

    fun loadPersistedState() {
        val accountPrefs = HostEnv.accountPreference
        val legacy = KvStore.getBoolOrDef(technicalId, defaultEnabled)
        _isEnabled = if (accountPrefs.contains(technicalId)) {
            accountPrefs.getBoolean(technicalId, defaultEnabled)
        } else {
            accountPrefs.edit { putBoolean(technicalId, legacy) }
            legacy
        }
    }

    final override fun startup() {
        if (shouldEnableOnStartup) enable()
    }

    /** Cached user preference (desired state). Distinct from [isActive], the runtime truth. */
    @Suppress("PropertyName")
    protected var _isEnabled = false

    var isEnabled
        get() = _isEnabled
        set(value) {
            if (_isEnabled == value) return
            _isEnabled = value
            if (value) {
                WeLogger.i("SwitchFeature", "enabling $technicalPath...")
                enable()
            } else {
                WeLogger.i("SwitchFeature", "disabling $technicalPath...")
                disable()
            }
        }

    private var toggleCompletionCallback: Runnable? = null

    open fun onBeforeToggle(newState: Boolean, context: Context): Boolean = true

    fun setToggleCompletionCallback(callback: Runnable) {
        toggleCompletionCallback = callback
    }

    fun applyToggle(newState: Boolean) {
        isEnabled = newState
        toggleCompletionCallback?.run()
    }
}
