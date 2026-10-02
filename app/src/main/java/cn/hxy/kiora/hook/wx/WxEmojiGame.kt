package cn.hxy.kiora.hook.wx

import cn.hxy.kiora.host.HostEnv
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.common.ModuleScope
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.loader.hookapi.Chain
import cn.hxy.kiora.ui.components.dialogs.CenterDialogContainerNoButton
import cn.hxy.kiora.ui.core.compatibility.KioraCenterDialog
import cn.hxy.kiora.ui.core.theme.KioraTheme
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.hook.hookReplace
import cn.hxy.kiora.utils.hook.invokeOriginal
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.wx.WeChatDexKit
import cn.hxy.kiora.utils.wx.WeChatHookContracts
import java.lang.reflect.Method

/**
 * 表情游戏（移植自 WA `EmojiGameHook`）：在猜拳 / 骰子点击时弹出选择框，出想要的结果。
 *
 * 两条锚点配合：[WeChatDexKit.EMOJI_GAME_CLICK] 判「点了猜拳还是骰子」，
 * [WeChatDexKit.EMOJI_GAME_RANDOM] 改「出什么」。
 *
 * ## 与 WA 的差异（实证，勿改回）
 *
 * WA 读点击信息的两步在 8.0.78 上都不可靠，原因**不是查询失败**，而是
 * 混淆产物把结构拆了：
 *
 * ```
 * 基类  sr/u0  public abstract            字段 a:I              ← 判别位在这里
 * 子类  sr/g   public final extends sr/u0 字段 b:IEmojiInfo;（+ c:I、e:I）
 * ```
 *
 * `args(3)` 的**声明类型是基类、运行期是子类**，于是：
 * 1. WA 的 `firstField { FINAL; Int }` 在子类上有 3 个候选（继承的 `a` 加自身的
 *    `c`、`e`），取到的大概率不是判别位；
 * 2. `IEmojiInfo` 在**子类**上，得沿运行期类的继承链按**类型**找。
 *
 * 这两步都收进了 [WeChatHookContracts.EmojiClick]：判别位从**声明类型**取
 * （基类只有这一个 int 字段），emoji 从**运行期类**取（按接口名匹配）。
 *
 * ## 为什么没有配置页
 *
 * 选什么就是这一次点击的事，当场在弹窗里定 —— 与 QQ 侧 `CustomRandomFace` 一致。
 * 所以本项是纯开关（[BaseSwitchHookItem]），预设只是一个**临时值**、
 * 不落盘、也没有设置页；见 [pending]。
 *
 * ## 为什么走 `hookReplace` 且必须在弹窗回调里用 `invokeOriginal`
 *
 * 本框架的 `hookBefore` 没有「先拦下、之后再补调原方法」的原生支持 ——
 * 拦下就得自己控制原方法的调用时机，那是 `hookReplace` 的语义。
 *
 * 但弹窗是**异步**的：本回调一返回，拦截就结束了，此时 `chain` 已失效
 * （再用 `chain.proceed()` 会抛 `Chain cannot be used after the interception ends`）。
 * 所以补调一律走 [invokeOriginal]（底层 `Method.callOriginal`），
 * 它只依赖同步捕获下来的 `method / thisObject / args`，与拦截是否结束无关。
 */
@HookItemAnnotation(
    tag = "表情游戏",
    desc = "点击猜拳或骰子时弹出选择框，指定这一次出什么",
    category = HookCategory.MISC,
    hosts = ["wechat"]
)
object WxEmojiGame : BaseSwitchHookItem() {

    /** 猜拳取值：0 剪刀 / 1 石头 / 2 布 —— 与宿主随机方法的入参类型 `2` 对应。 */
    private val morraNames = listOf("剪刀", "石头", "布")

    /** 骰子取值：0~5 对应一~六 —— 与宿主随机方法的入参类型 `5` 对应。 */
    private val diceNames = listOf("一", "二", "三", "四", "五", "六")

    private var randomMethod: Method? = null
    private var clickMethod: Method? = null

