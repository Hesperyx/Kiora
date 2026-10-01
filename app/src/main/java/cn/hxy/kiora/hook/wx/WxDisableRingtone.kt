package cn.hxy.kiora.hook.wx

import android.os.Bundle
import androidx.compose.runtime.Composable
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.conf.WxRingtoneConfig
import cn.hxy.kiora.hook.base.BaseClickableHookItem
import cn.hxy.kiora.ui.pages.configs.wx.WxRingtonePage
import cn.hxy.kiora.utils.hook.hookReplace
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.wx.WeChatDexKit

/**
 * 屏蔽通话铃声（移植自 WA `DisableRingtonePlayHook`）。
 *
 * 目标方法 `Lx44/u;->dj(Lx44/i;Landroid/os/Bundle;)Z` 返回「铃声是否已播」。
 * §17.2 已核验 `args(1)` 确为 `Bundle`（这是「可照抄下标」的一类），
 * Bundle 里 `scene == "start"` 表示开始播放，`isOutCall` 区分呼出/呼入。
 *
 * ## 为什么用 `hookReplace` 而不是 WA 的 before + `resultFalse`
 *
 * 返回值布尔的方法，最直接的语义是「让原方法返回 false」，也就是替换调用；
 * `hookReplace` 不调 `chain.proceed()` 时原方法根本不执行 ——
 * 铃声也就不会被放出来（只改返回值的话，声音可能已经响了）。
 */
@HookItemAnnotation(
    tag = "屏蔽通话铃声",
    desc = "自己拨打或接听语音/视频通话时不播放铃声",
    category = HookCategory.CHAT,
    hosts = ["wechat"]
)
object WxDisableRingtone : BaseClickableHookItem<WxRingtoneConfig>(WxRingtoneConfig.serializer()) {

    override val defaultConfig: WxRingtoneConfig = WxRingtoneConfig()

    private const val SCENE_START = "start"
    private const val KEY_SCENE = "scene"
    private const val KEY_IS_OUT_CALL = "isOutCall"

    override fun onInit(): Boolean =
        runCatching { WeChatDexKit.requireMethod(WeChatDexKit.DISABLE_RINGTONE) }
            .onFailure { LogUtils.w("$name 未取到铃声播放方法，通常是还没跑过「查找方法」") }
            .isSuccess

    override fun onHook() {
        WeChatDexKit.requireMethod(WeChatDexKit.DISABLE_RINGTONE).hookReplace(this) { chain ->
            val bundle = chain.args.getOrNull(1) as? Bundle
            if (bundle?.getString(KEY_SCENE) == SCENE_START) {
                val isOutCall = bundle.getBoolean(KEY_IS_OUT_CALL)
                val blocked = if (isOutCall) config.blockOutCall else config.blockInCall
                if (blocked) return@hookReplace false
            }
            chain.proceed()
        }
    }

    @Composable
    override fun ConfigContent(onDismiss: () -> Unit) {
        WxRingtonePage(config, ::updateConfig, onDismiss)
    }
}
