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

    /** 开发基线版本：新增功能主要基于该版本开发。 */
    const val DEV_BASELINE_VERSION = "9.3.70"

    /** 模块用到的开源项目（顺序同 README 致谢表）。 */
    val ossCredits = listOf(
        "LSPosed",
        "LibXposed",
        "DexKit",
        "Jetpack Compose",
        "AndroidLiquidGlass",
        "BeanShell",
        "QAuxiliary",
        "TCQT"
    )
}
