package dev.ujhhgtg.wekit.features.core

import android.content.Context
import androidx.core.content.edit
import cn.hxy.kiora.host.HostEnv
import dev.ujhhgtg.wekit.data.KvStore
import dev.ujhhgtg.wekit.utils.WeLogger

/**
 * 带开关的功能基类。开关状态的权威存储是 [HostEnv.accountPreference]（键 = [technicalId]），
 * 与设置页的 WeKit 适配项、[loadPersistedState] 共用同一份；[KvStore] 仅作为旧数据的
 * 一次性迁移来源。
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
        // 读取同样要容错：`getBoolean` 在键被历史版本写成别的类型时抛 ClassCastException，
        // 会让这一项（乃至整批 loadPersistedState）中断。取不到就按默认值处理并回写规范化。
        val stored = if (accountPrefs.contains(technicalId)) {
            runCatching { accountPrefs.getBoolean(technicalId, defaultEnabled) }.getOrNull()
        } else {
            null
        }
        // 去重时删掉了一批与 WeKit 功能重复的原生 `Wx*` hook。原生 hook 的开关键是
        // 类名（如 WxLocation），与这里的 technicalId（如「虚拟定位」）不同，直接删除
        // 会让老用户**已经开启**的功能静默回到关闭 —— 表现就是「升级后一堆功能失效」。
        // 这里在一次读取时把旧键迁移过来：仅在 WeKit 键缺席、旧键存在时才读旧值。
        val migrated = if (stored == null) {
            LEGACY_NATIVE_KEYS[technicalId]?.let { legacyKey ->
                if (accountPrefs.contains(legacyKey)) {
                    runCatching { accountPrefs.getBoolean(legacyKey, defaultEnabled) }.getOrNull()
                } else {
                    null
                }
            }
        } else {
            null
        }
        _isEnabled = when {
            stored != null -> stored
            migrated != null -> {
                accountPrefs.edit { putBoolean(technicalId, migrated) }
                migrated
            }
            else -> {
                accountPrefs.edit { putBoolean(technicalId, legacy) }
                legacy
            }
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
        // 上游写 KvStore；切片里 loadPersistedState 以 HostEnv.accountPreference 为准
        // （KvStore 只在键缺席时作为一次性迁移源），设置页开关走的是同一个键，
        // 所以这里必须写账号偏好，否则本方法切换的状态重启即丢。
        HostEnv.accountPreference.edit { putBoolean(technicalId, newState) }
        isEnabled = newState
        toggleCompletionCallback?.run()
    }

    companion object {
        /**
         * 已删除的原生（Kiora `Wx*`）重复 hook：键 = WeKit 功能的 [BaseFeature.technicalId]，
         * 值 = 被删除的原生 hook 类名（即它当年的开关键）。仅用于把老用户的开关状态迁移到
         * 去重后保留的 WeKit 功能上，避免功能静默失效。
         */
        private val LEGACY_NATIVE_KEYS = mapOf(
            "防撤回" to "WxAntiRevoke",
            "自动启用发送原图" to "WxAutoSelectOriginal",
            "自动查看原图" to "WxAutoViewOriginal",
            "屏蔽铃声" to "WxDisableRingtone",
            "禁止上传正在输入状态" to "WxDisableSendStatus",
            "表情游戏控制" to "WxEmojiGame",
            "虚拟定位" to "WxLocation",
            "允许公众号网页多开" to "WxMultiWebView",
            "移除分享签名校验" to "WxShareCheck",
            "修改运动步数" to "WxSportStep",
            "伪装语音时长" to "WxVoiceLength",
        )
    }
}
