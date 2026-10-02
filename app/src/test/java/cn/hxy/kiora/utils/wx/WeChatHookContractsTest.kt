package cn.hxy.kiora.utils.wx

import cn.hxy.kiora.utils.wx.WeChatHookContracts.EmojiClick
import cn.hxy.kiora.utils.wx.WeChatHookContracts.SendText
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Method

/**
 * [WeChatHookContracts] 的纯 JVM 单测。
 *
 * 这些契约是「查询命中之后怎么读实参和字段」的知识，WA 的两个样例正是在这一步翻车的
 * （查询全都命中，挂掉的是按下标/按字段名取）。所以这里锁的是**取值语义**，不是查询本身。
 */
class WeChatHookContractsTest {

    // ==================== 常量 canary ====================

    @Test
    fun sendText_constants_areFrozen() {
        assertEquals(15, SendText.PARAM_COUNT)
        assertEquals(8, SendText.TEXT_ARG_MAIN)
        assertEquals(4, SendText.TEXT_ARG_FAKE)
    }

    @Test
    fun emojiClick_constants_areFrozen() {
        assertEquals(3, EmojiClick.CLICK_ARG_INDEX)
        assertEquals(0, EmojiClick.CLICK_KIND_SEND_EMOJI)
        assertEquals("com.tencent.mm.api.IEmojiInfo", EmojiClick.EMOJI_INFO_TYPE)
        assertEquals("9bd1281af3a31710a45b84d736363691", EmojiClick.MD5_MORRA)
        assertEquals("08f223fa83f1ca34e143d1e580252c7c", EmojiClick.MD5_DICE)
    }

    // ==================== SendText.constructorOrNull ====================

    @Test
    fun constructorOrNull_single15Param_returnsAccessibleCtor() {
        val ctor = SendText.constructorOrNull(Ctor15::class.java)

        assertNotNull(ctor)
        assertEquals(15, ctor!!.parameterCount)
        // 契约靠 apply { isAccessible = true } 让调用方能改写实参，这条不能丢
        assertTrue(ctor.isAccessible)
    }

    @Test
    fun constructorOrNull_single14Param_returnsNull() {
        // WA 用 paramCount(12..14) 筛选，在 8.0.78 的 15 参形态下匹配 0 个 → 整条 hook 不挂载
        assertNull(SendText.constructorOrNull(Ctor14::class.java))
    }

    @Test
    fun constructorOrNull_twoCtors_returnsNull() {
        // singleOrNull()：形态不唯一时宁可放弃，也不猜
        assertNull(SendText.constructorOrNull(CtorTwo::class.java))
    }

    @Test
    fun constructorOrNull_zeroCtors_returnsNull() {
        // 具体类必有 <init>，只有接口/数组的 declaredConstructors 为空数组
        assertNull(SendText.constructorOrNull(com.tencent.mm.api.IEmojiInfo::class.java))
    }

    // ==================== SendText.applyFormat ====================

    @Test
    fun applyFormat_sameReference_arg4AndArg8_bothRewritten() {
        // 8.0.78 实证：唯一构造点把同一个 String 同时灌进 arg4 与 arg8
        val text = StringBuilder("hello").toString()
        val args = arrayOfNulls<Any?>(15)
        args[SendText.TEXT_ARG_MAIN] = text
        args[SendText.TEXT_ARG_FAKE] = text
        assertSame(text, args[4])

        assertTrue(SendText.applyFormat(args) { it.uppercase() })

        assertEquals("HELLO", args[8])
        assertEquals("HELLO", args[4])
    }

    @Test
    fun applyFormat_equalButNotSameReference_onlyArg8Rewritten() {
        // 关键用例：arg4 只在与原文「同一引用」时才改。
        // 若有人把 === 写成 == / equals，这条立刻红。
        val main = StringBuilder("hello").toString()
        val fake = String(charArrayOf('h', 'e', 'l', 'l', 'o'))
        assertNotSame("夹具前提：必须是两个不同实例", main, fake)
        assertEquals("夹具前提：值必须相等", main, fake)

        val args = arrayOfNulls<Any?>(15)
        args[SendText.TEXT_ARG_MAIN] = main
        args[SendText.TEXT_ARG_FAKE] = fake

        assertTrue(SendText.applyFormat(args) { it.uppercase() })

        assertEquals("HELLO", args[8])
        assertEquals("hello", args[4])
    }

    @Test
    fun applyFormat_arg8Null_returnsFalse_arrayUntouched() {
        val args = arrayOfNulls<Any?>(15)
        args[SendText.TEXT_ARG_FAKE] = StringBuilder("keep").toString()
        val before = args.copyOf()

        assertFalse(SendText.applyFormat(args) { it.uppercase() })
        assertArrayEquals(before, args)
    }

    @Test
    fun applyFormat_arg8NotString_returnsFalse_arrayUntouched() {
        val args = arrayOfNulls<Any?>(15)
        args[SendText.TEXT_ARG_MAIN] = 42
        val before = args.copyOf()

        assertFalse(SendText.applyFormat(args) { it.uppercase() })
        assertArrayEquals(before, args)
    }

    @Test
    fun applyFormat_emptyArray_returnsFalse_noException() {
        // getOrNull 语义：越界返回 null 而不是抛 IndexOutOfBounds
        assertFalse(SendText.applyFormat(arrayOfNulls<Any?>(0)) { it.uppercase() })
    }

    @Test
    fun applyFormat_size5_returnsFalse() {
        assertFalse(SendText.applyFormat(arrayOfNulls<Any?>(5)) { it.uppercase() })
    }

