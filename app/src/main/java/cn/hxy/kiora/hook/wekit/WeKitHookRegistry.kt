package cn.hxy.kiora.hook.wekit

import cn.hxy.kiora.annotation.HookCategory
import dev.ujhhgtg.wekit.features.WxFeatureRegistry
import dev.ujhhgtg.wekit.features.core.BaseFeature
import dev.ujhhgtg.wekit.features.core.FeatureCategoryIds

object WeKitHookRegistry {

    private val CATEGORY_MAP = mapOf(
        FeatureCategoryIds.CHAT to HookCategory.CHAT,
        FeatureCategoryIds.CONTACTS_GROUPS to HookCategory.GROUP,
        FeatureCategoryIds.PAYMENT to HookCategory.RED_PACKET,
        FeatureCategoryIds.MOMENTS to HookCategory.SOCIAL,
        FeatureCategoryIds.SYSTEM_PRIVACY to HookCategory.PURIFY,
        FeatureCategoryIds.VOIP to HookCategory.MSG,
        FeatureCategoryIds.NOTIFICATIONS to HookCategory.NOTIFICATION,
        FeatureCategoryIds.BEAUTIFY to HookCategory.APPEARANCE,
        FeatureCategoryIds.OFFICIAL_ACCOUNTS to HookCategory.MISC,
        FeatureCategoryIds.MINIAPPS to HookCategory.MISC,
        FeatureCategoryIds.CHANNELS to HookCategory.SOCIAL,
        FeatureCategoryIds.PROFILE to HookCategory.MISC,
        FeatureCategoryIds.DEBUG to HookCategory.DEBUG,
        FeatureCategoryIds.SCRIPTING_JAVA to HookCategory.MISC,
        FeatureCategoryIds.SCRIPTING_PYTHON to HookCategory.MISC,
        FeatureCategoryIds.ENTERTAIN to HookCategory.APPEARANCE,
    )

    val hookItems: List<WeKitFeatureHookItem> by lazy {
        WxFeatureRegistry.all.map(::WeKitFeatureHookItem)
    }

    fun categoryOf(feature: BaseFeature): String =
        CATEGORY_MAP[feature.categoryIds.firstOrNull()] ?: HookCategory.OTHER
}
