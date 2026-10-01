package cn.hxy.kiora.hook.wx

import android.content.Intent
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.wx.WeChatDexKit

/**
 * 文章多开窗口（移植自 WA `MultiWebViewHook`）。
 *
 * 微信把公众号文章放进一个「模板 WebView Activity」里，且大概率是
 * `singleTask` —— 所以点第二篇会把第一篇顶掉。这里在启动前给 Intent 加两个 flag，
 * 让它以「新文档、独立任务」的形式打开，于是可以同时留多篇。
 *
 * ## 下标是核验过的
 *
 * 目标方法 `Lhc5/l;->j(Context,String,String,Intent,Bundle)V`：`args(2)` 是模板类名、
 * `args(3)` 是 Intent。§17.2 已经确认过这两个下标在 8.0.78 上成立 ——
 * 这条属于「可照抄」的一类，不像 `MsgFormat` 那样需要改写。
 */
@HookItemAnnotation(
    tag = "文章多开窗口",
    desc = "同时阅读多篇公众号文章，不再互相顶掉",
    category = HookCategory.MISC,
    hosts = ["wechat"]
)
object WxMultiWebView : BaseSwitchHookItem() {

    /** 公众号文章的模板 WebView 容器类名。 */
    private const val TMPL_WEBVIEW_CLASS = ".ui.timeline.preload.ui.TmplWebViewMMUI"

    override fun onInit(): Boolean =
        runCatching { WeChatDexKit.requireMethod(WeChatDexKit.MULTI_WEBVIEW) }
            .onFailure { LogUtils.w("$name 未取到多开入口方法，通常是还没跑过「查找方法」") }
            .isSuccess

    override fun onHook() {
        WeChatDexKit.requireMethod(WeChatDexKit.MULTI_WEBVIEW).hookBefore(this) { param ->
            if (param.args.getOrNull(2) as? String != TMPL_WEBVIEW_CLASS) return@hookBefore

            // 改的是 Intent 本身（引用传递），不需要回写 args ——
            // 但 Intent 可能为 null（重载或异常路径），取不到就放行。
            val intent = param.args.getOrNull(3) as? Intent ?: return@hookBefore
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
    }
}
