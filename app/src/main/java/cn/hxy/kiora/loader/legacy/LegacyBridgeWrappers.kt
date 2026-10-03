package cn.hxy.kiora.loader.legacy

import cn.hxy.kiora.loader.hookapi.IHookBridge
import de.robv.android.xposed.XC_MethodHook
import java.lang.reflect.Member

/**
 * [IHookBridge.IMemberHookParam] 在传统 Xposed 通路上的实现。
 *
 * 直接透传到 [XC_MethodHook.MethodHookParam]：result / throwable 的「跳过原始实现」语义
 * 由 Xposed 原生处理，这里不需要额外状态。
 *
 * [param] 被置空表示该次调用已结束（after 已跑完），此后任何访问都是编程错误。
 */
internal class LegacyBridgeHookParam(
    private var param: XC_MethodHook.MethodHookParam?
) : IHookBridge.IMemberHookParam {

    override val member: Member
        get() = checkLifecycle().method

    override val thisObject: Any?
        get() = checkLifecycle().thisObject

    override var args: Array<Any?>
        get() = checkLifecycle().args
        set(value) {
            checkLifecycle().args = value
        }

    override var result: Any?
        get() = checkLifecycle().result
        set(value) {
            checkLifecycle().result = value
        }

    override var throwable: Throwable?
        get() = checkLifecycle().throwable
        set(value) {
            checkLifecycle().throwable = value
        }

    override var extra: Any? = null

    /** after 跑完后断开对宿主回调对象的引用，供 GC。 */
    fun clear() {
        param = null
        extra = null
    }

    private fun checkLifecycle(): XC_MethodHook.MethodHookParam =
        param ?: error("attempt to access hook param after destroyed")
}

/**
 * 单次 [XC_MethodHook] 同时承载 before 与 after，两次回调共享同一个 [LegacyBridgeHookParam]。
 *
 * 共享靠 `MethodHookParam.setObjectExtra` 实现：before 存入、after 取回。这是 WeKit
 * `Xp51HookWrapper` 的做法，也是 [IHookBridge.IMemberHookParam.extra] 能跨阶段传递的前提。
 *
 * [tag] 按 hookId 唯一：同一 member 被多次 hook 时各自的回调都会跑，共用 tag 会互相覆盖。
 */
internal class LegacyBridgeHookCallback(
    private val callback: IHookBridge.IMemberHookCallback,
    priority: Int,
    hookId: Long
) : XC_MethodHook(priority) {

    private val tag = "kiora_bridge_$hookId"

    override fun beforeHookedMethod(param: XC_MethodHook.MethodHookParam) {
        val bridgeParam = LegacyBridgeHookParam(param)
        param.setObjectExtra(tag, bridgeParam)
        callback.beforeHookedMember(bridgeParam)
    }

    override fun afterHookedMethod(param: XC_MethodHook.MethodHookParam) {
        val bridgeParam = param.getObjectExtra(tag) as? LegacyBridgeHookParam
            ?: throw AssertionError("bridge hook param is null, tag: $tag")

        callback.afterHookedMember(bridgeParam)

        param.setObjectExtra(tag, null)
        bridgeParam.clear()
    }
}
