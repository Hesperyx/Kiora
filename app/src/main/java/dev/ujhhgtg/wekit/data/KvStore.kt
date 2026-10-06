package dev.ujhhgtg.wekit.data

import cn.hxy.kiora.host.HostEnv
import kotlin.jvm.JvmName
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

    fun getStringOrDef(key: String, def: String): String = prefs.getString(key, def) ?: def

    /**
     * 可空重载（WeKit 原版 `data\KvStore.kt:83`）。两者在 JVM 上擦除后签名相同，
     * 故用 `@JvmName` 区分 —— 调用方多为 `getStringOrDef(key, "literal")` 需要非空返回。
     */
    @JvmName("getStringOrDefNullable")
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

    /** WeKit 原版别名，语义与 [contains] 相同。 */
    fun containsKey(key: String): Boolean = contains(key)

    /** SharedPreferences 交出的 Set 由框架持有，拷贝后再返回，避免调用方原地修改污染缓存。 */
    fun getStringSet(key: String, def: Set<String>?): Set<String>? =
        prefs.getStringSet(key, def)?.toSet()

    fun getStringSetOrDef(key: String, def: Set<String>): Set<String> = getStringSet(key, def)!!

    fun putStringSet(key: String, value: Set<String>) {
        prefs.edit().putStringSet(key, value.toSet()).apply()
    }

    fun getObject(key: String): Any? = if (prefs.contains(key)) prefs.all[key] else null

    /**
     * WeKit 原版用它强制「老设置已迁移到新权威域」后才允许读取。
     * Kiora 版全部键都落在同一个 `Kiora_Config_global` 上，没有独立的老存储可迁，
     * 因此退化为可读性校验：键存在时必须能读出值。
     */
    fun requireMigrationKeys(keys: Collection<String>) {
        keys.forEach { key ->
            if (prefs.contains(key)) {
                checkNotNull(getObject(key)) { "Unreadable legacy preference value for $key" }
            }
        }
    }

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
                getStringOrDef(key, defValue)

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

    fun prefOption(key: String, defValue: Float): ReadWriteProperty<Any?, Float> =
        object : ReadWriteProperty<Any?, Float> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): Float =
                getFloatOrDef(key, defValue)

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
                putFloat(key, value)
            }
        }

    fun prefOption(key: String, defValue: Set<String>): ReadWriteProperty<Any?, Set<String>> =
        object : ReadWriteProperty<Any?, Set<String>> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): Set<String> =
                getStringSetOrDef(key, defValue)

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Set<String>) {
                putStringSet(key, value)
            }
        }

    /**
     * 可空重载（WeKit 原版 `data\KvStore.kt:235`）。`null` 表示「未设置」，读时用
     * `contains` 判定 —— SharedPreferences 无法区分「键不存在」与「存了空串」，
     * 而调用方（如 FingerprintPay）要靠 `null` 判断指纹支付密码是否已配置。
     */
    @JvmName("prefOptionNullable")
    fun prefOption(key: String, defValue: String?): ReadWriteProperty<Any?, String?> =
        object : ReadWriteProperty<Any?, String?> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): String? =
                if (prefs.contains(key)) prefs.getString(key, null) else defValue

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
                putString(key, value)
            }
        }
}
