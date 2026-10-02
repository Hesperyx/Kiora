package cn.hxy.kiora.wx.hook

import androidx.compose.runtime.Composable
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.wx.conf.WxMsgFormatConfig
import cn.hxy.kiora.hook.base.BaseClickableHookItem
import cn.hxy.kiora.wx.ui.WxMsgFormatPage
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.wx.util.WeChatDexKit
import cn.hxy.kiora.wx.util.WeChatHookContracts
import java.lang.reflect.Constructor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 发送文本格式（移植自 WA `MsgFormatHook`）。
 *
 * ## 与 WA 的两处关键差异（都已实证，勿改回）
 *
 * 1. **不再自己声明 DexKit 查询、也不再用 `paramCount(12..14)` 挑构造方法**。
 *    锚点由 [WeChatDexKit]（宿主适配器注册）统一持有，这里只读缓存；
 *    构造方法按 [WeChatHookContracts.SendText.constructorOrNull] 取 ——
 *    8.0.78 上该类**只有一个构造方法、15 个参数**，WA 的 `12..14` 区间匹配 0 个，
 *    `singleOrNull()` 拿到 null 后整条 hook **静默不挂载**。这是 WA 在 8.0.78 上
 *    彻底失效的根因。
 * 2. **文本下标是 8 而不是 WA 的 `else -> 7`**，且 **arg4 也要一起写**（见下）。
 *    WA 的 7 在 15 参形态下取到的是接收者（`toUserName`），改它等于把收件人改成了格式串。
 *
 * ## 为什么 arg4 与 arg8 都要写
 *
 * `pm.run()`（8.0.78）按构造参数 1（`d:Z`）二分：
 * - `d == 0` → 走 `SendTextTask` 框架，正文读字段 `n`（**arg8**）；
 * - `d != 0`（`talker == "medianote"`，即文件传输助手）→ 走 `NetSceneSendMsgFake`
 *   本地落库，正文读字段 `g`（**arg4**）。
 *
 * 而这两个字段在唯一构造点 `om;->w0(...)` 处**拿到的是同一个 String 引用** ——
 * 所以只写一个必然漏掉另一条路径。[WeChatHookContracts.SendText.applyFormat]
 * 把「同源」这件事写成了「仅当 arg4 与原文是同一引用时才改」的条件，
 * 比按版本号分支可靠。
 *
 * 完整证据链见 `docs/Kiora多宿主改造设计.md` §17.3。
 */
@HookItemAnnotation(
    tag = "发送文本格式",
    desc = "把发出的文本按自定义格式改写，支持 \${sendText} / \${line} / \${sendTime} 占位符",
    category = HookCategory.CHAT,
    hosts = ["wechat"]
)
object WxMsgFormat : BaseClickableHookItem<WxMsgFormatConfig>(WxMsgFormatConfig.serializer()) {

    override val defaultConfig: WxMsgFormatConfig = WxMsgFormatConfig()

    private var constructor: Constructor<*>? = null

    override fun onInit(): Boolean {
        val clazz = runCatching {
            WeChatDexKit.requireClass(WeChatDexKit.MSG_FORMAT_SEND_TEXT_CLASS)
        }.getOrNull()
        if (clazz == null) {
            LogUtils.w("$name 未取到发送文本组件类，通常是还没跑过「查找方法」")
            return false
        }

        constructor = WeChatHookContracts.SendText.constructorOrNull(clazz)
        if (constructor == null) {
            // 形态不符就整条跳过，绝不退化成「随便取一个构造方法」——
            // 那正是 WA 在 8.0.78 上改错参数的翻车方式。
            LogUtils.w(
                "$name 构造方法形态不符（参数个数 != " +
                    "${WeChatHookContracts.SendText.PARAM_COUNT}），已跳过以免改错实参"
            )
            return false
        }

        return super.onInit()
    }

    override fun onHook() {
        val ctor = constructor ?: return

        ctor.hookBefore(this) { param ->
            WeChatHookContracts.SendText.applyFormat(param.args) { format(it) }
        }
    }

    private fun format(msg: String): String = config.textFormat
        .replace("\${sendText}", msg)
        .replace("\${line}", "\n")
        .replace("\${sendTime}", timeFormatter(config.timeFormat).format(Date()))

    /**
     * `SimpleDateFormat` 不是线程安全的，每次新建。
     *
     * 发送频率是人手点击的级别，构造开销可以忽略；
     * 换成共享实例反而要引入锁或 `ThreadLocal`，不值当。
     */
    private fun timeFormatter(pattern: String): SimpleDateFormat =
        SimpleDateFormat(pattern, Locale.getDefault())

    @Composable
    override fun ConfigContent(onDismiss: () -> Unit) {
        WxMsgFormatPage(config, ::updateConfig, onDismiss)
    }
}
