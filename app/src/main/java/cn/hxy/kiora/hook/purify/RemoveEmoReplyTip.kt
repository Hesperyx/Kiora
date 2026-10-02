package cn.hxy.kiora.hook.purify

import com.tencent.mobileqq.aio.msg.AIOMsgItem
import com.tencent.mobileqq.aio.msglist.holder.component.graptips.common.CommonGrayTipsComponent
import com.tencent.qqnt.kernelpublic.nativeinterface.Contact
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.qq.util.QQCurrentEnv
import cn.hxy.kiora.utils.reflect.findMethod
import java.lang.reflect.Method

@HookItemAnnotation(
    "移除表情回应提示",
    "移除所有表情回应的灰字提示",
    HookCategory.PURIFY
)
object RemoveEmoReplyTip : BaseSwitchHookItem() {

    private lateinit var handleUIState: Method

    override fun onInit(): Boolean {
        handleUIState = CommonGrayTipsComponent::class.java
            .findMethod {
                returnType = void
                paramTypes(int, AIOMsgItem::class.java, list)
            }
        return super.onInit()
    }

    override fun onHook() {

        handleUIState.hookAfter(this) { param ->
            val aioMsgItem = param.args[1] as AIOMsgItem
            val msgRecord = aioMsgItem.msgRecord
            val grayTip = msgRecord.elements.firstOrNull() ?: return@hookAfter
            if (grayTip.grayTipElement.xmlElement?.toString()?.contains("回应了你的") ?: false) {
                QQCurrentEnv.kernelMsgService?.deleteMsg(
                    Contact(msgRecord.chatType, msgRecord.peerUid, ""),
                    arrayListOf(msgRecord.msgId),
                    null
                )
            }
        }
    }

}