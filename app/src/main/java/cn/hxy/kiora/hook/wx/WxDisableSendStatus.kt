package cn.hxy.kiora.hook.wx

import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.doNothing
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.wx.WeChatDexKit

/**
 * 禁止发送状态（移植自 WA `DisableSendStatusHook`）。
 *
 * 聊天框内容变化时会向服务端发「正在输入中」信令 —— 对端能看见你在打字。
 * 这里把发信令的方法 `L...chatting.component.cn;->u0(I)V` 置空。
 *
 * 之所以叫 `u0(I)` 这种名字也要挂：类名方法名每版都换，锚点靠的是
 * `MicroMsg.SignallingComponent` + `[doDirectSend] mChattingContext is null!`
 * 这一对日志串（见 [WeChatDexKit.DISABLE_SEND_STATUS]）。
 */
@HookItemAnnotation(
    tag = "禁止发送状态",
    desc = "聊天框输入时不向对方显示「正在输入」",
    category = HookCategory.CHAT,
    hosts = ["wechat"]
)
object WxDisableSendStatus : BaseSwitchHookItem() {

    override fun onInit(): Boolean =
        runCatching { WeChatDexKit.requireMethod(WeChatDexKit.DISABLE_SEND_STATUS) }
            .onFailure { LogUtils.w("$name 未取到发信令方法，通常是还没跑过「查找方法」") }
            .isSuccess

    override fun onHook() {
        WeChatDexKit.requireMethod(WeChatDexKit.DISABLE_SEND_STATUS).doNothing(this)
    }
}
