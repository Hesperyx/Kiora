package cn.hxy.kiora.hook.chat

import cn.hxy.kiora.host.HostEnv
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.common.ModuleScope
import cn.hxy.kiora.hook.api.MenuClickListener
import cn.hxy.kiora.hook.api.OnGetRKey
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.plugin.bean.MsgData
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.net.HttpUtils
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.utils.qq.MsgTool
import cn.hxy.kiora.utils.reflect.findMethod
import cn.hxy.kiora.utils.reflect.findMethodOrNull
import cn.hxy.kiora.utils.reflect.toClass
import java.io.File
import java.lang.reflect.Constructor
import java.lang.reflect.Method

@HookItemAnnotation(
    "快捷重发消息",
    "撤回自己发出的图文复合消息时自动将消息插入输入框",
    HookCategory.CHAT
)
object FastReSend : BaseSwitchHookItem(), MenuClickListener {

    private lateinit var recoverElements: Method

    private lateinit var ctor: Constructor<*>

    private var delegate: Any? = null

    override val menuKey: String = "[Kiora],$name,撤回重发,,2,9"

    override fun onClick(msgData: MsgData) {

        if (msgData.userUin != HostEnv.currentAccount) return

        val elements = msgData.data.elements

        MsgTool.recallMsg(msgData.contact, msgData.msgId)
        ModuleScope.launchIO { recoverElements(elements, msgData.type) }
    }

    override fun onInit(): Boolean {
        val delegateClass = "com.tencent.mobileqq.aio.input.draft.InputDraftVMDelegate".toClass
        recoverElements = delegateClass.findMethodOrNull {
            visibility = private
            returnType = void
            paramTypes(list)
        } ?: delegateClass.findMethod {
            isStatic = true
            returnType = void
            paramTypes(delegateClass, list)
        }
        ctor = delegateClass.constructors.first()
        return super.onInit()
    }

    override fun onHook() {
        ctor.hookAfter(this) { delegate = it.thisObject }
    }

    suspend fun recoverElements(elements: ArrayList<MsgElement>, chatType: Int) {
        if (HostInfo.isTIM || (HostInfo.isQQ && HostInfo.versionCode <= 13188)) {
            elements.mapNotNull {
                it.picElement
            }.forEach {
                val rkey = if (chatType == 2) OnGetRKey.groupRkey else OnGetRKey.friendRkey
                val url = "https://multimedia.nt.qq.com.cn${it.originImageUrl}$rkey"
                val fileName = "net_img_${it.md5HexStr}"
                val savePath = "${HostEnv.currentDir}cache/images/$fileName"
                if (File(savePath).exists() || HttpUtils.downloadSuspend(url, savePath)) {
                    it.fileName = savePath
                }
            }
        }
        if (recoverElements.parameterCount == 2) {
            recoverElements.invoke(null, delegate, elements)
        } else {
            recoverElements.invoke(delegate, elements)
        }
    }

}