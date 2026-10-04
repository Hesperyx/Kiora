package cn.hxy.kiora.hook.api

import com.tencent.mobileqq.aio.msg.AIOMsgItem
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseApiHookItem
import cn.hxy.kiora.hook.base.Listener
import cn.hxy.kiora.utils.hook.hookAfter

@HookItemAnnotation("监听获取消息记录")
object OnGetMsgRecord : BaseApiHookItem<GetMsgRecordListener>() {

    override fun loadHook() {
        AIOMsgItem::class.java.getDeclaredMethod("getMsgRecord")
            .hookAfter(this) { param ->
                val msgRecord = param.result as MsgRecord
                forEachChecked { it.onGet(msgRecord) }
            }
    }
}

fun interface GetMsgRecordListener : Listener {
    fun onGet(msgRecord: MsgRecord)
}