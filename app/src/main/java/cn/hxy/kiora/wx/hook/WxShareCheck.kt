package cn.hxy.kiora.wx.hook

import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.returnConstant
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.wx.util.WeChatDexKit

/**
 * 分享签名校验（移植自 WA `ShareCheckHook`）。
 *
 * 第三方 App 分享内容到微信时，微信会校验它声明的签名与包名是否匹配；
 * 不匹配就拒绝。这里让校验方法直接返回 `true`（`L...pluginsdk.model.app.i1;->b(...)Z`）。
 *
 * 锚点用 `checkAppSignature get local signature failed` 这条失败日志反查 ——
 * 它只在校验分支里出现，比类名稳（类名带 `i1` 这种混淆后缀）。
 */
@HookItemAnnotation(
    tag = "分享签名校验",
    desc = "跳过第三方应用分享到微信的签名校验",
    category = HookCategory.MISC,
    hosts = ["wechat"]
)
object WxShareCheck : BaseSwitchHookItem() {

    override fun onInit(): Boolean =
        runCatching { WeChatDexKit.requireMethod(WeChatDexKit.SHARE_CHECK) }
            .onFailure { LogUtils.w("$name 未取到签名校验方法，通常是还没跑过「查找方法」") }
            .isSuccess

    override fun onHook() {
        WeChatDexKit.requireMethod(WeChatDexKit.SHARE_CHECK).returnConstant(this, true)
    }
}
