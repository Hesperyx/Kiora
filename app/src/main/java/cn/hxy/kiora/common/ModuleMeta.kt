package cn.hxy.kiora.common

/**
 * 模块对外声明的静态元数据：适配版本与开源致谢。
 *
 * 内容与 README 的「适配与运行环境 / 致谢」两节保持一致，改这里时记得同步 README。
 */
object ModuleMeta {

    /** 适配的 QQ 最低版本。 */
    const val ADAPTED_QQ_VERSION = "9.1.25"

    /** 适配的 TIM 最低版本（旧版架构，仅部分兼容）。 */
    const val ADAPTED_TIM_VERSION = "4.0.95"

    /** 适配的微信版本（8.0.78 arm64 已真机验证）。 */
    const val ADAPTED_WECHAT_VERSION = "8.0.78"

    /**
     * 开发者头像（在线加载，不打包进 APK）。
     *
     * QQ 头像 CDN 直链，`s=640` 取 640×640 原图，显示端缩到 60dp。
     */
    val developerAvatars = listOf(
        "https://q1.qlogo.cn/g?b=qq&nk=551234445&s=640",
        "https://q1.qlogo.cn/g?b=qq&nk=2962772241&s=640"
    )

    /** 模块用到的开源项目（顺序同 README 致谢表）。 */
    val ossCredits = listOf(
        "LSPosed",
        "LibXposed",
        "DexKit",
        "Jetpack Compose",
        "AndroidLiquidGlass",
        "BeanShell",
        "QAuxiliary",
        "TCQT",
        "WAuxiliary"
    )
}
