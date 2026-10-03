package dev.ujhhgtg.wekit

/**
 * WeKit 血统代码的 BuildConfig 入口桥。
 *
 * WeKit 的 `constants/PackageNames.kt` 等文件引用 `dev.ujhhgtg.wekit.BuildConfig.APPLICATION_ID`
 * 之类的字段。与 `R` 一样，这里转发到 Kiora 的 `cn.hxy.kiora.BuildConfig`。
 */
object BuildConfig {
    val APPLICATION_ID: String = cn.hxy.kiora.BuildConfig.APPLICATION_ID

    val VERSION_CODE: Int = cn.hxy.kiora.BuildConfig.VERSION_CODE

    val VERSION_NAME: String = cn.hxy.kiora.BuildConfig.VERSION_NAME

    val DEBUG: Boolean = cn.hxy.kiora.BuildConfig.DEBUG

    /** WeKit 原版的日志 TAG 常量（buildConfigField "WeKit"），这里用 Kiora 的。 */
    const val TAG: String = "Kiora"
}
