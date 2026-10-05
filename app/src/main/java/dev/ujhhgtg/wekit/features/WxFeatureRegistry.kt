package dev.ujhhgtg.wekit.features

import dev.ujhhgtg.wekit.features.core.BaseFeature
import dev.ujhhgtg.wekit.features.core.validateFeatures
import dev.ujhhgtg.wekit.features.items.beautify.HideOtherDevicesBanner
import dev.ujhhgtg.wekit.features.items.beautify.HideHomeScreenSwipeDownPage
import dev.ujhhgtg.wekit.features.items.beautify.HideMeTabPageItems
import dev.ujhhgtg.wekit.features.items.beautify.ApplyDialogBackgroundBlur
import dev.ujhhgtg.wekit.features.items.chat.AntiReadReceipts
import dev.ujhhgtg.wekit.features.items.chat.AntiSecMsg
import dev.ujhhgtg.wekit.features.items.chat.BlockAbnormalSizeStickers
import dev.ujhhgtg.wekit.features.items.chat.BypassRiskFileBlocking
import dev.ujhhgtg.wekit.features.items.chat.DisableMessageCollapsing
import dev.ujhhgtg.wekit.features.items.chat.DisablePat
import dev.ujhhgtg.wekit.features.items.chat.DisablePinnedChatsCollapsing
import dev.ujhhgtg.wekit.features.items.chat.DisableTypingStatusUploading
import dev.ujhhgtg.wekit.features.items.chat.ExternalSharingEvolved
import dev.ujhhgtg.wekit.features.items.chat.FakeVoiceDuration
import dev.ujhhgtg.wekit.features.items.chat.HideMessagesAvatars
import dev.ujhhgtg.wekit.features.items.chat.MergeChatMessageContextMenuItems
import dev.ujhhgtg.wekit.features.items.contacts.AutoDndAfterJoinGroup
import dev.ujhhgtg.wekit.features.items.home_screen_menu.KillHostProcess
import dev.ujhhgtg.wekit.features.items.home_screen_menu.MarkAllAsRead
import dev.ujhhgtg.wekit.features.items.home_screen_menu.ToggleAllConversationsVisibility
import dev.ujhhgtg.wekit.features.items.home_screen_menu.ModuleSettings
import dev.ujhhgtg.wekit.features.items.home_screen_menu.OpenConversationMenu
import dev.ujhhgtg.wekit.features.items.debug.CopyWeChatDebugInfo
import dev.ujhhgtg.wekit.features.items.debug.ProfileMemory
import dev.ujhhgtg.wekit.features.items.debug.CrashInterceptor
import dev.ujhhgtg.wekit.features.items.debug.NativeCrashInterceptor
import dev.ujhhgtg.wekit.features.items.debug.SendPacket
import dev.ujhhgtg.wekit.features.items.debug.TriggerCrash
import dev.ujhhgtg.wekit.features.items.debug.LaunchInternalUrls
import dev.ujhhgtg.wekit.features.items.miniapps.RemoveMenuLimits
import dev.ujhhgtg.wekit.features.items.miniapps.RemoveSplashAds
import dev.ujhhgtg.wekit.features.items.miniapps.SkipSplash
import dev.ujhhgtg.wekit.features.items.miniapps.SpoofHostVersion
import dev.ujhhgtg.wekit.features.items.moments.AntiMomentsDelete
import dev.ujhhgtg.wekit.features.items.moments.DisableVideosAutoPlay
import dev.ujhhgtg.wekit.features.items.moments.EnhanceQuery
import dev.ujhhgtg.wekit.features.items.moments.MomentsEditorBackOptimization
import dev.ujhhgtg.wekit.features.items.moments.RepostMoments
import dev.ujhhgtg.wekit.features.items.moments.CustomSourceApp
import dev.ujhhgtg.wekit.features.items.profile.SetProfileNickname
import dev.ujhhgtg.wekit.features.items.entertain.ClearProfileDetails
import dev.ujhhgtg.wekit.features.items.entertain.ImageRotation
import dev.ujhhgtg.wekit.features.items.official_accounts.RemoveOfficialAccountAds
import dev.ujhhgtg.wekit.features.items.official_accounts.UseLegacyOfficialAccountsView
import dev.ujhhgtg.wekit.features.items.official_accounts.UseMultiWebViewForOfficialAccounts
import dev.ujhhgtg.wekit.features.items.system.AutoCleanCache
import dev.ujhhgtg.wekit.features.items.system.CustomDpi
import dev.ujhhgtg.wekit.features.items.system.DisableResumeWatchingToast
import dev.ujhhgtg.wekit.features.items.system.DisableShareScreenshotToast
import dev.ujhhgtg.wekit.features.items.system.DisableWebViewSafetyWarnings
import dev.ujhhgtg.wekit.features.items.system.ForceTabletMode
import dev.ujhhgtg.wekit.features.items.system.NerfBackgroundProcessChecker
import dev.ujhhgtg.wekit.features.items.system.RemoveArticleAds
import dev.ujhhgtg.wekit.features.items.system.PredictiveBackGestures
import dev.ujhhgtg.wekit.features.items.system.QrCodeRecord
import dev.ujhhgtg.wekit.features.items.system.RemoveQrCodeScanLimit
import dev.ujhhgtg.wekit.features.items.system.RemoveExternalAppSharingSignatureVerify
import dev.ujhhgtg.wekit.features.items.system.AutoApproveDeviceLogin
import dev.ujhhgtg.wekit.features.items.system.ModifySportsStepCount
import dev.ujhhgtg.wekit.features.items.voip.RemoveLimitsDuringCalls
import dev.ujhhgtg.wekit.features.items.voip.BlockVoipRingtone
import dev.ujhhgtg.wekit.features.items.beautify.BeautifyViewPressEffect
import dev.ujhhgtg.wekit.features.items.beautify.DisableChatBackgroundDimming
import dev.ujhhgtg.wekit.features.items.chat.AutoEnableSendAsMediaGroup
import dev.ujhhgtg.wekit.features.items.chat.AutoEnableSendOriginalMedia
import dev.ujhhgtg.wekit.features.items.chat.AutoSpeechToText
import dev.ujhhgtg.wekit.features.items.chat.AutoViewOriginalMedia
import dev.ujhhgtg.wekit.features.items.chat.DisableSpeechToTextButton
import dev.ujhhgtg.wekit.features.items.chat.ForceEnableAllTools
import dev.ujhhgtg.wekit.features.items.chat.MergeMessagesIntoGroups
import dev.ujhhgtg.wekit.features.items.chat.MonitorGroupMemberOperations
import dev.ujhhgtg.wekit.features.items.chat.QuickRemoveQuote
import dev.ujhhgtg.wekit.features.items.chat.RemoveCustomStickersLimit
import dev.ujhhgtg.wekit.features.items.chat.RemoveMessageSelectionLimit
import dev.ujhhgtg.wekit.features.items.chat.RemoveSendMediaCountLimit
import dev.ujhhgtg.wekit.features.items.contacts.AutoAddNearbyFriends
import dev.ujhhgtg.wekit.features.items.contacts.DisplayHiddenContactSettings
import dev.ujhhgtg.wekit.features.items.contacts.LimitGroupMemberNicknameLength
import dev.ujhhgtg.wekit.features.items.contacts.ModifyFriendsCount
import dev.ujhhgtg.wekit.features.items.contacts.RemoveGroupMemberNicknameControlCharacters
import dev.ujhhgtg.wekit.features.items.contacts.RemoveGroupMemberNicknameLengthLimit
import dev.ujhhgtg.wekit.features.items.contacts.RemoveMessageBatchForwardLimit
import dev.ujhhgtg.wekit.features.items.contacts.QuickOpenMoments
import dev.ujhhgtg.wekit.features.items.contacts.OpenConversation
import dev.ujhhgtg.wekit.features.items.chat.DisplayGroupMemberInviter
import dev.ujhhgtg.wekit.features.items.chat.QuickBackToBottom
import dev.ujhhgtg.wekit.features.items.chat.StickersManagerEnhancements
import dev.ujhhgtg.wekit.features.items.chat.ViewStickerAsImage
import dev.ujhhgtg.wekit.features.items.contacts.DisplayGroupMemberMessages
import dev.ujhhgtg.wekit.features.items.contacts.ShowFriendAddTime
import dev.ujhhgtg.wekit.features.items.contacts.ShowWxIdInContactDetails
import dev.ujhhgtg.wekit.features.items.entertain.TrollBan
import dev.ujhhgtg.wekit.features.items.entertain.RainbowText
import dev.ujhhgtg.wekit.features.items.miniapps.BypassUnderageGamingLimit
import dev.ujhhgtg.wekit.features.items.miniapps.RemoveEmbeddedAds
import dev.ujhhgtg.wekit.features.items.miniapps.RemoveVideoAds
import dev.ujhhgtg.wekit.features.items.miniapps.SkipRewardedAds
import dev.ujhhgtg.wekit.features.items.scripting_java.DecompileBeanShellSnapshot
import dev.ujhhgtg.wekit.features.items.miniapps.ErudaConsole
import dev.ujhhgtg.wekit.features.items.moments.AlwaysShowInteractionEntry
import dev.ujhhgtg.wekit.features.items.moments.AntiMomentCommentsDelete
import dev.ujhhgtg.wekit.features.items.moments.NoCloseVideoPlayerOnClick
import dev.ujhhgtg.wekit.features.items.moments.OpenDetailsOnItemClick
import dev.ujhhgtg.wekit.features.items.moments.RemoveMomentsAds
import dev.ujhhgtg.wekit.features.items.payment.AllowPrivateChatReceiveOutgoingRedPackets
import dev.ujhhgtg.wekit.features.items.payment.ModifyTransferWalletBalanceDisplay
import dev.ujhhgtg.wekit.features.items.payment.ModifyWalletBalanceDisplay
import dev.ujhhgtg.wekit.features.items.profile.RemoveSignatureLimits
import dev.ujhhgtg.wekit.features.items.system.DisableLowAvailableStorageDetection
import dev.ujhhgtg.wekit.features.items.profile.RemoveTextStatusLengthLimit
import dev.ujhhgtg.wekit.features.items.profile.UploadTransparentAvatars
import dev.ujhhgtg.wekit.features.items.shortvideos.DisableCommentSizeLimit
import dev.ujhhgtg.wekit.features.items.shortvideos.RemoveCommentAds
import dev.ujhhgtg.wekit.features.items.shortvideos.DownloadMedia
import dev.ujhhgtg.wekit.features.items.system.DisableHighBrightness
import dev.ujhhgtg.wekit.features.items.system.DisableHostHotUpdates
import dev.ujhhgtg.wekit.features.items.system.EnableWebViewFeatures
import dev.ujhhgtg.wekit.features.items.system.HideModuleFromAppList
import dev.ujhhgtg.wekit.features.items.system.PowerSaver
import dev.ujhhgtg.wekit.features.items.system.PreventModuleDataDeletion
import dev.ujhhgtg.wekit.features.items.system.PreventXposedDetection
import dev.ujhhgtg.wekit.features.items.system.SpoofEnvironment
import dev.ujhhgtg.wekit.features.items.system.UseLegacyWalletViewInMePage
import dev.ujhhgtg.wekit.features.items.chat.BatchRevoke
import dev.ujhhgtg.wekit.features.items.chat.DisplayMessageDetails
import dev.ujhhgtg.wekit.features.items.chat.DownloadFilesToLocalStorage
import dev.ujhhgtg.wekit.features.items.chat.DownloadImagesToLocalStorage
import dev.ujhhgtg.wekit.features.items.chat.ModifyTextMessageDisplay
import dev.ujhhgtg.wekit.features.items.chat.QuickRevokeAndEdit
import dev.ujhhgtg.wekit.features.items.chat.RedirectDownloadPath
import dev.ujhhgtg.wekit.features.items.chat.RemoveChatMessageContextMenuItems
import dev.ujhhgtg.wekit.features.items.chat.RepeatMessages
import dev.ujhhgtg.wekit.features.items.chat.SaveStickersToLocalStorage
import dev.ujhhgtg.wekit.features.items.chat.SaveVoicesToLocalStorage
import dev.ujhhgtg.wekit.features.items.moments.ForwardMessagesToMoments
import dev.ujhhgtg.wekit.features.items.contacts.DetectDeletedFriends


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
                HideHomeScreenSwipeDownPage,
                AntiReadReceipts,
                AntiSecMsg,
                BlockAbnormalSizeStickers,
                BypassRiskFileBlocking,
                DisableMessageCollapsing,
                DisablePat,
                DisablePinnedChatsCollapsing,
                DisableTypingStatusUploading,
                ExternalSharingEvolved,
                FakeVoiceDuration,
                MergeChatMessageContextMenuItems,
                MergeMessagesIntoGroups,
                AutoDndAfterJoinGroup,
                ProfileMemory,
                CrashInterceptor,
                NativeCrashInterceptor,
                SendPacket,
                TriggerCrash,
                RemoveMenuLimits,
                RemoveSplashAds,
                SkipSplash,
                SpoofHostVersion,
                AntiMomentsDelete,
                DisableVideosAutoPlay,
                EnhanceQuery,
                MomentsEditorBackOptimization,
                RepostMoments,
                CustomSourceApp,
                RemoveOfficialAccountAds,
                UseMultiWebViewForOfficialAccounts,
                AutoCleanCache,
                CustomDpi,
                DisableResumeWatchingToast,
                DisableShareScreenshotToast,
                DisableWebViewSafetyWarnings,
                ForceTabletMode,
                NerfBackgroundProcessChecker,
                RemoveArticleAds,
                PredictiveBackGestures,
                QrCodeRecord,
                RemoveQrCodeScanLimit,
                RemoveExternalAppSharingSignatureVerify,
                RemoveLimitsDuringCalls,
                AllowPrivateChatReceiveOutgoingRedPackets,
                ModifyTransferWalletBalanceDisplay,
                ModifyWalletBalanceDisplay,
                AlwaysShowInteractionEntry,
                AntiMomentCommentsDelete,
                AutoEnableSendAsMediaGroup,
                AutoEnableSendOriginalMedia,
                AutoSpeechToText,
                AutoViewOriginalMedia,
                BeautifyViewPressEffect,
                BypassUnderageGamingLimit,
                DisableChatBackgroundDimming,
                DisableCommentSizeLimit,
                DisableHighBrightness,
                DisableHostHotUpdates,
                DisableSpeechToTextButton,
                DisplayHiddenContactSettings,
                LimitGroupMemberNicknameLength,
                AutoAddNearbyFriends,
                CopyWeChatDebugInfo,
                EnableWebViewFeatures,
                ForceEnableAllTools,
                HideModuleFromAppList,
                MonitorGroupMemberOperations,
                NoCloseVideoPlayerOnClick,
                OpenDetailsOnItemClick,
                PowerSaver,
                PreventModuleDataDeletion,
                QuickRemoveQuote,
                RainbowText,
                TrollBan,
                QuickBackToBottom,
                StickersManagerEnhancements,
                ViewStickerAsImage,
                DisplayGroupMemberInviter,
                DisplayGroupMemberMessages,
                ShowFriendAddTime,
                ShowWxIdInContactDetails,
                RemoveCommentAds,
                DownloadMedia,
                RemoveCustomStickersLimit,
                RemoveEmbeddedAds,
                RemoveGroupMemberNicknameControlCharacters,
                RemoveGroupMemberNicknameLengthLimit,
                RemoveMessageBatchForwardLimit,
                QuickOpenMoments,
                OpenConversation,
                RemoveMessageSelectionLimit,
                RemoveMomentsAds,
                RemoveSendMediaCountLimit,
                RemoveTextStatusLengthLimit,
                RemoveVideoAds,
                SkipRewardedAds,
                DecompileBeanShellSnapshot,
                ErudaConsole,
                SpoofEnvironment,
                UploadTransparentAvatars,
                UseLegacyWalletViewInMePage,
                RemoveSignatureLimits,
                DisableLowAvailableStorageDetection,
                KillHostProcess,
                MarkAllAsRead,
                ToggleAllConversationsVisibility,
                ModuleSettings,
                OpenConversationMenu,
                SetProfileNickname,
                ClearProfileDetails,
                LaunchInternalUrls,
                AutoApproveDeviceLogin,
                BlockVoipRingtone,
                HideMeTabPageItems,
                HideMessagesAvatars,
                ImageRotation,
                ApplyDialogBackgroundBlur,
                ModifySportsStepCount,
                ModifyFriendsCount,
                UseLegacyOfficialAccountsView,
                PreventXposedDetection,
                RemoveChatMessageContextMenuItems,
                BatchRevoke,
                DownloadFilesToLocalStorage,
                DownloadImagesToLocalStorage,
                ModifyTextMessageDisplay,
                RepeatMessages,
                SaveStickersToLocalStorage,
                SaveVoicesToLocalStorage,
                DisplayMessageDetails,
                QuickRevokeAndEdit,
                RedirectDownloadPath,
                ForwardMessagesToMoments,
                DetectDeletedFriends,
            )
        )
    }
}
