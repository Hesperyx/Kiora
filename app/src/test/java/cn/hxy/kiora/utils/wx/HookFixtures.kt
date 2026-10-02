package cn.hxy.kiora.utils.wx

import com.tencent.mm.api.IEmojiInfo

/**
 * 单测夹具。
 *
 * 每个场景用**专属类**：`ReflectCache` 会把「查不到」缓存成 NOT_FOUND，
 * 而缓存键含目标类名，复用同一个类会让不同场景互相污染。
 *
 * 判别位一律用 `@JvmField val x: Int`（编译成 `public final int`，无 getter）：
 * **不能写成 `Int?`** —— 装箱 `Integer` 也会被 `type = int` 匹配到
 * （见 `ReflectExtensions.isCompatibleWith` 的 primitiveWrapperMap 分支），
 * 随后 `Field.getInt` 会抛 `IllegalArgumentException`。
 */

// ---- SendText.constructorOrNull ----

/**
 * 唯一 15 参构造 —— 契约期望的形态。
 * 不能有默认参数值：那会额外生成 `(…, int mask, DefaultConstructorMarker)` 合成构造，
 * 使 `declaredConstructors` 变成 2 个，`singleOrNull()` 直接失效。
 */
@Suppress("UNUSED_PARAMETER")
class Ctor15(
    p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?,
    p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?,
    p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?,
)

/** 唯一 14 参构造 —— WA 的 `paramCount(12..14)` 会命中它，但契约要求恰好 15。 */
@Suppress("UNUSED_PARAMETER")
class Ctor14(
    p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?,
    p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?,
    p10: Any?, p11: Any?, p12: Any?, p13: Any?,
)

/** 两个构造方法 —— `singleOrNull()` 应当返回 null。 */
@Suppress("UNUSED_PARAMETER")
class CtorTwo(p0: Any?) {
    constructor(p0: Any?, p1: Any?) : this(p0)
}

// ---- EmojiClick.emojiMd5OrNull ----

/** 判别位基类（对应 8.0.78 的 `sr/u0`）：只有一个 int 实例字段。 */
open class KindBase(@JvmField val kind: Int)

/**
 * 运行期子类（对应 `sr/g`）。
 *
 * `extra1` / `extra2` 不是摆设：它们是 WA「在运行期类上找第一个 final int」的干扰项，
 * 专门用来锁住「判别位必须从**声明类型**取」这条契约。
 */
class KindSub(
    kind: Int,
    @JvmField val extra1: Int,
    @JvmField val extra2: Int,
    @JvmField val emoji: IEmojiInfo?,
) : KindBase(kind)

/** 子类但没有 emoji 字段。 */
class KindNoEmoji(kind: Int) : KindBase(kind)

/**
 * 完全没有 int 字段的类。
 *
 * 绝不要用 `String::class.java` 之类 JDK 类代替：`findFieldOrNull` 会 `setAccessible(true)`，
 * 而 `java.base` 未 open，会抛 `InaccessibleObjectException`。
 */
open class NoIntBase

// ---- emoji 实现 ----

class EmojiImpl(private val md5: String) : IEmojiInfo {
    override fun getMd5(): String = md5
}

class EmojiNullMd5 : IEmojiInfo {
    override fun getMd5(): String? = null
}

class EmojiThrowing : IEmojiInfo {
    override fun getMd5(): String? = throw IllegalStateException("boom")
}

// ---- Method 夹具 ----

/**
 * 只为提供 `parameterTypes[3]`。
 *
 * 前 3 个参数故意用 `Any?`：契约只读第 4 个参数的类型、从不调用方法本身，
 * 所以这里不需要 `View` / `Context`，测试才能跑在纯 JVM 上。
 */
@Suppress("UNUSED_PARAMETER")
class ClickMethods {
    fun click4(p0: Any?, p1: Any?, p2: Int, p3: KindBase) = Unit
    fun click4NoInt(p0: Any?, p1: Any?, p2: Int, p3: NoIntBase) = Unit
    fun click3(p0: Any?, p1: Any?, p2: Int) = Unit
}
