package cn.hxy.kiora.hook.base

/**
 * [BaseHookItem.exclusiveGroup] 用到的分组标识。
 *
 * 集中定义，避免各处写字面量拼错 —— 拼错不会报错，只会静默失去互斥效果。
 */
object ExclusiveGroup {

    /**
     * 接管宿主底部导航栏的实现。
     *
     * 「悬浮底栏」走布局手术把原生底栏整体搬进玻璃容器，「液态玻璃导航栏」
     * 直接替换原生底栏。两者都认领同一个宿主控件，同时生效会互相拆台。
     */
    const val HOST_BOTTOM_BAR = "HostBottomBar"
}
