@file:Suppress("NOTHING_TO_INLINE")

package dev.ujhhgtg.wekit.features.core

import android.content.Context
import androidx.annotation.StringRes
import dev.ujhhgtg.reflekt.reflected.BaseReflectedMethod
import dev.ujhhgtg.reflekt.reflected.ReflectedConstructor
import dev.ujhhgtg.wekit.dexkit.dsl.BaseDexDelegate
import dev.ujhhgtg.wekit.dexkit.dsl.DexConstructorDelegate
import dev.ujhhgtg.wekit.dexkit.dsl.DexMethodDelegate
import dev.ujhhgtg.wekit.utils.HookAction
import dev.ujhhgtg.wekit.utils.HookHandle
import dev.ujhhgtg.wekit.utils.HookParam
import dev.ujhhgtg.wekit.utils.TargetProcess
import dev.ujhhgtg.wekit.utils.TargetProcesses
import dev.ujhhgtg.wekit.utils.WeLogger
import dev.ujhhgtg.wekit.utils.hookAfterDirectly
import dev.ujhhgtg.wekit.utils.hookBeforeDirectly
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Executable

/**
 * WeKit 血统功能的根基类。
 *
 * 与 WeKit 原版的差异只有一处：原版还提供了
 * `BaseReflectedMethod` / `ReflectedConstructor`（reflekt 库类型）上的
 * `hookBefore` / `hookAfter` 重载，切片未迁 —— 那部分依赖 reflekt，
 * 需要时用 Kiora 的 `utils/reflect` 或直接传 [Executable]。
 */
abstract class BaseFeature {

    abstract val technicalId: String

    @get:StringRes
    abstract val nameRes: Int

    abstract val categoryIds: List<String>

    /** Processes where this feature may be enabled. Defaults to the main process only. */
    open val targetProcesses: Set<TargetProcess> = setOf(TargetProcess.MAIN)

    @get:StringRes
    open val descriptionRes: Int? = null

    val technicalPath: String
        get() = categoryIds.joinToString(",") + "/" + technicalId

    fun localizedName(context: Context): String = context.getString(nameRes)

    fun localizedDescription(context: Context): String =
        descriptionRes?.let(context::getString).orEmpty()

    open fun startup() {
        error("You shouldn't inherit BaseFeature")
    }

    /** Whether this feature's hooks are currently installed (runtime truth). */
    var isActive: Boolean = false
        private set

    fun enable() {
        if (TargetProcesses.currentType !in targetProcesses || isActive) return

        runCatching {
            isActive = true
            onEnable()
        }.onFailure { e ->
            WeLogger.e(TAG, "failed to enable feature $technicalPath", e)
            // ensure transaction is fully discarded
            unhookAll()
            isActive = false
        }
    }

    fun disable() {
        if (!isActive) return

        runCatching {
            isActive = false
            unhookAll()
            onDisable()
        }.onFailure { e ->
            WeLogger.e(TAG, "failed to disable feature $technicalPath", e)
            isActive = true
        }
    }

    open fun onEnable() {}

    open fun onDisable() {}

    private val _dexDelegates = mutableListOf<BaseDexDelegate>()

    val dexDelegates: List<BaseDexDelegate> get() = _dexDelegates

    fun registerDexDelegate(d: BaseDexDelegate) {
        d.owner = this
        _dexDelegates += d
    }

    fun resolveInlineDex(dexKit: DexKitBridge) {
        dexDelegates.forEach { it.findInline(dexKit) }
    }

    val unhooks = mutableListOf<HookHandle>()

    fun registerUnhook(u: HookHandle) {
        unhooks += u
    }

    fun unhookAll() {
        unhooks.forEach { it.unhook() }
        unhooks.clear()
    }

    // --- hookBefore ---

    fun Executable.hookBefore(
        priority: Int = 50,
        action: HookAction
    ) = registerUnhook(
        hookBeforeDirectly(priority) {
            executeHookAction(this, action)
        }
    )

    fun Class<*>.hookBeforeOnCreate(action: HookAction) =
        this.declaredMethods.first { it.name == "onCreate" }.hookBefore(50, action)

    fun Class<*>.hookAfterOnCreate(action: HookAction) =
        this.declaredMethods.first { it.name == "onCreate" }.hookAfter(50, action)

    // --- hookAfter ---

    fun Executable.hookAfter(
        priority: Int = 50,
        action: HookAction
    ) = registerUnhook(
        hookAfterDirectly(priority) {
            executeHookAction(this, action)
        }
    )

    // --- reflekt 反射类型上的 hook 重载（依赖 dev.ujhhgtg.reflekt，已随源码迁入）---

    @JvmName("hookBeforeReflectedMethod")
    fun BaseReflectedMethod.hookBefore(
        priority: Int = 50,
        action: HookAction
    ) = self.hookBefore(priority, action)

    @JvmName("hookBeforeReflectedConstructor")
    fun ReflectedConstructor<*>.hookBefore(
        priority: Int = 50,
        action: HookAction
    ) = self.hookBefore(priority, action)

    @JvmName("hookAfterReflectedMethod")
    fun BaseReflectedMethod.hookAfter(
        priority: Int = 50,
        action: HookAction
    ) = self.hookAfter(priority, action)

    @JvmName("hookAfterReflectedConstructor")
    fun ReflectedConstructor<*>.hookAfter(
        priority: Int = 50,
        action: HookAction
    ) = self.hookAfter(priority, action)

    // --- dex delegate ---

    fun DexMethodDelegate.hookBefore(
        priority: Int = 50,
        action: HookAction
    ) = method.hookBefore(priority, action)

    fun DexMethodDelegate.hookAfter(
        priority: Int = 50,
        action: HookAction
    ) = method.hookAfter(priority, action)

    fun DexConstructorDelegate.hookBefore(
        priority: Int = 50,
        action: HookAction
    ) = constructor.hookBefore(priority, action)

    fun DexConstructorDelegate.hookAfter(
        priority: Int = 50,
        action: HookAction
    ) = constructor.hookAfter(priority, action)

    fun executeHookAction(param: HookParam, action: HookAction) {
        runCatching {
            action(param)
        }.onFailure { e ->
            WeLogger.e("executeHookAction", "failed to execute hook of $technicalId", e)
        }
    }

    companion object {
        private const val TAG = "BaseFeature"
    }
}
