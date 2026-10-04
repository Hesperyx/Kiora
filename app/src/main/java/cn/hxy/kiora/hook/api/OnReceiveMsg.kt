package cn.hxy.kiora.hook.api

import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import com.tencent.qqnt.kernelpublic.nativeinterface.Contact
import kotlinx.coroutines.delay
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.common.ModuleScope
import cn.hxy.kiora.hook.base.BaseApiHookItem
import cn.hxy.kiora.hook.base.Listener
import cn.hxy.kiora.utils.dexkit.DexKitTask
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.qq.util.QQCurrentEnv
import cn.hxy.kiora.utils.reflect.findMethod
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.base.BaseMatcher
import java.lang.reflect.Modifier

@HookItemAnnotation("监听接收消息")
object OnReceiveMsg : BaseApiHookItem<ReceiveMsgListener>(), DexKitTask {

    @Suppress("UNCHECKED_CAST")
    override fun loadHook() {

        val msgService = requireClass("msgService")

        msgService.findMethod {
            name = "onAddSendMsg"
        }.hookAfter(this) { param ->

            val msgRecord = param.args[0] as MsgRecord

            if (msgRecord.elements.isEmpty()) return@hookAfter

            ModuleScope.launchIO("onSelfMsg") {

                delay(250)

                QQCurrentEnv.kernelMsgService?.getMsgsByMsgId(
                    Contact(msgRecord.chatType, msgRecord.peerUid, msgRecord.guildId),
                    arrayListOf(msgRecord.msgId)
                ) { _, _, arrayList ->
                    val record = arrayList.firstOrNull() ?: msgRecord
                    forEachChecked { it.onReceive(record) }
                }

            }


        }

        msgService.findMethod {
            name = "onRecvMsg"
        }.hookAfter(this) { param ->

            val msgRecords = param.args[0] as ArrayList<MsgRecord>

            if (msgRecords[0].elements.isEmpty()) return@hookAfter

            forEachChecked { it.onReceive(msgRecords[0]) }
        }
    }

    override fun getQueryMap(): Map<String, BaseMatcher> = mapOf(
        "msgService" to FindClass().apply {
            searchPackages("com.tencent.qqnt.msg")
            excludePackages("com.tencent.qqnt.msg.migration")
            matcher {
                modifiers(Modifier.FINAL)
                methods {
                    add { name("onRecvMsg") }
                    add { name("onAddSendMsg") }
                }
            }
        }
    )
}

fun interface ReceiveMsgListener : Listener {
    fun onReceive(msgRecord: MsgRecord)
}