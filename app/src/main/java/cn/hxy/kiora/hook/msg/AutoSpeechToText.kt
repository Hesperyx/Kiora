package cn.hxy.kiora.hook.msg

import com.tencent.mobileqq.aio.msglist.holder.component.ptt.AIOPttContentComponent
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.returnConstant
import cn.hxy.kiora.utils.qq.HostInfo
import cn.hxy.kiora.utils.reflect.findMethod

@HookItemAnnotation(
    "语音消息自动转文本",
    "收到语音消息时自动转换为文字显示",
    HookCategory.MSG
)
object AutoSpeechToText : BaseSwitchHookItem() {

    override fun onInit() = HostInfo.isQQ

    override fun onHook() {
        AIOPttContentComponent::class.java.findMethod {
            returnType = boolean
            paramTypes(boolean)
        }.returnConstant(this, true)
    }
}