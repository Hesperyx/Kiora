package dev.ujhhgtg.wekit.utils

import cn.hxy.kiora.loader.hookapi.HookEngineManager
import cn.hxy.kiora.loader.hookapi.IHookBridge
import java.lang.reflect.Executable
import java.lang.reflect.Method

typealias HookParam = IHookBridge.IMemberHookParam

typealias HookHandle = IHookBridge.MemberUnhookHandle

typealias HookAction = HookParam.() -> Unit

/**
 * WeKit 血统功能的挂钩入口。
 *
 * 与 WeKit 原版逐字对齐，唯一的差别是 hook 原语的来源：原版从 `StartupInfo.hookBridge`
 * 取，这里从 Kiora 的 [HookEngineManager] 取当前引擎并要求它实现 [IHookBridge]。
 *
 * 原版还有 `BaseReflectedMethod` / `ReflectedConstructor` 上的重载（依赖 reflekt 库），
 * 切片未迁 —— 需要时用 Kiora 的 `utils/reflect` 或直接传 `Executable`。
 */
val currentHookBridge: IHookBridge
    get() = HookEngineManager.engine as? IHookBridge
        ?: error("当前引擎未实现 IHookBridge：${HookEngineManager.engine.javaClass.name}")

abstract class HookCallback(val priority: Int = 50) : IHookBridge.IMemberHookCallback {

    protected open fun beforeHookedMethod(param: HookParam) {}

    protected open fun afterHookedMethod(param: HookParam) {}

    final override fun beforeHookedMember(param: HookParam) = beforeHookedMethod(param)

    final override fun afterHookedMember(param: HookParam) = afterHookedMethod(param)
}

/**
 * 捕获「原始方法」调用入口，供 hook 内部调用未被改写前的实现。
 */
class OriginalMethodInvoker(
    private val hookBridge: IHookBridge,
    private val method: Method,
    private val thisObject: Any?,
    private val originalArgs: Array<Any?>
) {
    operator fun invoke(args: Array<Any?>? = null): Any? =
        hookBridge.invokeOriginalMethod(method, thisObject, args ?: originalArgs)
}

fun Executable.hookBeforeDirectly(
    priority: Int = 50,
    action: HookAction
): HookHandle = currentHookBridge.hookMethod(
    this, object : HookCallback(priority) {
        override fun beforeHookedMethod(param: HookParam) {
            action(param)
        }
    }, priority
)

fun Executable.hookAfterDirectly(
    priority: Int = 50,
    action: HookAction
): HookHandle = currentHookBridge.hookMethod(
    this, object : HookCallback(priority) {
        override fun afterHookedMethod(param: HookParam) {
            action(param)
        }
    }, priority
)

fun Executable.hookDirectly(hook: HookCallback): HookHandle =
    currentHookBridge.hookMethod(this, hook, hook.priority)

fun HookParam.captureOriginalMethod(): OriginalMethodInvoker {
    val method = member as? Method
        ?: throw IllegalStateException("invokeOriginalMethod is only supported for methods: $member")
    return OriginalMethodInvoker(currentHookBridge, method, thisObject, args.copyOf())
}

fun HookParam.invokeOriginalMethod(thisObject: Any? = null, args: Array<Any?>? = null): Any? {
    val method = member as? Method
        ?: throw IllegalStateException("invokeOriginalMethod is only supported for methods: $member")
    return currentHookBridge.invokeOriginalMethod(
        method,
        thisObject ?: this.thisObject,
        args ?: this.args
    )
}

// --- reflekt 反射类型上的 hook 重载（原版有，切片此前未迁）---
// WeKit 血统功能用 reflekt 的 `firstMethod{...}.hookBeforeDirectly{}` 这类写法，
// 这里补上转发到 Executable 版本的重载，避免功能代码改动。

fun dev.ujhhgtg.reflekt.reflected.BaseReflectedMethod.hookBeforeDirectly(
    priority: Int = 50,
    action: HookAction
): HookHandle = self.hookBeforeDirectly(priority, action)

fun dev.ujhhgtg.reflekt.reflected.BaseReflectedMethod.hookAfterDirectly(
    priority: Int = 50,
    action: HookAction
): HookHandle = self.hookAfterDirectly(priority, action)

fun dev.ujhhgtg.reflekt.reflected.ReflectedConstructor<*>.hookAfterDirectly(
    priority: Int = 50,
    action: HookAction
): HookHandle = self.hookAfterDirectly(priority, action)

fun dev.ujhhgtg.reflekt.reflected.BaseReflectedMethod.hookDirectly(
    hook: HookCallback
): HookHandle = self.hookDirectly(hook)
