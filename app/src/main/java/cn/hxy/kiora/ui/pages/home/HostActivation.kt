package cn.hxy.kiora.ui.pages.home

/** 单个宿主的激活状态。 */
enum class ActivationState {
    /** LSPosed 作用域里勾了该宿主。 */
    ACTIVE,

    /** 框架可读作用域，但没勾该宿主。 */
    INACTIVE,

    /** 框架不提供作用域信息（Taichi / 老 EdXposed / 部分 LSPatch），无法判断。 */
    UNKNOWN
}

/**
 * 主页「激活状态」卡的一行。
 *
 * 判定依据是 LSPosed 作用域（`XposedService.scope`）里有没有该宿主的包名 ——
 * 全项目唯一能按宿主区分的信号；`HookStatus.isModuleEnabled()` 只看框架是否
 * 加载了模块，是框架级的，分不出 QQ 还是微信。
 */
data class HostActivation(val name: String, val state: ActivationState)
