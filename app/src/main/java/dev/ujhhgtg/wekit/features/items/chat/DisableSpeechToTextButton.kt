package dev.ujhhgtg.wekit.features.items.chat

import com.tencent.mm.pluginsdk.ui.chat.ChatFooter
import dev.ujhhgtg.reflekt.utils.fastJavaMethod
import dev.ujhhgtg.wekit.R
import dev.ujhhgtg.wekit.features.core.FeatureCategoryIds
import dev.ujhhgtg.wekit.features.core.SwitchFeature

object DisableSpeechToTextButton : SwitchFeature() {

    override val technicalId = "禁用输入框快捷语音转文字"
    override val nameRes = R.string.feature_disable_speech_to_text_button_name
    override val categoryIds = listOf(FeatureCategoryIds.CHAT)
    override val descriptionRes = R.string.feature_disable_speech_to_text_button_description

    override fun onEnable() {
        // 方法解析失败时只跳过本钩子；用 `!!` 会在 onEnable 里抛 NPE，被 enable() 捕获后
        // 整个功能会被静默置为关闭（表现为开关打开但功能不生效）。
        ChatFooter::getV2TBtnLayout.fastJavaMethod?.hookBefore {
            result = null
        }
    }
}
