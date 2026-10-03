package dev.ujhhgtg.wekit.data

import cn.hxy.kiora.host.HostEnv

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
}
