package dev.ujhhgtg.wekit.loader.startup

import cn.hxy.kiora.common.ModuleLoader

/**
 * 上游版本这里持有 `loader.abc.ILoaderService` 与 `IHookBridge`（loader 服务抽象层）。
 * Kiora 没有这一层，模块 APK 路径直接从 [ModuleLoader] 取。
 *
 * 唯一使用点是 `loader\utils\ResourcesInjector.kt` 的 `File(StartupInfo.modulePath)`。
 */
object StartupInfo {

    val modulePath: String
        get() = ModuleLoader.getMODULE_PATH()
}