    /**
     * 本次点击预设的结果，被随机方法消费后立即清空；`null` 表示「交给宿主随机」。
     *
     * 只在弹窗点选到「补调原点击」之间短暂存活，随机方法正是原点击链路上的一环，
     * 所以无需落盘、也无需配置页。
     */
    private var pending: Int? = null

    /** 宿主随机方法的入参类型：2 = 猜拳、5 = 骰子。 */
    private const val TYPE_MORRA = 2
    private const val TYPE_DICE = 5

    override fun onInit(): Boolean {
        randomMethod = runCatching {
            WeChatDexKit.requireMethod(WeChatDexKit.EMOJI_GAME_RANDOM)
        }.getOrNull()
        clickMethod = runCatching {
            WeChatDexKit.requireMethod(WeChatDexKit.EMOJI_GAME_CLICK)
        }.getOrNull()

        if (randomMethod == null || clickMethod == null) {
            LogUtils.w(
                "$name 锚点缺失（random=${randomMethod != null}, click=${clickMethod != null}），" +
                    "通常是还没跑过「查找方法」"
            )
            return false
        }
        return super.onInit()
    }

    override fun onHook() {
        val random = randomMethod ?: return
        val click = clickMethod ?: return

        random.hookAfter(this) { param ->
            val type = param.args.getOrNull(0) as? Int ?: return@hookAfter
            if (type != TYPE_MORRA && type != TYPE_DICE) return@hookAfter
            // 没预设就保持宿主原本的随机结果
            param.result = pending ?: return@hookAfter
            pending = null
        }

        click.hookReplace(this) { chain ->
            val md5 = WeChatHookContracts.EmojiClick.emojiMd5OrNull(
                chain.method as? Method ?: return@hookReplace chain.proceed(),
                chain.args.getOrNull(WeChatHookContracts.EmojiClick.CLICK_ARG_INDEX)
            )
            val (title, names) = when (md5) {
                WeChatHookContracts.EmojiClick.MD5_MORRA -> "选择猜拳" to morraNames
                WeChatHookContracts.EmojiClick.MD5_DICE -> "选择骰子" to diceNames
                else -> return@hookReplace chain.proceed()
            }
            showChooser(title, names, chain)
            null
        }
    }

    /**
     * 弹一个单选列表，点选后写回本次预设并补调原点击。
     *
     * 列表首项固定是「随机」：它不写预设（[pending] 置空），原点击照常跑，
     * 出什么交回宿主；其余项写 `index - 1`（列表比 [names] 多出一个「随机」头）。
     *
     * [HostEnv.activity] 只是沿用了类名：它读的是 ActivityThread 的
     * `mActivities`，本身与宿主无关，微信侧同样可用。
     */
    private fun showChooser(title: String, names: List<String>, chain: Chain) {
        ModuleScope.launchMain {
            val activity = HostEnv.activity
            if (activity == null) {
                // 拿不到界面就没有可弹的窗 —— 直接放行比静默吃掉点击好。
                resume(chain)
                return@launchMain
            }

            KioraCenterDialog(activity) { dismiss ->
                CenterDialogContainerNoButton(title) {
                    SelectionList(listOf("随机") + names) { index ->
                        pending = if (index == 0) null else index - 1
                        dismiss()
                        resume(chain)
                    }
                }
            }.show()
        }
    }

    /** 补调被拦下的原点击。异常吞进日志，不影响弹窗流程。 */
    private fun resume(chain: Chain) {
        runCatching { chain.invokeOriginal() }
            .onFailure { LogUtils.e(this, it) }
    }
}

/**
 * 居中弹窗里的单选列表。
 *
 * 与 QQ 侧 `CustomRandomFace` 的同名组件是**同一套视觉**（各 hook 自带弹窗内容，
 * 与该组件在两处的用途一致）：一列可点的居中条目，点谁回调谁的下标。
 */
@Composable
private fun SelectionList(items: List<String>, onItemClick: (Int) -> Unit) {
    val colors = KioraTheme.colors
    LazyColumn(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
    ) {
        itemsIndexed(items) { index, item ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.background)
                    .clickable { onItemClick(index) }
                    .padding(20.dp, 16.dp),
                Alignment.Center
            ) {
                Text(item, fontSize = 16.sp, color = colors.textPrimary)
            }
        }
    }
}
