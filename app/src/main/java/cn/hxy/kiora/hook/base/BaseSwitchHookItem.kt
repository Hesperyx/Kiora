package cn.hxy.kiora.hook.base

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.utils.qq.QQCurrentEnv
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Suppress("DEPRECATION")
abstract class BaseSwitchHookItem : BaseHookItem() {

    val tag: String get() = annotation?.tag ?: "Unknown"
    val desc: String get() {
        val originalDesc = annotation?.desc ?: ""
        return if (isNeedRestart) "$originalDesc，重启生效" else originalDesc
    }
    val category: String get() = annotation?.category ?: HookCategory.OTHER

    override var isEnable: Boolean by BooleanPreference(name, false)
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

    class BooleanPreference(private val key: String, private val default: Boolean) :
        ReadWriteProperty<Any, Boolean> {


        override fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
            return prefs.getBoolean(key, default)
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
            prefs.edit { putBoolean(key, value) }
        }
    }

    companion object {
        val prefs: SharedPreferences
            get() = HostInfo.hostContext.getSharedPreferences(
                "Kiora_Config_${HostInfo.adapter?.currentAccount ?: QQCurrentEnv.currentUin}",
                Context.MODE_MULTI_PROCESS
            )

    }

}