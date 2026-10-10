package cn.hxy.kiora.hook.base

import android.content.SharedPreferences
import androidx.core.content.edit
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.host.HostEnv
import cn.hxy.kiora.utils.log.LogUtils

@Suppress("DEPRECATION")
abstract class BaseSwitchHookItem(
    private val switchKey: String? = null
) : BaseHookItem() {

    open val tag: String get() = annotation?.tag ?: "Unknown"

    open val desc: String get() {
        val originalDesc = annotation?.desc ?: ""
        return if (isNeedRestart) "$originalDesc，重启生效" else originalDesc
    }

    open val category: String get() = annotation?.category ?: HookCategory.OTHER

    /**
     * 开关实际落盘的键：默认用 [name]，WeKit 适配器可传 [switchKey] 改用 technicalId，
     * 避免不同宿主同名类（`javaClass.simpleName`）互相覆盖。
     */
    private val enableKey: String get() = switchKey ?: name

    /**
     * 开关状态的容错读取。
     *
     * `SharedPreferences.getBoolean` 在键存在但类型不符时抛 [ClassCastException]；设置页会在
     * 主线程上把每个功能项的 `isEnable` 都读一遍，只要有一个键被历史版本用别的类型写过，
     * 异常就会从 Compose 组合期冒出来，整片功能列表都渲染不出来。这里统一退化为默认值。
     */
    private fun readEnable(): Boolean =
        runCatching { prefs.getBoolean(enableKey, false) }.getOrDefault(false)

    override var isEnable: Boolean
        get() = readEnable()
        set(value) {
            // 幂等：值没变就不写盘、不回调，避免互斥组收敛等场景重复触发 onEnabledChange。
            if (readEnable() == value) return
            prefs.edit { putBoolean(enableKey, value) }
            onEnabledChange(value)
        }

    /**
     * 开关状态变化回调。原生 hook 只需要持久化，默认空实现；
     * WeKit 适配器在此调用 `SwitchFeature.enable/disable`，让运行中切换立即生效。
     */
    protected open fun onEnabledChange(enabled: Boolean) {}

    var isAvailable: Boolean = false

    open val isNeedRestart: Boolean = false

    fun init() {
        try {
            // 先过宿主闸门再跑 onInit()：onInit() 通常要摸宿主特有的类，
            // 在别的宿主里跑纯属白费力，还会刷一堆"类不存在"。
            if (!isInTargetHost()) {
                isAvailable = false
                return
            }

            isAvailable = onInit()
            if (isAvailable && shouldLoad()) {
                if (this is BaseClickableHookItem<*>) initData()
                onHook()
            }
        } catch (t: Throwable) {
            LogUtils.e(this, t)
            isAvailable = false
        }
    }

    protected open fun onInit(): Boolean = true

    protected open fun onHook() {}

    companion object {
        val prefs: SharedPreferences
            get() = HostEnv.accountPreference
    }
}
