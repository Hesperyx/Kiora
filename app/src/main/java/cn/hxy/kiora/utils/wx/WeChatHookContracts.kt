package cn.hxy.kiora.utils.wx

import cn.hxy.kiora.utils.reflect.findFieldOrNull
import java.lang.reflect.Constructor
import java.lang.reflect.Method

/**
 * 微信侧「被混淆结构」的实证契约。
 *
 * ## 为什么单独成文件
 *
 * [WeChatDexKit] 负责**找到**类和方法；找到之后怎么**读它的实参与字段**，是另一套
 * 知识，而且是最容易出错的那部分。WA 的两个样例（`MsgFormat`、`EmojiGame`）在
 * 微信 8.0.78 上失效，**没有一个是查询失败** —— DexKit 的字符串特征串全都唯一命中，
 * 挂掉的是「拿到方法以后按下标 / 按字段名去取」这一步。
 *
 * 这类错误特别隐蔽：查询命中、hook 挂上、日志干净，只是取到的值不对，或者
 * `!!` 抛在无人看的 catch 里。所以把读法固化成契约，让翻译阶段无处「凭感觉取」。
 *
 * ## 证据
 *
 * 下面每一条都能在 `recon/` 的反汇编输出里逐指令对上，
 * 完整证据链见 `docs/Kiora多宿主改造设计.md` §17.3 / §17.4。
 *
 * ## 设计原则
 *
 * **只依赖结构信息**（参数个数、实参下标、字段的**类型**），不依赖类名、方法名、
 * 字段名 —— 后者每版都换，前者不会。
 */
object WeChatHookContracts {

    /**
     * 发送文本组件（WA `MsgFormatHook`）的构造契约。
     *
     * 8.0.78 的混淆类名是 `com.tencent.mm.ui.chatting.component.pm`，
     * 但下面所有判断都不依赖这个类名。
     */
    object SendText {

        /**
         * 构造方法的参数个数。
         *
         * WA 假设 `paramCount(12..14)`，8.0.78 实测是 **15** → 匹配 0 个方法 →
         * `singleOrNull()` 拿 null → 整条 hook 不挂载。**照抄这个区间就是死。**
         */
        const val PARAM_COUNT = 15

        /**
         * 主发送路径（`run()` 里 `d == 0`）读的文本实参下标 —— 字段 `n`。
         *
         * 依据：`run()` 把它作为 p1 交给 `Loh0/c;->a(...)`，而该方法的 Kotlin 形参名常量
         * 正是 `"content"`（p0 是 `"toUserName"`，来自字段 `m`）。**不是** WA 那个
         * `else -> 7` —— 在 15 参形态下下标 7 是**接收者**，不是文本。
         */
        const val TEXT_ARG_MAIN = 8

        /**
         * 本地 fake 路径（`run()` 里 `d != 0`，`talker == "medianote"`）读的文本实参下标
         * —— 字段 `g`。同一路径走 `Lf51/b`（`MicroMsg.NetSceneSendMsgFake`）落库，
         * 其构造里 `e9.t1(1)`（文本类型）、`b1(p2)` 写正文。
         */
        const val TEXT_ARG_FAKE = 4

        /**
         * 取发送文本组件的构造方法；形态不符时返回 null（让 hook 安全跳过，而不是乱改）。
         *
         * 该类**只有一个构造方法**，所以按「唯一」取即可。用参数个数比对反而脆 ——
         * WA 正是被 `paramCount(12..14)` 卡死的。这里保留 [PARAM_COUNT] 断言，
         * 是把它当**兜底校验**而不是筛选条件。
         */
        fun constructorOrNull(clazz: Class<*>): Constructor<*>? =
            clazz.declaredConstructors
                .singleOrNull()
                ?.takeIf { it.parameterCount == PARAM_COUNT }
                ?.apply { isAccessible = true }

        /**
         * 把格式化结果回写到构造实参，返回是否改写成功。
         *
         * **两个下标都要写**：`run()` 按 `d:Z`（ctor index 1）二分，
         * 普通会话走 [TEXT_ARG_MAIN]，`medianote` 的本地 fake 路径走 [TEXT_ARG_FAKE]。
         * 只写一个必然漏掉另一条路径。
         *
         * [TEXT_ARG_FAKE] **仅在与原文是同一引用时才改**：这正是 8.0.78 的实证事实 ——
         * `pm` 的唯一构造点 `om;->w0(...)` 把同一个 `String` 同时灌进 arg4 与 arg8
         * （`recon/regtrace.py` 可见 arg4 ↔ `v6` @0x450 刚被写成 `v10`，
         * 而 arg8 ↔ `v10` 本身）。用「同源」这个事实做条件，比按版本号判断可靠：
         * 哪一版让 `g` 承载别的字符串，这里就自动不碰它。
         */
        fun applyFormat(args: Array<Any?>, transform: (String) -> String): Boolean {
            val original = args.getOrNull(TEXT_ARG_MAIN) as? String ?: return false
            val formatted = transform(original)
            args[TEXT_ARG_MAIN] = formatted
            if (args.getOrNull(TEXT_ARG_FAKE) === original) args[TEXT_ARG_FAKE] = formatted
            return true
        }
    }

