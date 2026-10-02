package cn.hxy.kiora.hook.chat

import cn.hxy.kiora.host.HostEnv
import com.tencent.qqnt.kernel.nativeinterface.MsgAttributeInfo
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.api.GetMsgRecordListener
import cn.hxy.kiora.hook.base.BaseSwitchHookItem

@HookItemAnnotation(
    "默认气泡和字体",
    "在聊天界面中将他人的气泡和字体全部显示为默认",
    HookCategory.CHAT
)
object DefaultBubbleAndFont : BaseSwitchHookItem(), GetMsgRecordListener {

    override fun onGet(msgRecord: MsgRecord) {
        if (msgRecord.senderUin != HostEnv.currentAccount.toLong())
            msgRecord.msgAttrs = HashMap<Int, MsgAttributeInfo>()

    }

}