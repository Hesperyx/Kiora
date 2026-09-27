package cn.hxy.kiora.hook.api

import com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseApiHookItem
import cn.hxy.kiora.hook.base.Listener
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.reflect.findMethod

@HookItemAnnotation("监听发送消息")
object OnSendMsg : BaseApiHookItem<SendMsgListener>() {


    @Suppress("UNCHECKED_CAST")
    override fun loadHook() {
        IKernelMsgService.CppProxy::class.java
            .findMethod {
                name = "sendMsg"
            }.hookBefore(this) { param ->

                val elements = param.args[2] as ArrayList<MsgElement>
                forEachChecked { it.onSend(elements) }
            }
    }

}

fun interface SendMsgListener : Listener {
    fun onSend(elements: ArrayList<MsgElement>)
}