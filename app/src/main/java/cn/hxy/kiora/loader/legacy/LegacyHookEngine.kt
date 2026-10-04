package cn.hxy.kiora.loader.legacy

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import cn.hxy.kiora.loader.hookapi.Chain
import cn.hxy.kiora.loader.hookapi.HookParam
import cn.hxy.kiora.loader.hookapi.IHookBridge
import cn.hxy.kiora.loader.hookapi.IHookEngine
import cn.hxy.kiora.loader.hookapi.Invoker
import cn.hxy.kiora.loader.hookapi.SimpleUnhookHandle
import cn.hxy.kiora.loader.hookapi.Unhook
import cn.hxy.kiora.utils.reflect.getStaticObject
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class LegacyHookEngine : IHookEngine, IHookBridge {

    override val apiLevel: Int = XposedBridge.getXposedVersion()
    override val frameworkName: String = XposedBridge::class.java.getStaticObject("TAG") as String
    override val frameworkVersion: String = XposedBridge.getXposedVersion().toString()
    override val frameworkVersionCode: Long = XposedBridge.getXposedVersion().toLong()
    override val bridgeClass: Class<*> = XposedBridge::class.java

    override fun hookBefore(method: Member, priority: Int, callback: (HookParam) -> Unit): Unhook {
        val unhook = XposedBridge.hookMethod(method, object : XC_MethodHook(priority) {
            override fun beforeHookedMethod(param: MethodHookParam) {
                callback(LegacyHookParam(param))
            }
        })
        return Unhook { unhook.unhook() }
    }

    override fun hookAfter(method: Member, priority: Int, callback: (HookParam) -> Unit): Unhook {
        val unhook = XposedBridge.hookMethod(method, object : XC_MethodHook(priority) {
            override fun afterHookedMethod(param: MethodHookParam) {
                callback(LegacyHookParam(param))
            }
        })
        return Unhook { unhook.unhook() }
    }

    override fun hookReplace(method: Member, priority: Int, callback: (Chain) -> Any?): Unhook {
        val unhook = XposedBridge.hookMethod(method, object : XC_MethodReplacement(priority) {
            override fun replaceHookedMethod(param: MethodHookParam): Any? {
                return callback(LegacyChain(param))
            }
        })
        return Unhook { unhook.unhook() }
    }

    override fun getInvoker(method: Member): Invoker {
        return LegacyInvoker(method)
    }

    override fun deoptimize(method: Member): Boolean {
        return false
    }

    override fun log(priority: Int, tag: String?, msg: String, t: Throwable?) {
        val finalMsg = if (tag.isNullOrEmpty()) msg else "$tag $msg"
        XposedBridge.log(finalMsg)

        if (t != null) {
            XposedBridge.log(t)
        }
    }

    /* ==================== IHookBridge ==================== */

    /**
     * frameworkName / frameworkVersion / frameworkVersionCode 与 [IHookEngine] 同名同型，
     * 上面的 override 已同时满足两个接口，这里不重复声明。
     */
    override val hookBridgeName: String = "Kiora Legacy ($frameworkName)"

    private val nextHookId = AtomicLong(1)
    private val hookedMemberSet: MutableSet<Member> = ConcurrentHashMap.newKeySet()

    override val hookCounter: Long get() = nextHookId.get() - 1

    override val hookedMethods: Set<Member> get() = hookedMemberSet

    /**
     * 单次 [XC_MethodHook] 承载 before + after，两次回调共享同一个 param 实例
     * （靠 [LegacyBridgeHookCallback] 内的 `setObjectExtra` 传递）。
     *
     * result / throwable 的「跳过原始实现」由 Xposed 原生处理，这里不需要额外状态。
     */
    override fun hookMethod(
        member: Member,
        callback: IHookBridge.IMemberHookCallback,
        priority: Int
    ): IHookBridge.MemberUnhookHandle {
        if (member !is Executable) {
            throw IllegalArgumentException("only method and constructor can be hooked, but got $member")
        }

        val xcCallback = LegacyBridgeHookCallback(callback, priority, nextHookId.getAndIncrement())
        val unhook = XposedBridge.hookMethod(member, xcCallback)
            ?: throw UnsupportedOperationException("XposedBridge.hookMethod returned null for $member")

        hookedMemberSet.add(member)
        return SimpleUnhookHandle(member, callback) { unhook.unhook() }
    }

    override val isDeoptimizationSupported: Boolean = false

    override fun deoptimize(executable: Executable): Boolean {
        return false
    }

    override fun invokeOriginalMethod(method: Method, thisObject: Any?, args: Array<Any?>): Any? {
        return XposedBridge.invokeOriginalMethod(method, thisObject, args)
    }

    override fun invokeOriginalConstructor(ctor: Constructor<*>, thisObject: Any, args: Array<Any?>) {
        // 以 <init> 作为方法在既有实例上重放，与 WeKit 的 Xp51HookImpl 同法
        XposedBridge.invokeOriginalMethod(ctor, thisObject, args)
    }

    override fun <T> newInstanceOrigin(constructor: Constructor<T>, vararg args: Any): T {
        throw UnsupportedOperationException("allocate instance is not supported")
    }
}