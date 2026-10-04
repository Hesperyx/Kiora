package cn.hxy.kiora.ui.viewmodel

import android.content.Context
import androidx.activity.ComponentActivity
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.ui.pages.settings.FunctionData
import dev.ujhhgtg.wekit.features.WxFeatureRegistry
import dev.ujhhgtg.wekit.features.core.BaseFeature
import dev.ujhhgtg.wekit.features.core.ClickableFeature
import dev.ujhhgtg.wekit.features.core.SwitchFeature
import dev.ujhhgtg.wekit.utils.WeLogger

/**
 * WeKit 血统功能 → Kiora 设置页 [FunctionData] 的桥接器。
 *
 * WeKit 功能的开关存 `Kiora_Config_global`（KvStore），生命周期（enable/disable）由
 * [SwitchFeature.applyToggle] 内部触发，与 Kiora 原生 hook 项（`Kiora_Config_<账号>` +
 * `isEnable` setter）不兼容。因此不手写 30 个 [cn.hxy.kiora.hook.base.BaseSwitchHookItem]
 * 子类，而是在这里把 [WxFeatureRegistry.all] 直接映射成 [FunctionData]，
 * 由 [SettingViewModel] 按 id 前缀 `wekit:` 分流到本桥接器的 toggle/click。
 */
object WxFeatureAdapter {

    /** WeKit 项在 [FunctionData.id] 里的统一前缀，与原生类名 id 隔离。 */
    const val ID_PREFIX = "wekit:"

    private const val TAG = "WxFeatureAdapter"

    /** WeKit 英文 categoryId → Kiora 中文分类。未映射的兜底到 [HookCategory.OTHER]。 */
    private val CATEGORY_MAP = mapOf(
        "chat" to HookCategory.CHAT,
        "contacts_groups" to HookCategory.GROUP,
        "moments" to HookCategory.SOCIAL,
        "system_privacy" to HookCategory.PURIFY,
        "voip" to HookCategory.MSG,
        "beautify" to HookCategory.APPEARANCE,
        "official_accounts" to HookCategory.MISC,
        "miniapps" to HookCategory.MISC,
        "debug" to HookCategory.DEBUG,
    )

    /** 把 WeKit 功能映射成 (分类名, FunctionData)。多分类功能取第一个 categoryId。 */
    fun toFunctionData(context: Context): List<Pair<String, FunctionData>> =
        WxFeatureRegistry.all.mapNotNull { feature ->
            val category = CATEGORY_MAP[feature.categoryIds.firstOrNull()] ?: HookCategory.OTHER
            // 兜底补读持久化状态：微信下 WxFeatureLoader.load() 已 loadPersistedState，
            // 但防御非微信进程 / 未加载场景，避免开关显示成默认值全关。
            (feature as? SwitchFeature)?.let { runCatching { it.loadPersistedState() } }
            category to feature.toFunctionData(context)
        }

    private fun BaseFeature.toFunctionData(context: Context): FunctionData {
        val isClickable = this is ClickableFeature
        val noSwitch = isClickable && this.noSwitchWidget
        return FunctionData(
            id = ID_PREFIX + technicalId,
            name = localizedName(context),
            description = localizedDescription(context),
            isEnabled = (this as? SwitchFeature)?.isEnabled ?: false,
            // WeKit 无 isAvailable 概念；noSwitchWidget 的功能用置灰开关表达「不可切，仅点击」
            isAvailable = !noSwitch,
            isClickable = isClickable,
            configKey = if (isClickable) ID_PREFIX + technicalId else null,
            lockedBy = null,
        )
    }

    /** 切换 WeKit 功能开关，立即写回 KvStore 并触发 enable/disable。 */
    fun toggle(technicalId: String, enabled: Boolean) {
        val feature = WxFeatureRegistry.all.find { it.technicalId == technicalId }
        if (feature == null) {
            WeLogger.w(TAG, "toggle 找不到功能：$technicalId")
            return
        }
        (feature as? SwitchFeature)?.applyToggle(enabled)
    }

    /** 触发 WeKit ClickableFeature 的 onClick。 */
    fun click(technicalId: String, activity: ComponentActivity) {
        val feature = WxFeatureRegistry.all.find { it.technicalId == technicalId }
        if (feature == null) {
            WeLogger.w(TAG, "click 找不到功能：$technicalId")
            return
        }
        runCatching { (feature as ClickableFeature).onClick(activity) }
            .onFailure { WeLogger.e(TAG, "click 执行失败：$technicalId", it) }
    }

    /** 判断一个 FunctionData.id 是否 WeKit 项。 */
    fun isWeKitId(id: String): Boolean = id.startsWith(ID_PREFIX)

    /** 从 WeKit 项 id 反解 technicalId。 */
    fun technicalIdOf(id: String): String = id.removePrefix(ID_PREFIX)

    /** 供 SettingViewModel 注入 context 用（解析 nameRes 需要宿主 context）。 */
    val hostContext: Context get() = HostInfo.hostContext
}