    @Test
    fun applyFormat_arg4Null_arg8String_onlyArg8Rewritten() {
        val args = arrayOfNulls<Any?>(15)
        args[SendText.TEXT_ARG_MAIN] = StringBuilder("x").toString()

        assertTrue(SendText.applyFormat(args) { it.uppercase() })

        assertEquals("X", args[8])
        assertNull(args[4])
    }

    @Test
    fun applyFormat_sizeExactly9_works() {
        // 边界：下标 8 恰好是最后一个元素
        val args = arrayOfNulls<Any?>(9)
        args[SendText.TEXT_ARG_MAIN] = StringBuilder("x").toString()

        assertTrue(SendText.applyFormat(args) { it.uppercase() })
        assertEquals("X", args[8])
    }

    @Test
    fun applyFormat_identityTransform_keepsSameReference() {
        val text = StringBuilder("hi").toString()
        val args = arrayOfNulls<Any?>(15)
        args[SendText.TEXT_ARG_MAIN] = text
        args[SendText.TEXT_ARG_FAKE] = text

        assertTrue(SendText.applyFormat(args) { it })

        assertSame(text, args[8])
        assertSame(text, args[4])
    }

    @Test
    fun applyFormat_transformThrows_propagatesAndArg8Unchanged() {
        // 契约不吞 transform 的异常；抛之前 args[8] 还没被改写，不留半改状态
        val text = StringBuilder("hi").toString()
        val args = arrayOfNulls<Any?>(15)
        args[SendText.TEXT_ARG_MAIN] = text

        assertThrows(IllegalStateException::class.java) {
            SendText.applyFormat(args) { throw IllegalStateException("boom") }
        }

        assertSame(text, args[8])
    }

    // ==================== EmojiClick.emojiMd5OrNull ====================

    private val click4: Method = ClickMethods::class.java.getDeclaredMethod(
        "click4", Any::class.java, Any::class.java, Integer.TYPE, KindBase::class.java
    )
    private val click4NoInt: Method = ClickMethods::class.java.getDeclaredMethod(
        "click4NoInt", Any::class.java, Any::class.java, Integer.TYPE, NoIntBase::class.java
    )
    private val click3: Method = ClickMethods::class.java.getDeclaredMethod(
        "click3", Any::class.java, Any::class.java, Integer.TYPE
    )

    @Test
    fun emojiMd5OrNull_nullClickObj_returnsNull() {
        assertNull(EmojiClick.emojiMd5OrNull(click4, null))
    }

    @Test
    fun emojiMd5OrNull_parameterTypesTooShort_returnsNull() {
        // getOrNull(3) 语义：参数不足 4 个时直接放弃
        assertNull(EmojiClick.emojiMd5OrNull(click3, KindSub(0, 1, 2, EmojiImpl("x"))))
    }

    @Test
    fun emojiMd5OrNull_declaredTypeHasNoIntField_returnsNull() {
        assertNull(EmojiClick.emojiMd5OrNull(click4NoInt, NoIntBase()))
    }

    @Test
    fun emojiMd5OrNull_kindIs6_returnsNull() {
        // 反向回归：extra1 = 0，WA 在运行期类上读第一个 int 会拿到它 → 误判为「发送表情」而放行。
        // 契约从声明类型（基类）读到 kind = 6 → 正确拦截。
        val obj = KindSub(kind = 6, extra1 = 0, extra2 = 0, emoji = EmojiImpl("should-not-be-read"))
        assertNull(EmojiClick.emojiMd5OrNull(click4, obj))
    }

    @Test
    fun emojiMd5OrNull_kindIsZeroWithNoisyExtras_returnsMd5() {
        // 正向回归：extra1 = 111，WA 会误拦。契约读到基类 kind = 0 → 放行。
        // 与上一条方向相反，谁把「从声明类型取判别位」改错都会红。
        val obj = KindSub(kind = 0, extra1 = 111, extra2 = 222, emoji = EmojiImpl("md5-ok"))
        assertEquals("md5-ok", EmojiClick.emojiMd5OrNull(click4, obj))
    }

    @Test
    fun emojiMd5OrNull_runtimeTypeHasNoEmojiField_returnsNull() {
        assertNull(EmojiClick.emojiMd5OrNull(click4, KindNoEmoji(0)))
    }

    @Test
    fun emojiMd5OrNull_emojiFieldNull_returnsNull() {
        assertNull(EmojiClick.emojiMd5OrNull(click4, KindSub(0, 1, 2, null)))
    }

    @Test
    fun emojiMd5OrNull_getMd5ReturnsNull_returnsNull() {
        assertNull(EmojiClick.emojiMd5OrNull(click4, KindSub(0, 1, 2, EmojiNullMd5())))
    }

    @Test
    fun emojiMd5OrNull_getMd5Throws_returnsNull_noThrow() {
        // readMd5 用 runCatching{}.getOrNull() 兜底，异常不能外泄
        assertNull(EmojiClick.emojiMd5OrNull(click4, KindSub(0, 1, 2, EmojiThrowing())))
    }

    @Test
    fun emojiMd5OrNull_emojiTakenFromRuntimeSubclass() {
        // emoji 字段只声明在子类上，能取到就说明走的是运行期类型而非声明类型
        val obj = KindSub(kind = 0, extra1 = 0, extra2 = 0, emoji = EmojiImpl("from-subclass"))
        assertEquals("from-subclass", EmojiClick.emojiMd5OrNull(click4, obj))
    }
}
