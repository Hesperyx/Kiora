package cn.hxy.kiora.loader.hookapi

import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Member
import java.lang.reflect.Method

/**
 * 与注入框架无关的挂钩原语契约。
 *
 * 与 [IHookEngine] 的分工：
 * - [IHookEngine] 是 Kiora 原生接口，用 hookBefore / hookAfter / hookReplace 三段式表达意图。
 * - [IHookBridge] 是桥接契约，以「一次注册、前后回调」为单位，供 WeKit 血统的功能包直接复用其
 *   DSL（`hookBeforeDirectly` / `hookAfterDirectly` / `hookDirectly`）而无需改写调用点。
 *
 * 两者由同一批引擎实例同时实现，底层是同一条 hook 通路，不存在第二套运行时。
 * **新增功能请优先用 [IHookEngine]。**
 */
interface IHookBridge {

    interface IMemberHookParam {
        val member: Member

        val thisObject: Any?

        var args: Array<Any?>

        var result: Any?

        var throwable: Throwable?

        /** 单次调用内跨 before / after 传递的私有槽位。 */
        var extra: Any?
    }

    interface IMemberHookCallback {
        fun beforeHookedMember(param: IMemberHookParam)

        fun afterHookedMember(param: IMemberHookParam)
    }

    interface MemberUnhookHandle {
        val member: Member

        val callback: IMemberHookCallback

        val isHookActive: Boolean

        fun unhook()
    }

    val hookBridgeName: String

    val frameworkName: String

    val frameworkVersion: String

    val frameworkVersionCode: Long

    /**
     * 注册一次前后回调。
     *
     * 同一 member 的多次注册各自独立、互不合并；一次调用内 before 与 after 拿到的是
     * **同一个** [IMemberHookParam] 实例，因此 [IMemberHookParam.extra] 可以跨阶段传递。
     */
    fun hookMethod(
        member: Member,
        callback: IMemberHookCallback,
        priority: Int = 50
    ): MemberUnhookHandle

    val isDeoptimizationSupported: Boolean

    fun deoptimize(executable: Executable): Boolean

    fun invokeOriginalMethod(method: Method, thisObject: Any?, args: Array<Any?>): Any?

    /**
     * 在已存在的实例上重放构造器原始实现（不等价于新建实例）。
     *
     * 刻意不用泛型：调用方通常只持有 `Constructor<*>`，而 `thisObject` 的静态类型与被构造类
     * 未必能对上，泛型只会强迫调用方做无意义的转型。
     */
    fun invokeOriginalConstructor(ctor: Constructor<*>, thisObject: Any, args: Array<Any?>)

    /** 绕过 hook 新建实例。旧版 Xposed 不支持。 */
    fun <T> newInstanceOrigin(constructor: Constructor<T>, vararg args: Any): T

    val hookCounter: Long

    val hookedMethods: Set<Member>
}

/**
 * [IHookBridge.MemberUnhookHandle] 的通用实现。
 *
 * 两个引擎共用：卸载动作由调用方以 lambda 传入，[isHookActive] 用本地状态跟踪，
 * 因此卸载后不会继续返回 true。
 */
internal class SimpleUnhookHandle(
    override val member: Member,
    override val callback: IHookBridge.IMemberHookCallback,
    private val onUnhook: () -> Unit
) : IHookBridge.MemberUnhookHandle {

    @Volatile
    private var active = true

    override val isHookActive: Boolean
        get() = active

    override fun unhook() {
        if (!active) return
        active = false
        onUnhook()
    }
}
