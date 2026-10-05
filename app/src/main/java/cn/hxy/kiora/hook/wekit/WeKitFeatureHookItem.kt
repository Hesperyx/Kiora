package cn.hxy.kiora.hook.wekit

import androidx.activity.ComponentActivity
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.host.HostInfo
import dev.ujhhgtg.wekit.features.core.BaseFeature
import dev.ujhhgtg.wekit.features.core.ClickableFeature
import dev.ujhhgtg.wekit.features.core.SwitchFeature
import dev.ujhhgtg.wekit.utils.TargetProcesses

/**
 * 把一个 WeKit 血统功能包装成 Kiora 设置页的 [BaseSwitchHookItem]。
 *
 * - 开关键用 [BaseFeature.technicalId]（`switchKey`），与 [SwitchFeature.loadPersistedState]
 *   迁移后读写的账号偏好键一致。
 * - `isEnable` setter 由基类写账号偏好后回调 [onEnabledChange]，这里再触发
 *   [SwitchFeature.isEnabled]，让运行中切换立即 enable/disable。
 */
class WeKitFeatureHookItem(
    val feature: BaseFeature
) : BaseSwitchHookItem(feature.technicalId) {

    override val name: String = feature.technicalId

    /** 是否 ClickableFeature：设置页据此决定行卡片点击行为，而非把全部 WeKit 项都当可点击。 */
    val isClickable: Boolean get() = feature is ClickableFeature

    override val tag: String
        get() = runCatching { HostInfo.hostContext }
            .getOrNull()?.let(feature::localizedName) ?: feature.technicalId

    override val desc: String
        get() = runCatching { HostInfo.hostContext }
            .getOrNull()?.let(feature::localizedDescription).orEmpty()

    override val category: String
        get() = WeKitHookRegistry.categoryOf(feature)

    override fun isInTargetHost(): Boolean = HostInfo.isWeChat

    override fun isInTargetProcess(): Boolean =
        TargetProcesses.currentType in feature.targetProcesses

    override fun shouldLoad(): Boolean = isInTargetHost() && isInTargetProcess()

    override fun onInit(): Boolean {
        val clickable = feature as? ClickableFeature
        return clickable?.noSwitchWidget != true
    }

    override fun onEnabledChange(enabled: Boolean) {
        (feature as? SwitchFeature)?.isEnabled = enabled
    }

    fun onClick(activity: ComponentActivity) {
        val clickable = feature as? ClickableFeature ?: return
        runCatching { clickable.onClick(activity) }
    }
}
