package cn.hxy.kiora.hook.debug

import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.reflect.clazz
import cn.hxy.kiora.utils.reflect.findMethodOrNull
import java.lang.reflect.Proxy

/** FEKit 调用内容打印，移植自 TCQT TCQTDeBug。 */
@HookItemAnnotation(
    "FEKit打印调用内容",
    "向框架日志中输出 FEKit 的消息发送与安全事件调用内容。本功能仅做调试使用，正常使用模块请勿启用",
    HookCategory.DEBUG,
    ":msf"
)
object TCQTDeBug : BaseSwitchHookItem() {

    override fun onHook() {
        hookSend()
        hookEvent()
    }

    private fun hookSend() {
        val clazz = "com.tencent.mobileqq.channel.ChannelProxyExt".clazz ?: return
        val method = clazz.findMethodOrNull {
            name = "sendMessageInner"
            paramTypes(string, byteArr, long)
        } ?: clazz.findMethodOrNull {
            name = "sendMessage"
            paramTypes(string, byteArr, long)
        } ?: return

        method.hookBefore(this) { param ->
            val cmd = param.args[0] as? String
            val body = param.args[1] as? ByteArray ?: return@hookBefore
            val callbackId = param.args[2] as? Long
            val bcmd = parseFieldOneAsString(body)

            LogUtils.i(
                "sendMessageInner Log Start\n" +
                        "cmd: $cmd\n" +
                        "bcmd: $bcmd\n" +
                        "callbackId: $callbackId\n" +
                        "body: ${body.toHexString()}\n" +
                        "sendMessageInner Log End"
            )
        }
    }

    private fun hookEvent() {
        val signClass = "com.tencent.mobileqq.sign.QQSecuritySign".clazz ?: return
        val callbackClass = "com.tencent.mobileqq.fe.EventCallback".clazz ?: return

        signClass.findMethodOrNull {
            name = "dispatchEvent"
            paramTypes(string, string, callbackClass)
        }?.hookBefore(this) { param ->
            val eventName = param.args[0] as? String
            val eventData = param.args[1] as? String
            val originalCallback = param.args[2] ?: return@hookBefore

            val proxy = Proxy.newProxyInstance(
                originalCallback.javaClass.classLoader,
                arrayOf(callbackClass)
            ) { _, method, args ->
                if (method.name == "onResult" && args?.size == 2) {
                    val code = args[0] as? Int
                    val result = (args[1] as? ByteArray)?.toString(Charsets.UTF_8).orEmpty()

                    if (result.isNotEmpty()) {
                        LogUtils.i(
                            "dispatchEvent Log Start\n" +
                                    "eventName: $eventName\n" +
                                    "eventData: $eventData\n" +
                                    "code: $code\n" +
                                    "result: $result\n" +
                                    "dispatchEvent Log End"
                        )
                    }
                }

                method.invoke(originalCallback, *(args ?: emptyArray()))
            }

            param.args[2] = proxy
        }
    }
}
