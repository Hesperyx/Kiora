package dev.ujhhgtg.wekit.extensions

/**
 * 上游由 `:libs:common:annotation-scanner`（KSP）扫描所有 [ExtensionPack] 实现后自动生成，
 * Kiora 没有这个注解处理器，改为手写注册表。
 *
 * 上游的实现者是 `ArchLinuxPack` / `PythonRuntimePack` / `ScriptDepsPack` 三个，均尚未迁入
 * Kiora，因此这里暂时为空 —— `ExtensionPacks.packs` 的使用方
 * （`activity\settings\BackupCoordinator.kt:365`、`:373`、`data\LegacyDocumentMigration.kt:224`）
 * 会按「没有任何扩展包」处理，不会崩。后续迁入扩展包时在此登记。
 */
object ExtensionPacksProvider {

    val ALL_PACKS: List<ExtensionPack> = emptyList()
}
