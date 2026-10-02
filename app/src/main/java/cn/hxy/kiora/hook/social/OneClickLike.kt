package cn.hxy.kiora.hook.social

import android.view.View
import android.widget.ImageView
import androidx.compose.runtime.Composable
import com.tencent.mobileqq.activity.VisitorsActivity
import com.tencent.mobileqq.app.CardHandler
import com.tencent.mobileqq.data.Card
import com.tencent.mobileqq.data.CardProfile
import com.tencent.mobileqq.profile.vote.VoteHelper
import com.tencent.mobileqq.profilecard.base.component.AbsProfileHeaderComponent
import com.tencent.mobileqq.vas.api.IVasSingedApi
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.conf.OneClickLikeConfig
import cn.hxy.kiora.hook.base.BaseClickableHookItem
import cn.hxy.kiora.ui.pages.configs.OneClickLikePage
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.hook.hookReplace
import cn.hxy.kiora.utils.hook.invokeOriginal
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.utils.qq.QQCurrentEnv
import cn.hxy.kiora.utils.reflect.findField
import cn.hxy.kiora.utils.reflect.findMethod

/**
 * 一键点赞，两条路径二选一（见 [OneClickLikeConfig.mode]）。
 *
 * - 协议层（QFun 原实现）：拦 CardHandler 的发送方法，把单次请求的点赞数量改成 10 再连发 5 次；
 * - UI 点击层（移植自 TCQT OneClickLikes）：拦访客页/资料卡的点赞点击，把点赞动作重复 N 次
 *   （SVIP 20 次、普通 10 次）。
 *
 * 两条路径都在启动时装好，模式在回调里按 [config] 现读，所以改配置即时生效、无需重启。
 */
@HookItemAnnotation(
    "一键点赞",
    "一次点赞即点满当日上限，可选协议层 / UI 点击层拦截",
    HookCategory.SOCIAL
)
object OneClickLike : BaseClickableHookItem<OneClickLikeConfig>(OneClickLikeConfig.serializer()) {

    override val defaultConfig: OneClickLikeConfig = OneClickLikeConfig()

    override fun onHook() {
        hookProtocolPath()
        hookUiPath()
    }

    /** 协议层：单次请求 10 赞 × 5 次 = 50，正好是当日点赞上限。 */
    private fun hookProtocolPath() {
        runCatching {
            CardHandler::class.java.findMethod {
                returnType = void
                paramTypes(long, long, byteArr, int, int, int)
            }.hookReplace(this) { param ->
                if (config.mode != OneClickLikeConfig.MODE_PROTOCOL) {
                    return@hookReplace param.invokeOriginal()
                }

                param.args[4] = 10
                repeat(5) { param.invokeOriginal() }
                null
            }
        }.onFailure {
            LogUtils.e("OneClickLike 协议层不可用", it)
        }
    }

    /**
     * UI 点击层：访客页和资料卡是两个独立入口，都得拦。
     * TIM 没有点赞行为，直接跳过（与 TCQT 一致）。
     */
    private fun hookUiPath() {
        if (!HostInfo.isQQ) return

        runCatching {
            val vote = VoteHelper::class.java.findMethod {
                paramCount = 2
                paramTypes(CardProfile::class.java, ImageView::class.java)
            }
            val voteHelperField = VisitorsActivity::class.java.findField {
                type = VoteHelper::class.java
            }

            VisitorsActivity::class.java.findMethod {
                name = "onClick"
                paramTypes(View::class.java)
            }.hookBefore(this) { param ->
                if (config.mode != OneClickLikeConfig.MODE_UI) return@hookBefore

                val view = param.args.getOrNull(0) as? View ?: return@hookBefore
                val profile = view.tag as? CardProfile ?: return@hookBefore
                val voteHelper = voteHelperField.get(param.thisObject) ?: return@hookBefore

                repeat(likeCount()) { vote.invoke(voteHelper, profile, view) }
                param.result = Unit
            }

            AbsProfileHeaderComponent::class.java.findMethod {
                name = "handleVoteBtnClickForGuestProfile"
                paramTypes(Card::class.java)
            }.hookReplace(this) { param ->
                if (config.mode != OneClickLikeConfig.MODE_UI) {
                    return@hookReplace param.invokeOriginal()
                }

                repeat(likeCount()) { param.invokeOriginal() }
                null
            }
        }.onFailure {
            LogUtils.e("OneClickLike UI 点击层不可用", it)
        }
    }

    /** 当日点赞上限跟会员等级走：SVIP 20、普通号 10（同 TCQT）。 */
    private fun likeCount(): Int = if (isSvip()) 20 else 10

    private fun isSvip(): Boolean = runCatching {
        QQCurrentEnv.qQAppInterface
            .getRuntimeService(IVasSingedApi::class.java, "all")
            .vipStatus.isSVip
    }.getOrDefault(false)

    @Composable
    override fun ConfigContent(onDismiss: () -> Unit) {
        OneClickLikePage(
            currentConfig = config,
            onSave = ::updateConfig,
            onDismiss = onDismiss
        )
    }
}
