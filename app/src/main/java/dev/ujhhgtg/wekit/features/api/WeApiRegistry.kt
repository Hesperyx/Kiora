package dev.ujhhgtg.wekit.features.api.core

import dev.ujhhgtg.wekit.features.api.net.WeNetSceneApi
import dev.ujhhgtg.wekit.features.api.net.WePacketHelper
import dev.ujhhgtg.wekit.features.api.net.WeTransferApi
import dev.ujhhgtg.wekit.features.api.net.listener.WePacketDispatcher
import dev.ujhhgtg.wekit.features.api.ui.WeAlertDialogApi
import dev.ujhhgtg.wekit.features.api.ui.WeChatInputBarApi
import dev.ujhhgtg.wekit.features.api.ui.WeChatInputBarMenuApi
import dev.ujhhgtg.wekit.features.api.ui.WeChatMessageContextMenuApi
import dev.ujhhgtg.wekit.features.items.chat.ChatFooterHooks
import dev.ujhhgtg.wekit.features.api.ui.WeContactHeaderApi
import dev.ujhhgtg.wekit.features.api.ui.WeChatMessageViewApi
import dev.ujhhgtg.wekit.features.api.ui.WeContactPrefsScreenApi
import dev.ujhhgtg.wekit.features.api.ui.WeConversationContextMenuApi
import dev.ujhhgtg.wekit.features.api.ui.WeConversationListViewApi
import dev.ujhhgtg.wekit.features.api.ui.WeShortVideosShareMenuApi
import dev.ujhhgtg.wekit.features.api.ui.WeWebViewApi
import dev.ujhhgtg.wekit.features.api.ui.WeCurrentConversationApi
import dev.ujhhgtg.wekit.features.api.ui.WeMainActivityBeautifyApi
import dev.ujhhgtg.wekit.features.api.ui.WeHomeScreenPopupMenuApi
import dev.ujhhgtg.wekit.features.api.ui.WeStartActivityApi
import dev.ujhhgtg.wekit.features.api.ui.WeMomentsApi
import dev.ujhhgtg.wekit.features.api.ui.WeMomentsContextMenuApi
import dev.ujhhgtg.wekit.features.items.scripting_java.JavaHookApi
import dev.ujhhgtg.wekit.features.core.BaseFeature

/**
 * WeKit 服务层（API 功能）注册表。
 *
 * 与 [dev.ujhhgtg.wekit.features.WxFeatureRegistry]（用户可见 items 功能）互补：
 * 这些 ApiFeature 是**被 items 功能跨对象引用的依赖层**（例如
 * `RemoveMessageSelectionLimit` 的 matcher 引用 `WeMessageApi.classChattingDataAdapter.data`）。
 *
 * **顺序即依赖顺序**：`.data` 求值要求被引用方的委托已解析，
 * 因此列表必须按「被依赖者在前」排列：
 * - [WeMessageApi] / [WeDatabaseApi] 被 [WePacketHelper]、[WePacketDispatcher] 及多个 items 功能引用 → 最前
 * - [WePacketDispatcher] / [WeTransferApi] / [WePacketHelper] 依赖上面两者 → 最后
 *
 * 本表只收「实现 IResolveDex、有 DexKit 委托」的服务；
 * WeDatabaseListenerApi、WeUnsafeApi 无 Dex 委托，不在表内。
 */
object WeApiRegistry {

    /** 按依赖顺序排列、需要 DexKit 解析的服务层功能。 */
    val dexBacked: List<BaseFeature> = listOf(
        WeMessageApi,
        WeDatabaseApi,
        WeConversationApi,
        WeContactApi,
        WeContactLabelApi,
        WeGroupApi,
        WeAuthApi,
        WeAppMsgApi,
        WePaymentApi,
        WeServiceApi,
        WeTextStatusApi,
        WeXmlParserApi,
        WeNetSceneApi,
        WePacketDispatcher,
        WeTransferApi,
        WePacketHelper,
        WeMomentsApi,
        WeAlertDialogApi,
        WeMainActivityBeautifyApi,
    )

    /** 需要随功能加载器启动（有 Hook 副作用）的服务层功能。 */
    val startupBacked: List<BaseFeature> = listOf(
        WeChatInputBarApi,
        WeChatInputBarMenuApi,
        ChatFooterHooks,
        WeChatMessageViewApi,
        WeContactPrefsScreenApi,
        WeConversationContextMenuApi,
        WeConversationListViewApi,
        WeShortVideosShareMenuApi,
        WeWebViewApi,
        WeContactHeaderApi,
        WeCurrentConversationApi,
        WeHomeScreenPopupMenuApi,
        WeStartActivityApi,
        WeMomentsContextMenuApi,
        WeChatMessageContextMenuApi,
        JavaHookApi,
    )
}
