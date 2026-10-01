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

    /** 开发者条目：头像 + 点击跳转的 QQ 链接。 */
    data class Developer(
        val avatarUrl: String,
        val profileUrl: String,
    )

    /**
     * 开发者列表（在线头像，不打包进 APK）。
     *
     * QQ 头像 CDN 直链，`s=640` 取 640×640 原图，显示端缩到 60dp；
     * `profileUrl` 是点击头像时跳转的 QQ 主页链接。
     */
    val developers = listOf(
        Developer(
            avatarUrl = "https://q1.qlogo.cn/g?b=qq&nk=551234445&s=640",
            profileUrl = "tencent://ntqq-open?subCmd=profile&action=openMiniBuddyProfile&actionParams={\"uin\":\"551234445\",\"sourceType\":\"QrCodeShareBuddyLink\"}"
        ),
        Developer(
            avatarUrl = "https://q1.qlogo.cn/g?b=qq&nk=2962772241&s=640",
            profileUrl = "tencent://ntqq-open?subCmd=profile&action=openMiniBuddyProfile&actionParams={\"uin\":\"2962772241\",\"sourceType\":\"QrCodeShareBuddyLink\"}"
        ),
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
