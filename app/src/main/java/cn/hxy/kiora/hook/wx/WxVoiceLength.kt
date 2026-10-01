package cn.hxy.kiora.hook.wx

import androidx.compose.runtime.Composable
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.conf.WxVoiceLengthConfig
import cn.hxy.kiora.hook.base.BaseClickableHookItem
import cn.hxy.kiora.ui.pages.configs.wx.WxVoiceLengthPage
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.reflect.findFieldOrNull
import cn.hxy.kiora.utils.wx.WeChatDexKit

/**
 * 语音时长（移植自 WA `VoiceLengthHook`）。
 *
 * 把「这条语音多长」（秒）改掉。目标方法 `Lv61/m1;->K1(String,Lv61/c1;)Z` 拿到
 * 一个语音存储对象，对象里有 `l:I` 字段存毫秒 —— 改它即可。
 *
 * ## 字段名 `l`：这里**必须**按名字取，不能只按类型
 *
 * 直觉上「按类型找唯一 int 字段」更抗混淆，但对这个类是**错的** ——
 * `recon/verify_wa2.py` 复算 8.0.78 的 `Lv61/c1`：**25 个实例字段里十几个是 `I`**
 * （`a f g h i l o r s t u v y`…）。只按类型取会拿到排在第一个的 `a`，
 * 改的是完全不相干的字段，而且**不会报错**。
 *
 * 所以按 WA 的写法取 `l`，并附上类型约束把范围收成「名为 `l` 的 int 字段」。
 * 这个字段名是**版本相关的假设**（§17.2 在 8.0.78 上核对过），
 * 代价很小：名字一旦变了，`findFieldOrNull` 返回 null，本方法**直接不写**，
 * 也就是退化成「这条功能不生效」，而不是往错字段里塞值。
 *
 * ## 为什么按参数个数分支
 *
 * WA 写的是 `args.size == 1 → 0`、`args.size == 2 && args[0] is String → 1`：
 * 目标方法的第二参就是那个语音对象，但不同形态下位置不同。这里保留这个判定，
 * 因为它是**按实参类型**推的（`is String`），不是猜的。
 */
@HookItemAnnotation(
    tag = "语音时长",
    desc = "自定义发送语音消息显示的时长（秒）",
    category = HookCategory.CHAT,
    hosts = ["wechat"]
)
object WxVoiceLength : BaseClickableHookItem<WxVoiceLengthConfig>(WxVoiceLengthConfig.serializer()) {

    override val defaultConfig: WxVoiceLengthConfig = WxVoiceLengthConfig()

    override fun onInit(): Boolean =
        runCatching { WeChatDexKit.requireMethod(WeChatDexKit.VOICE_LENGTH) }
            .onFailure { LogUtils.w("$name 未取到语音存储方法，通常是还没跑过「查找方法」") }
            .isSuccess

    override fun onHook() {
        WeChatDexKit.requireMethod(WeChatDexKit.VOICE_LENGTH).hookBefore(this) { param ->
            val objIndex = when {
                param.args.size == 1 -> 0
                param.args.size == 2 && param.args[0] is String -> 1
                else -> return@hookBefore
            }

            val voice = param.args.getOrNull(objIndex) ?: return@hookBefore
            val field = voice.javaClass.findFieldOrNull {
                name = VOICE_LENGTH_FIELD
                type = Integer.TYPE
                isStatic = false
            } ?: return@hookBefore

            runCatching { field.setInt(voice, config.seconds * MILLIS_PER_SECOND) }
                .onFailure { LogUtils.e(this, it) }
        }
    }

    @Composable
    override fun ConfigContent(onDismiss: () -> Unit) {
        WxVoiceLengthPage(config, ::updateConfig, onDismiss)
    }

    private const val MILLIS_PER_SECOND = 1000

    /** 语音对象里存毫秒时长的字段（8.0.78 = `Lv61/c1.l:I`）。 */
    private const val VOICE_LENGTH_FIELD = "l"
}
