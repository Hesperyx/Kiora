package cn.hxy.kiora.hook.ui.liquidglass

import android.os.Build
import cn.hxy.kiora.conf.FloatingBarConfig

/** 悬浮底栏的两种实现。 */
internal enum class BottomBarImplementation {
    CLASSIC,
    NEW_VIEW,
}

internal enum class FloatingBottomBarMode {
    NORMAL,
    LIQUID_GLASS,
}

/** 悬浮底栏距屏幕底部的垂直位置。 */
internal enum class FloatingBottomBarPosition {

    MODERATE,
    BOTTOM;

    /** 距底部锚点的间隙（dp），按有无导航栏区分。 */
    fun offsetDp(hasNavBar: Boolean): Float = when (this) {
        BOTTOM -> if (hasNavBar) 8f else 12f
        MODERATE -> if (hasNavBar) 12f else 28f
    }
}

internal data class FloatingBottomBarConfig(
    val implementation: BottomBarImplementation = BottomBarImplementation.CLASSIC,
    val mode: FloatingBottomBarMode = FloatingBottomBarMode.NORMAL,
    val scale: Float = 1f,
    val blurPercent: Int = 100,
    val position: FloatingBottomBarPosition = FloatingBottomBarPosition.MODERATE,
)

/**
 * 悬浮底栏的运行时配置快照。
 *
 * 配置本体由 [cn.hxy.kiora.hook.ui.FloatingBottomBar] 通过 Kiora 的 Config 体系持有，
 * 这里只保存只读快照并由其上推，避免本包反向引用 HookItem。
 *
 * 读出的实现方式与渲染模式都带系统版本门槛：经典实现的折射管线依赖
 * `RuntimeShader`（API 33），低版本若照配置直走会在玻璃视图的类加载阶段失败，
 * 因此统一在此降级到「新视图 + 普通」，宿主底栏仍被接管，不会留下半成品外观。
 */
internal object FloatingBottomBarConfigStore {

    private const val DEFAULT_SCALE_PERCENT = 100
    private const val DEFAULT_BLUR_PERCENT = 100

    @Volatile
    private var current: FloatingBarConfig = FloatingBarConfig()

    /** 由 HookItem 层在配置加载或保存后推送。 */
    fun update(config: FloatingBarConfig) {
        current = config
    }

    /**
     * 平滑切页开关。
     *
     * 单独出一个访问器：切页与 pager 绑定都是高频路径，不应为此反复构造配置对象。
     */
    fun isSmoothPageSwitchEnabled(): Boolean = current.smoothPageSwitch

    fun read(): FloatingBottomBarConfig {
        val cfg = current
        val supportsLiquidGlass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

        val implementation = when {
            !supportsLiquidGlass -> BottomBarImplementation.NEW_VIEW
            cfg.implementation == FloatingBarConfig.IMPL_NEW_VIEW -> BottomBarImplementation.NEW_VIEW
            else -> BottomBarImplementation.CLASSIC
        }

        val mode = when {
            supportsLiquidGlass && cfg.mode == FloatingBarConfig.MODE_LIQUID_GLASS ->
                FloatingBottomBarMode.LIQUID_GLASS
            else -> FloatingBottomBarMode.NORMAL
        }

        val percent = cfg.scalePercent.takeIf { it in 80..120 } ?: DEFAULT_SCALE_PERCENT
        val blurPercent = cfg.blurPercent.takeIf { it in 0..100 } ?: DEFAULT_BLUR_PERCENT
        val position = when (cfg.position) {
            FloatingBarConfig.POSITION_BOTTOM -> FloatingBottomBarPosition.BOTTOM
            else -> FloatingBottomBarPosition.MODERATE
        }

        return FloatingBottomBarConfig(
            implementation = implementation,
            mode = mode,
            scale = percent / 100f,
            blurPercent = blurPercent,
            position = position,
        )
    }
}
