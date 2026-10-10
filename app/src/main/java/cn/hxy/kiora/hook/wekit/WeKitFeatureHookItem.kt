package cn.hxy.kiora.hook.wekit

import androidx.activity.ComponentActivity
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.host.HostInfo
import dev.ujhhgtg.wekit.features.core.BaseFeature
import dev.ujhhgtg.wekit.features.core.ClickableFeature
import dev.ujhhgtg.wekit.features.core.SwitchFeature
import dev.ujhhgtg.wekit.loader.utils.ResourcesInjector
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

    /**
     * 功能名。
     *
     * 取本地化字符串前先把模块自己的 `strings.xml` 注册进宿主资源表：未注册时
     * `getString(模块资源 id)` 不一定抛异常，而是返回空串，设置页整片标题就会空白。
     * 取不到或取到空白时回退到 [BaseFeature.technicalId]（上游就是中文名），保证有内容。
     */
    override val tag: String
        get() = localizedFromHost { feature.localizedName(it) }
            ?.takeIf { it.isNotBlank() }
            ?: feature.technicalId

    /** 功能描述，同样按空白安全处理，取不到就留空而不显示错误内容。 */
    override val desc: String
        get() = localizedFromHost { feature.localizedDescription(it) }
            ?.takeIf { it.isNotBlank() }
            .orEmpty()

    /** 在宿主 Context 上取本地化字符串，并在取值前确保模块资源已注册。 */
    private fun localizedFromHost(block: (android.content.Context) -> String): String? =
        runCatching {
            val context = HostInfo.hostContext
            ResourcesInjector.injectModuleRes(context.resources)
            block(context)
        }.getOrNull()

    /**
     * 是否需要渲染开关控件。
     *
     * [ClickableFeature.noSwitchWidget] 为 true 的项是**纯动作项**（批量操作、检测单向好友、
     * 调试工具等），它们没有 [dev.ujhhgtg.wekit.features.core.SwitchFeature.onEnable] 钩子，
     * 只靠点击行触发 [WeKitFeatureHookItem.onClick]。这类项不该显示开关（拨了也没效果），
     * 但仍必须可点击 —— 由设置页按本标志隐藏开关、保留行点击。
     */
    val showSwitch: Boolean
        get() = !(feature is ClickableFeature && feature.noSwitchWidget)

    override val category: String
        get() = WeKitHookRegistry.categoryOf(feature)

    override fun isInTargetHost(): Boolean = HostInfo.isWeChat

    override fun isInTargetProcess(): Boolean =
        TargetProcesses.currentType in feature.targetProcesses

    override fun shouldLoad(): Boolean = isInTargetHost() && isInTargetProcess()

    /**
     * 在微信宿主里一律视为可用。
     *
     * 旧实现把「不显示开关」错当成「不可用」，让 [ClickableFeature.noSwitchWidget] 的
     * 纯动作项在设置页被置灰且 `onClick` 被拦掉，整类功能点不动。可用性与是否显示开关
     * 是两件事：前者见本方法，后者见 [showSwitch]。
     */
    override fun onInit(): Boolean = true

    override fun onEnabledChange(enabled: Boolean) {
        (feature as? SwitchFeature)?.isEnabled = enabled
    }

    fun onClick(activity: ComponentActivity) {
        val clickable = feature as? ClickableFeature ?: return
        runCatching { clickable.onClick(activity) }
    }
}
