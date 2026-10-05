package dev.ujhhgtg.wekit.features.items.contacts

import androidx.activity.ComponentActivity
import dev.ujhhgtg.wekit.R
import dev.ujhhgtg.wekit.features.core.ClickableFeature
import dev.ujhhgtg.wekit.features.core.FeatureCategoryIds

object OpenConversation : ClickableFeature() {

    override val technicalId = "跳转对话"
    override val nameRes = R.string.feature_open_conversation_name
    override val categoryIds = listOf(FeatureCategoryIds.CONTACTS_GROUPS)
    override val descriptionRes = R.string.feature_open_conversation_description

    override val noSwitchWidget = true

    override fun onClick(context: ComponentActivity) {
        showOpenConversationDialog(context)
    }
}