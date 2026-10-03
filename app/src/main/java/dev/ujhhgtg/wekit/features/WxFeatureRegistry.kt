package dev.ujhhgtg.wekit.features

import dev.ujhhgtg.wekit.features.core.BaseFeature
import dev.ujhhgtg.wekit.features.core.validateFeatures
import dev.ujhhgtg.wekit.features.items.beautify.HideOtherDevicesBanner
import dev.ujhhgtg.wekit.features.items.chat.AntiReadReceipts
import dev.ujhhgtg.wekit.features.items.chat.AntiSecMsg
import dev.ujhhgtg.wekit.features.items.chat.BlockAbnormalSizeStickers
import dev.ujhhgtg.wekit.features.items.chat.BypassRiskFileBlocking
import dev.ujhhgtg.wekit.features.items.chat.DisableMessageCollapsing
import dev.ujhhgtg.wekit.features.items.chat.DisablePat
import dev.ujhhgtg.wekit.features.items.chat.DisablePinnedChatsCollapsing
import dev.ujhhgtg.wekit.features.items.chat.DisableTypingStatusUploading
import dev.ujhhgtg.wekit.features.items.chat.ExternalSharingEvolved
import dev.ujhhgtg.wekit.features.items.chat.MergeChatMessageContextMenuItems
import dev.ujhhgtg.wekit.features.items.contacts.AutoDndAfterJoinGroup
import dev.ujhhgtg.wekit.features.items.debug.ProfileMemory
import dev.ujhhgtg.wekit.features.items.miniapps.RemoveMenuLimits
import dev.ujhhgtg.wekit.features.items.miniapps.RemoveSplashAds
import dev.ujhhgtg.wekit.features.items.miniapps.SkipSplash
import dev.ujhhgtg.wekit.features.items.miniapps.SpoofHostVersion
import dev.ujhhgtg.wekit.features.items.moments.AntiMomentsDelete
import dev.ujhhgtg.wekit.features.items.moments.DisableVideosAutoPlay
import dev.ujhhgtg.wekit.features.items.moments.EnhanceQuery
import dev.ujhhgtg.wekit.features.items.moments.MomentsEditorBackOptimization
import dev.ujhhgtg.wekit.features.items.official_accounts.RemoveOfficialAccountAds
import dev.ujhhgtg.wekit.features.items.system.AutoCleanCache
import dev.ujhhgtg.wekit.features.items.system.DisableResumeWatchingToast
import dev.ujhhgtg.wekit.features.items.system.DisableShareScreenshotToast
import dev.ujhhgtg.wekit.features.items.system.DisableWebViewSafetyWarnings
import dev.ujhhgtg.wekit.features.items.system.NerfBackgroundProcessChecker
import dev.ujhhgtg.wekit.features.items.system.RemoveArticleAds
import dev.ujhhgtg.wekit.features.items.system.RemoveExternalAppSharingSignatureVerify
import dev.ujhhgtg.wekit.features.items.voip.RemoveLimitsDuringCalls

/**
 * WeKit 血统功能的注册表。
 *
 * WeKit 原版靠 KSP 扫描 `BaseFeature` 子类型，编译期生成 `FeaturesProvider`
 * （`libs/common:annotation-scanner`）。**切片改手写注册表** —— 功能数量有限时，
 * 引入一套 KSP 代码生成器不划算。
 *
 * 功能数量继续上来之后应该补回扫描器，否则每加一个功能都要改这里，而且容易漏。
 */
object WxFeatureRegistry {

    val all: List<BaseFeature> by lazy {
        validateFeatures(
            listOf(
                HideOtherDevicesBanner,
                AntiReadReceipts,
                AntiSecMsg,
                BlockAbnormalSizeStickers,
                BypassRiskFileBlocking,
                DisableMessageCollapsing,
                DisablePat,
                DisablePinnedChatsCollapsing,
                DisableTypingStatusUploading,
                ExternalSharingEvolved,
                MergeChatMessageContextMenuItems,
                AutoDndAfterJoinGroup,
                ProfileMemory,
                RemoveMenuLimits,
                RemoveSplashAds,
                SkipSplash,
                SpoofHostVersion,
                AntiMomentsDelete,
                DisableVideosAutoPlay,
                EnhanceQuery,
                MomentsEditorBackOptimization,
                RemoveOfficialAccountAds,
                AutoCleanCache,
                DisableResumeWatchingToast,
                DisableShareScreenshotToast,
                DisableWebViewSafetyWarnings,
                NerfBackgroundProcessChecker,
                RemoveArticleAds,
                RemoveExternalAppSharingSignatureVerify,
                RemoveLimitsDuringCalls,
            )
        )
    }
}