    /**
     * 表情面板点击（WA `EmojiGameHook.MethodPanelClick`）的读取契约。
     *
     * 8.0.78 的点击方法签名是 `a(View, Context, int, sr/u0)V` —— DexKit 查询照旧唯一命中，
     * 失效的是 WA 的读法。详见 [emojiMd5OrNull]。
     */
    object EmojiClick {

        /** 点击信息所在实参下标（签名里的第 4 个参数）。 */
        const val CLICK_ARG_INDEX = 3

        /** 判别位取值：0 = 点击后直接发送表情。 */
        const val CLICK_KIND_SEND_EMOJI = 0

        /**
         * emoji 对象的接口名。
         *
         * 用它而不是字段名（8.0.78 是 `b`）：**接口名是稳定的结构信息**，
         * 字段名每版都换。`IEmojiInfo` 是微信开放接口，方法名也不会被混淆。
         */
        const val EMOJI_INFO_TYPE = "com.tencent.mm.api.IEmojiInfo"

        /** 猜拳表情的 MD5（服务端固定值，跨版本不变）。 */
        const val MD5_MORRA = "9bd1281af3a31710a45b84d736363691"

        /** 骰子表情的 MD5。 */
        const val MD5_DICE = "08f223fa83f1ca34e143d1e580252c7c"

        /**
         * 取出被点击表情的 MD5；不是「点击发送表情」时返回 null。
         *
         * ## 为什么不能照抄 WA
         *
         * WA 的写法是在 `args(3)` 上「找第一个 `final int` 字段」当判别位，再「找第一个
         * `IEmojiInfo` 字段」当表情。8.0.78 把点击信息**拆成了两个类**：
         *
         * ```
         * 基类  sr/u0  public abstract            字段 a:I（public final）  ← 判别位在这里
         * 子类  sr/g   public final extends sr/u0 字段 b:IEmojiInfo;
         *                                          字段 c:I、e:I（也全是 final int）← 干扰项
         * ```
         *
         * 于是 `args(3)` 的**声明类型是基类、运行期是子类**，WA 的一步「找第一个 final int」
         * 在子类上有 **3 个候选**（继承的 `a` 加上自身的 `c`、`e`），取到的大概率不是判别位
         * → 闸门 `infoType == 0` 结果不稳定。所以这里分两步、**各按各的类取**：
         *
         * 1. 判别位：从 `clickMethod.parameterTypes[CLICK_ARG_INDEX]`（**声明类型**，即基类）
         *    取 int 字段 —— 基类只有这一个 int 字段，无歧义。
         * 2. emoji：从 `clickObj.javaClass`（**运行期子类**）沿继承链按类型找。
         *
         * ## 判别位的语义（实证）
         *
         * 子类构造方法里由 `IEmojiInfo.p1()` 映射出判别位（`0` 或 `6`）。点击方法按它分发：
         * `0` 走「随机表情判定 → 发送表情」（`Lq72/m;->m(IEmojiInfo)Z` 判断是否随机表情，
         * 猜拳/骰子走这里）；`6` 只打日志后进资料弹窗。所以闸门值 `0` 本身是对的，
         * 错的是读它的字段。
         */
        fun emojiMd5OrNull(clickMethod: Method, clickObj: Any?): String? {
            if (clickObj == null) return null

            // 第 1 步：判别位取「声明类型」上的 int 字段，避开运行期子类上的同类型干扰字段
            val declaredType = clickMethod.parameterTypes.getOrNull(CLICK_ARG_INDEX) ?: return null
            val kindField = declaredType.findFieldOrNull {
                type = Integer.TYPE
                isStatic = false
            } ?: return null
            if (kindField.getInt(clickObj) != CLICK_KIND_SEND_EMOJI) return null

            // 第 2 步：emoji 取「运行期类型」继承链上按类型名命中的字段
            val emojiField = clickObj.javaClass.findFieldOrNull {
                typeName = EMOJI_INFO_TYPE
                isStatic = false
            } ?: return null
            val emojiInfo = emojiField.get(clickObj) ?: return null

            return readMd5(emojiInfo)
        }

        /**
         * 调 `IEmojiInfo.getMd5()`。
         *
         * 优先在**接口**上取方法再 invoke：接口是 `public`，方法也是 `public`，
         * 这样即使实现类本身不是 public（混淆后很常见），`invoke` 也不会因为
         * 访问检查失败而抛 `IllegalAccessException`。取不到接口时退回实现类自省。
         */
        private fun readMd5(emojiInfo: Any): String? = runCatching {
            val loader = emojiInfo.javaClass.classLoader
            val owner = runCatching { Class.forName(EMOJI_INFO_TYPE, false, loader) }
                .getOrElse { emojiInfo.javaClass }
            owner.getMethod("getMd5").invoke(emojiInfo) as? String
        }.getOrNull()
    }
}
