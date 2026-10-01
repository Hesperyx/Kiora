package cn.hxy.kiora.hook.wx

import androidx.compose.runtime.Composable
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.conf.WxSportStepConfig
import cn.hxy.kiora.hook.base.BaseClickableHookItem
import cn.hxy.kiora.ui.pages.configs.wx.WxSportStepPage
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.wx.WeChatDexKit

/**
 * 运动步数（移植自 WA `SportStepHook`）。
 *
 * 目标 `Lcom/tencent/mm/plugin/sport/model/d;->a()J` 是「取今天的步数」，
 * 改它的返回值即可。§17.2 核验过它确实返回 `long`，与 `minOf(...)` 的写法相符。
 *
 * ## 上限 98800 不是随便写的
 *
 * 微信运动自身的上限是 98800，超了会被服务端/UI 压回来，看起来像「功能失效」。
 * 所以这里也先取小 —— 与 WA 一致。
 *
 * 生效时机：微信运动页面会缓存步数，所以改完**要重新进几次微信运动**才会刷新，
 * 这一点写在描述里，免得用户以为没生效。
 */
@HookItemAnnotation(
    tag = "运动步数",
    desc = "自定义微信运动展示的步数，改完需多进几次微信运动刷新",
    category = HookCategory.MISC,
    hosts = ["wechat"]
)
object WxSportStep : BaseClickableHookItem<WxSportStepConfig>(WxSportStepConfig.serializer()) {

    override val defaultConfig: WxSportStepConfig = WxSportStepConfig()

    /** 微信运动自身上限。 */
    private const val MAX_STEP = 98800L

    override fun onInit(): Boolean =
        runCatching { WeChatDexKit.requireMethod(WeChatDexKit.SPORT_STEP) }
            .onFailure { LogUtils.w("$name 未取到步数方法，通常是还没跑过「查找方法」") }
            .isSuccess

    override fun onHook() {
        WeChatDexKit.requireMethod(WeChatDexKit.SPORT_STEP).hookAfter(this) { param ->
            param.result = minOf(config.step, MAX_STEP)
        }
    }

    @Composable
    override fun ConfigContent(onDismiss: () -> Unit) {
        WxSportStepPage(config, ::updateConfig, onDismiss)
    }
}
