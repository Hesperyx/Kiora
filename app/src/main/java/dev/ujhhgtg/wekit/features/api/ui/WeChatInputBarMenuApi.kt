package dev.ujhhgtg.wekit.features.api.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.Button
import com.tencent.mm.pluginsdk.ui.chat.ChatFooter
import dev.ujhhgtg.wekit.R
import dev.ujhhgtg.wekit.features.core.ApiFeature
import dev.ujhhgtg.wekit.features.core.FeatureCategoryIds
import dev.ujhhgtg.wekit.features.items.chat.localizedChatString
import dev.ujhhgtg.wekit.ui.utils.findViewByChildIndexes
import dev.ujhhgtg.wekit.ui.utils.findViewWhich
import dev.ujhhgtg.wekit.utils.WeLogger

/**
 * 原生切片版聊天输入栏增强 API。
 *
 * 上游使用 Compose 弹窗与 `ImageVector`；这里改为 `AlertDialog.setItems`，
 * 保留 provider 注册、`findSendButton` / `showMenu` / `performSend` 接口。
 */
object WeChatInputBarMenuApi : ApiFeature() {

    override val technicalId = "聊天输入栏增强 API"
    override val nameRes = R.string.feature_we_chat_input_bar_menu_api_name
    override val categoryIds = listOf(FeatureCategoryIds.API)
    override val descriptionRes = R.string.feature_we_chat_input_bar_menu_api_description

    fun interface IActionItemsProvider {
        fun getActionItems(): List<ActionItem>
    }

    data class ActionItem(
        val id: String,
        val icon: Drawable?,
        val label: String,
        val isSupported: (Context, ChatFooter) -> Boolean = { _, _ -> true },
        val onClick: (Context, ChatFooter) -> Unit,
        val onLongClick: ((Context, ChatFooter) -> Unit)? = null,
    )

    private const val TAG = "WeChatInputBarMenuApi"

    private val providers = mutableSetOf<IActionItemsProvider>()

    fun addProvider(provider: IActionItemsProvider) {
        providers += provider
    }

    fun removeProvider(provider: IActionItemsProvider) {
        providers -= provider
    }

    fun findSendButton(chatFooter: ChatFooter): Button =
        chatFooter.findViewByChildIndexes(0)!!
            .findViewWhich { view ->
                view.javaClass.name == "android.widget.Button" && run {
                    val text = (view as Button).text?.toString()?.trim().orEmpty()
                    text == "发送" || text.equals("send", ignoreCase = true)
                }
            }!! as Button

    fun showMenu(context: Context, chatFooter: ChatFooter) {
        val applicableItems = providers
            .flatMap { it.getActionItems() }
            .filter { it.isSupported(context, chatFooter) }

        if (applicableItems.isEmpty()) {
            AlertDialog.Builder(context)
                .setTitle(context.localizedChatString(R.string.noncompose_chat_input_actions_title))
                .setMessage(context.localizedChatString(R.string.noncompose_chat_input_no_actions))
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            return
        }

        val labels = applicableItems.map { it.label }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle(context.localizedChatString(R.string.noncompose_chat_input_actions_title))
            .setItems(labels) { _, which ->
                val item = applicableItems[which]
                runCatching { item.onClick(context, chatFooter) }
                    .onFailure {
                        WeLogger.e(TAG, "exception while handling click for ${item.id}", it)
                    }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun performSend(chatFooter: ChatFooter) {
        findSendButton(chatFooter).performClick()
    }
}