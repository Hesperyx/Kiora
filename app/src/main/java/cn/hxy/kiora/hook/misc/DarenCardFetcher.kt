package cn.hxy.kiora.hook.misc

import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.common.ModuleScope
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.ui.Toasts
import cn.hxy.kiora.utils.reflect.ClassUtils
import cn.hxy.kiora.utils.reflect.findMethodOrNull
import com.tencent.mobileqq.mini.servlet.MiniAppSSOCmdHelper
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import NS_MINI_INTERFACE.INTERFACE
import java.util.WeakHashMap

/**
 * 自动领取达人补登卡（移植自 TCQT 的 CardFetcher）。
 *
 * 达人页是个 WebView，进页面后补登卡仍要手动点一次，或者去玩那个经常失灵的小游戏。
 * 这里 hook WebView 的加载完成回调，认出达人页就替用户把 `JudgeTiming` 上报发出去。
 *
 * 两处与 TCQT 不同的取舍：
 *
 * 1. TCQT 只在 TOOL 进程安装。进程名硬编码一旦跟不上宿主就静默失效，而这里挂的
 *    只是一个页面回调 —— 全进程安装的代价可以忽略，换掉一整类「换版本就没反应」的故障。
 * 2. 补登请求的参数照搬自 TCQT，其字段含义是反推出来的（原注释就写明「未必准确」）。
 *    这些值不影响本功能的正确性判断，但换 QQ 版本后若领不到卡，先怀疑这里。
 */
@HookItemAnnotation(
    "自动领取达人补登卡",
    "打开 QQ 达人页时自动领取当天的补登卡，不用再玩小游戏",
    HookCategory.MISC,
    "All"
)
object DarenCardFetcher : BaseSwitchHookItem() {

    /** 已触发过的页面，避免同一次加载反复上报。弱引用跟着 WebView 释放。 */
    private val triggerHistory = WeakHashMap<Any, String>()

    override fun onInit(): Boolean {
        if (ClassUtils.loadClassOrNull(WEBVIEW_CLIENT) == null) {
            LogUtils.w("[$name] 宿主里没有 ${WEBVIEW_CLIENT}，功能不启用")
            return false
        }
        if (ClassUtils.loadClassOrNull(MINI_SVC_HELPER) == null) {
            LogUtils.w("[$name] 宿主里没有 ${MINI_SVC_HELPER}，补登请求发不出去")
            return false
        }
        return true
    }

    override fun onHook() {
        val clientClass = ClassUtils.loadClassOrNull(WEBVIEW_CLIENT) ?: return

        val onPageFinished = clientClass.findMethodOrNull {
            name = "onPageFinished"
            paramTypes(WebView::class.java, String::class.java)
        } ?: run {
            LogUtils.w("[$name] ${clientClass.simpleName} 上没有 onPageFinished(WebView, String)")
            return
        }

        onPageFinished.hookAfter(this) { param ->
            val url = param.args.getOrNull(1) as? String ?: return@hookAfter
            if (!url.startsWith(DAREN_URL_PREFIX)) return@hookAfter

            val webView = param.args.getOrNull(0) ?: return@hookAfter
            if (!markTriggered(webView, url)) return@hookAfter

            LogUtils.i("[$name] 命中达人页，${TRIGGER_DELAY_MS}ms 后领取补登卡")
            ModuleScope.launchMainDelayed(TRIGGER_DELAY_MS) { claimCard() }
        }
    }

    /** 同一个 WebView 反复加载同一个地址时只认第一次。 */
    private fun markTriggered(webView: Any, url: String): Boolean = synchronized(triggerHistory) {
        if (triggerHistory[webView] == url) return false
        triggerHistory[webView] = url
        true
    }

    private fun claimCard() {
        runCatching {
            val nowSeconds = System.currentTimeMillis() / 1000

            val req = INTERFACE.StJudgeTimingReq().apply {
                appid.set(DAREN_APPID)
                factType.set(FACT_TYPE_TIMED_REPORT)
                duration.set(REPORT_DURATION_SECONDS)
                reportTime.set(nowSeconds)
                totalTime.set(DAILY_TOTAL_SECONDS)
                launchId.set(nowSeconds.toString())
                via.set(VIA)
                appType.set(APP_TYPE)
                scene.set(SCENE)
                afterCertify.set(CERTIFIED)
                AdsTotalTime.set(0)
                sourceID.set("")
            }

            MiniAppSSOCmdHelper.sendSSOCmdRequest(
                CMD_JUDGE_TIMING,
                DAREN_APPID,
                req,
                INTERFACE.StJudgeTimingRsp::class.java,
                null
            )

            Toasts.toast("已领取补登卡")
        }.onFailure {
            LogUtils.e("[$name] 领取补登卡失败", it)
        }
    }

    private const val WEBVIEW_CLIENT = "com.tencent.smtt.sdk.WebViewClient"
    private const val MINI_SVC_HELPER = "com.tencent.mobileqq.mini.servlet.MiniAppSSOCmdHelper"

    private const val DAREN_URL_PREFIX = "https://ti.qq.com/qqdaren/index"
    private const val CMD_JUDGE_TIMING = "LightAppSvc.mini_app_growguard.JudgeTiming"

    /** 达人小程序的 AppID，同时用作上报请求里的 appid。 */
    private const val DAREN_APPID = "1112173744"

    /** 等页面自己的初始化跑完再上报，早于这个点发出去会被判定为无效。 */
    private const val TRIGGER_DELAY_MS = 1500L

    /** 以下取值照搬自 TCQT，含义由 AI 反推、未逐项验证。 */
    private const val FACT_TYPE_TIMED_REPORT = 13
    private const val REPORT_DURATION_SECONDS = 32
    private const val DAILY_TOTAL_SECONDS = 300
    private const val VIA = "2016_4"
    private const val APP_TYPE = 1
    private const val SCENE = 2014
    private const val CERTIFIED = 1
}
