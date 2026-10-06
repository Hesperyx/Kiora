package dev.ujhhgtg.wekit.dexkit.resolution

import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
import dev.ujhhgtg.wekit.dexkit.dsl.BaseDexDelegate
import dev.ujhhgtg.wekit.features.core.BaseFeature
import org.luckypray.dexkit.DexKitBridge

/**
 * Dex 解析上下文。
 *
 * **切片版**：只保留最小闭环 —— 持有一个 [DexKitBridge]，逐个 feature 跑内联查找与
 * `resolveDex`。WeKit 原版的「递归依赖检测（`ensureResolved` 的会话栈）+ blocked-by
 * 归因 + `ResolutionCoordinator` 批量协调 + `DexResolutionBatch`」都没有迁。
 *
 * 这意味着两点差异，后续扩展时要注意：
 * 1. feature 之间若存在「A 的 matcher 依赖 B 的查找结果」这种跨 feature 依赖，
 *    当前实现不会自动解依赖，需要调用方保证解析顺序。
 * 2. 委托在**非解析期**被访问（例如读 `.data`）会直接报错，而不是惰性补解析。
 */
object DexResolutionContext {

    private val current = ThreadLocal<DexKitBridge?>()

    /** 本线程正在解析中的 owner，用于 [ensureResolved] 的重入判定。 */
    private val activeOwners = ThreadLocal.withInitial { mutableSetOf<IResolveDex>() }

    val dexKit: DexKitBridge
        get() = current.get() ?: error("Dex resolution context is not active")

    fun <T> withResolutionContext(dexKit: DexKitBridge, block: () -> T): T {
        val previous = current.get()
        if (previous === dexKit) return block()
        current.set(dexKit)
        try {
            return block()
        } finally {
            current.set(previous)
        }
    }

    fun resolve(item: IResolveDex) {
        val dexKit = current.get() ?: error("Dex resolution context is not active")
        val feature = item as BaseFeature

        val active = activeOwners.get()
        active += item
        try {
            item.dexDelegates.forEach(BaseDexDelegate::resetForResolution)
            feature.resolveInlineDex(dexKit)
            item.resolveDex(dexKit)
            item.dexDelegates.forEach(BaseDexDelegate::markIncomplete)

            check(item.dexDelegates.all {
                it.diagnostic.status == DexResolutionStatus.SUCCESS ||
                    it.diagnostic.status == DexResolutionStatus.EXPECTED_FAILURE
            }) { "Incomplete or failed Dex resolution: ${feature.technicalPath}" }
        } finally {
            active -= item
        }
    }

    /**
     * 确保某个委托已被解析（对上游 `dexkit\resolution\DexResolutionContext.kt:39` 的切片实现）。
     *
     * 上游用会话栈 + `ResolutionCoordinator.isOwnedByCurrentThread` 判定「是否正在由本线程
     * 解析」，以便把跨 feature 依赖递归解析并把环归因清楚。Kiora 没有协调器，改用
     * [activeOwners]（本线程正在解析中的 owner 集合）做同样的重入判定 —— 已在解析中的
     * owner 只校验描述符，其余情况就地补一次解析。
     */
    fun ensureResolved(delegate: BaseDexDelegate) {
        if (delegate.getDescriptorString() != null) return
        val owner = delegate.owner as? IResolveDex
            ?: error("Dex delegate has no resolvable owner: ${delegate.key}")
        val path = (owner as? BaseFeature)?.technicalPath ?: owner.toString()

        if (owner in activeOwners.get()) {
            error("Unresolved recursive Dex dependency: $path#${delegate.key}")
        }

        resolve(owner)

        check(delegate.getDescriptorString() != null) {
            "Incomplete or failed Dex dependency: $path#${delegate.key}"
        }
    }
}

fun IResolveDex.resolveAllDex(dexKit: DexKitBridge) =
    DexResolutionContext.withResolutionContext(dexKit) {
        DexResolutionContext.resolve(this)
    }
