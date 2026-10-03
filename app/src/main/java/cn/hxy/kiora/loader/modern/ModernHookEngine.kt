package cn.hxy.kiora.loader.modern

import io.github.libxposed.api.XposedInterface
import cn.hxy.kiora.loader.hookapi.Chain
import cn.hxy.kiora.loader.hookapi.HookParam
import cn.hxy.kiora.loader.hookapi.IHookBridge
import cn.hxy.kiora.loader.hookapi.IHookEngine
import cn.hxy.kiora.loader.hookapi.Invoker
import cn.hxy.kiora.loader.hookapi.SimpleUnhookHandle
import cn.hxy.kiora.loader.hookapi.Unhook
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class ModernHookEngine(private val base: XposedInterface) : IHookEngine, IHookBridge {

    override val apiLevel: Int = base.apiVersion
    override val frameworkName: String = base.frameworkName
    override val frameworkVersion: String = base.frameworkVersion
    override val frameworkVersionCode: Long = base.frameworkVersionCode
    override val bridgeClass: Class<*>? = null

    override fun hookBefore(method: Member, priority: Int, callback: (HookParam) -> Unit): Unhook {
        val handle = base.hook(method as Executable).setPriority(priority).intercept { chain ->
            val param = ModernHookParam(chain)
            callback(param)
            if (param.isReturnEarly) return@intercept param.result
            return@intercept chain.proceed(param.args)
        }
        return Unhook { handle.unhook() }
    }

    override fun hookAfter(method: Member, priority: Int, callback: (HookParam) -> Unit): Unhook {
        val handle = base.hook(method as Executable).setPriority(priority).intercept { chain ->
            val param = ModernHookParam(chain)
            try {
                param.result = chain.proceed(param.args)
            } catch (t: Throwable) {
                param.throwable = t
            }
            callback(param)
            if (param.throwable != null) throw param.throwable!!
            return@intercept param.result
        }
        return Unhook { handle.unhook() }
    }

    override fun hookReplace(method: Member, priority: Int, callback: (Chain) -> Any?): Unhook {
        val handle = base.hook(method as Executable).setPriority(priority).intercept { chain ->
            val modernChain = ModernChain(chain)
            return@intercept callback(modernChain)
        }
        return Unhook { handle.unhook() }
    }

    override fun getInvoker(method: Member): Invoker {
        return ModernInvoker(base, method)
    }

    override fun deoptimize(method: Member): Boolean {
        return base.deoptimize(method as Executable)
    }

    override fun log(priority: Int, tag: String?, msg: String, t: Throwable?) {
        if (t != null) {
            base.log(priority, tag, msg, t)
        } else {
            base.log(priority, tag, msg)
        }
    }

    /* ==================== IHookBridge ==================== */

    /**
     * frameworkName / frameworkVersion / frameworkVersionCode 与 [IHookEngine] 同名同型，
     * 上面的 override 已同时满足两个接口，这里不重复声明。
     */
    override val hookBridgeName: String = "Kiora Modern (${base.frameworkName})"

    private val nextHookId = AtomicLong(1)
    private val hookedMemberSet: MutableSet<Member> = ConcurrentHashMap.newKeySet()

    override val hookCounter: Long get() = nextHookId.get() - 1

    override val hookedMethods: Set<Member> get() = hookedMemberSet

    /**
     * 单次 `intercept` 同时承载 before 与 after。
     *
     * 刻意不拼 `hookBefore` + `hookAfter`：`hookBefore` 在 param 提前返回时直接
     * `return@intercept` 而不调 `chain.proceed()`，那样后注册的 after 拦截器永远不会执行，
     * 与 Xposed「before 早退后 after 仍执行」的语义不符。
     */
    override fun hookMethod(
        member: Member,
        callback: IHookBridge.IMemberHookCallback,
        priority: Int
    ): IHookBridge.MemberUnhookHandle {
        val executable = member as? Executable
            ?: throw IllegalArgumentException("only method and constructor can be hooked, but got $member")

        val handle = base.hook(executable)
            .setPriority(priority)
            // 必须显式指定 PASSTHROUGH。默认的 DEFAULT 会落到 PROTECTIVE —— 拦截器抛出的异常
            // 会被框架捕获并记录，然后「当作没有 hook 一样继续」，于是 before 里设的 throwable
            // 根本传不出去（桥接自测第 3 项实测因此失败）。
            // 传统 Xposed 的 XC_MethodHook 是直抛语义，两条分支必须对齐，否则同一个功能包
            // 在 libxposed 通路与传统通路上行为不一致。
            .setExceptionMode(XposedInterface.ExceptionMode.PASSTHROUGH)
            .intercept { chain ->
                val param = ModernBridgeHookParam(chain)

                callback.beforeHookedMember(param)

                if (!param.isSkipOriginal) {
                    try {
                        param.syncOutcome(chain.proceed(param.args), null)
                    } catch (t: Throwable) {
                        // 原方法抛异常：记录下来，after 仍要执行，与 Xposed 语义一致
                        param.syncOutcome(null, t)
                    }
                }

                callback.afterHookedMember(param)

                param.throwable?.let { throw it }
                return@intercept param.result
            }

        nextHookId.getAndIncrement()
        hookedMemberSet.add(member)
        return SimpleUnhookHandle(member, callback) { handle.unhook() }
    }

    override val isDeoptimizationSupported: Boolean = true

    override fun deoptimize(executable: Executable): Boolean {
        return base.deoptimize(executable)
    }

    override fun invokeOriginalMethod(method: Method, thisObject: Any?, args: Array<Any?>): Any? {
        return base.getInvoker(method)
            .setType(XposedInterface.Invoker.Type.ORIGIN)
            .invoke(thisObject, *args)
    }

    override fun invokeOriginalConstructor(ctor: Constructor<*>, thisObject: Any, args: Array<Any?>) {
        // 以 <init>(args...)V 的形式在既有实例上重放构造器原始实现
        base.getInvoker(ctor)
            .setType(XposedInterface.Invoker.Type.ORIGIN)
            .invoke(thisObject, *args)
    }

    override fun <T> newInstanceOrigin(constructor: Constructor<T>, vararg args: Any): T {
        @Suppress("UNCHECKED_CAST")
        val instance = base.getInvoker(constructor)
            .setType(XposedInterface.Invoker.Type.ORIGIN)
            .newInstance(*args) as T
        return instance
    }
}