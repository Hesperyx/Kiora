package cn.hxy.kiora.qq.conf

import kotlinx.serialization.Serializable

@Serializable
data class FloatingBarConfig(
    /** 1=经典（布局手术 + 液态玻璃折射）  2=新视图（View 绘制） */
    val implementation: Int = 1,
    /** 1=普通  2=液态玻璃 */
    val mode: Int = 1,
    /** 80 ~ 120 */
    val scalePercent: Int = 100,
    /** 0 ~ 100 */
    val blurPercent: Int = 100,
    /** 1=适中  2=靠底 */
    val position: Int = 1,
    /** 切页时走 ViewPager2 平滑路径 */
    val smoothPageSwitch: Boolean = false,
) {
    companion object {
        const val IMPL_CLASSIC = 1
        const val IMPL_NEW_VIEW = 2

        const val MODE_NORMAL = 1
        const val MODE_LIQUID_GLASS = 2

        const val POSITION_MODERATE = 1
        const val POSITION_BOTTOM = 2
    }
}
