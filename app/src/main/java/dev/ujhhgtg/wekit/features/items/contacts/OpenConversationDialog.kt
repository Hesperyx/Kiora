package dev.ujhhgtg.wekit.features.items.contacts

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import dev.ujhhgtg.wekit.R
import dev.ujhhgtg.wekit.features.api.core.WeApi
import dev.ujhhgtg.wekit.utils.android.showToast

fun showOpenConversationDialog(context: Context) {
    val input = EditText(context)
    input.hint = context.localizedContactsString(R.string.contacts_wechat_id)

    fun openOrToast(destination: WeApi.OpenContactDestination) {
        val wxId = input.text?.toString()?.trim().orEmpty()
        if (wxId.isEmpty()) {
            showToast(context, context.localizedContactsString(R.string.contacts_wechat_id_empty))
            return
        }
        WeApi.openContact(context, wxId, destination)
    }

    AlertDialog.Builder(context)
        .setTitle(context.localizedContactsString(R.string.feature_open_conversation_name))
        .setView(input)
        .setPositiveButton(context.localizedContactsString(R.string.contacts_open_homepage)) { _, _ ->
            openOrToast(WeApi.OpenContactDestination.HOMEPAGE)
        }
        .setNeutralButton(context.localizedContactsString(R.string.contacts_open_settings)) { _, _ ->
            openOrToast(WeApi.OpenContactDestination.SETTINGS)
        }
        .setNegativeButton(context.localizedContactsString(R.string.contacts_open_chat)) { _, _ ->
            openOrToast(WeApi.OpenContactDestination.CONVERSATION)
        }
        .show()
}