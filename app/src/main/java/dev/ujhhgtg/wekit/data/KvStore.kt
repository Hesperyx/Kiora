package dev.ujhhgtg.wekit.data

import cn.hxy.kiora.host.HostEnv
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * WeKit 血统功能的键值存储。
 *
 * 原版基于 FastKV（mmap 写入），还带 `prefOption` 属性委托、结构化数据与历史迁移逻辑。
 * 切片改为落在 Kiora 已有的全局偏好（`Kiora_Config_global`）上，不为 4 个功能引入
 * 第二套存储引擎。
 *
 * **已知简化**：`prefOption` 委托、`KvStore` 的结构化读写、以及 `WeKitDatabase`（Room）
 * 都没有迁。后续迁到用到这些的功能时，需要补回或改写。
 */
object KvStore {

    private val prefs get() = HostEnv.globalPreference

    fun getBoolOrDef(key: String, def: Boolean): Boolean = prefs.getBoolean(key, def)

    fun getBoolOrFalse(key: String): Boolean = getBoolOrDef(key, false)

    fun putBool(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getLongOrDef(key: String, def: Long): Long = prefs.getLong(key, def)

    fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    fun getIntOrDef(key: String, def: Int): Int = prefs.getInt(key, def)

    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun getStringOrDef(key: String, def: String?): String? = prefs.getString(key, def)

    /** 无默认值版，返回 null 表示不存在。WeKit 原版 API 名。 */
    fun getString(key: String): String? = prefs.getString(key, null)

    fun putString(key: String, value: String?) {
        prefs.edit().putString(key, value).apply()
    }

    fun getFloatOrDef(key: String, def: Float): Float = prefs.getFloat(key, def)

    fun putFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun contains(key: String): Boolean = prefs.contains(key)

    // ── prefOption 属性委托（WeKit 原版是 Room 实现，这里落在 SharedPreferences 上）──

    fun prefOption(key: String, defValue: Boolean): ReadWriteProperty<Any?, Boolean> =
        object : ReadWriteProperty<Any?, Boolean> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
                getBoolOrDef(key, defValue)

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
                putBool(key, value)
            }
        }

    fun prefOption(key: String, defValue: String): ReadWriteProperty<Any?, String> =
        object : ReadWriteProperty<Any?, String> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): String =
                getStringOrDef(key, defValue) ?: defValue

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
                putString(key, value)
            }
        }

    fun prefOption(key: String, defValue: Int): ReadWriteProperty<Any?, Int> =
        object : ReadWriteProperty<Any?, Int> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): Int =
                getIntOrDef(key, defValue)

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
                putInt(key, value)
            }
        }

    fun prefOption(key: String, defValue: Long): ReadWriteProperty<Any?, Long> =
        object : ReadWriteProperty<Any?, Long> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): Long =
                getLongOrDef(key, defValue)

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
                putLong(key, value)
            }
        }
}
