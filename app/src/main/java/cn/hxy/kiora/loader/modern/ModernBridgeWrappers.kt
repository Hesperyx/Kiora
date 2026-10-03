package cn.hxy.kiora.loader.modern

import cn.hxy.kiora.loader.hookapi.IHookBridge
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Member

/**
 * [IHookBridge.IMemberHookParam] 在 libxposed 通路上的实现。
 *
 * 与同目录的 [ModernHookParam] 并存而不是复用，原因有三：
 * - [thisObject] 可空。libxposed 的 `Chain.getThisObject()` 是平台类型，静态方法返回 null，
 *   而 [ModernHookParam] 声明为非空 `Any`，读它在静态方法上会抛 NPE。
 * - 多一个跨 before / after 传递的 [extra] 槽位。
 * - [result] / [throwable] 的 setter 带「跳过原始实现」副作用，供 before 阶段提前定值。
 */
internal class ModernBridgeHookParam(
    private val chain: XposedInterface.Chain
) : IHookBridge.IMemberHookParam {

    override val member: Member get() = chain.executable

    override val thisObject: Any? get() = chain.thisObject

    override var args: Array<Any?> = chain.args.toTypedArray()

    override var extra: Any? = null

    private var resultField: Any? = null
    private var throwableField: Throwable? = null

    /** before 阶段一旦给 result / throwable 赋值即为 true，引擎据此跳过 `chain.proceed`。 */
    var isSkipOriginal: Boolean = false
        private set

    override var result: Any?
        get() = resultField
        set(value) {
            resultField = value
            isSkipOriginal = true
        }

    override var throwable: Throwable?
        get() = throwableField
        set(value) {
            throwableField = value
            isSkipOriginal = true
        }

    /**
     * 由引擎在 `chain.proceed` 之后回填真实结果，供 after 阶段读取。
     *
     * 刻意不走 setter：回填不是「调用方主动设值」，不该把 [isSkipOriginal] 置真。
     */
    fun syncOutcome(result: Any?, throwable: Throwable?) {
        resultField = result
        throwableField = throwable
    }
}
