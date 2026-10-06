package dev.ujhhgtg.wekit.extensions

/**
 * 上游由 `:libs:common:annotation-scanner`（KSP）扫描所有 [ExtensionPack] 实现后自动生成，
 * Kiora 没有这个注解处理器，改为手写注册表。
 *
 * 上游的实现者是 `ArchLinuxPack` / `PythonRuntimePack` / `ScriptDepsPack` 三个；`ArchLinuxPack`
 * 随 agent 一起搁置（它要 proot 静态二进制 + Arch Linux rootfs），因此这里只登记其余两个。
 * 使用方 `activity\settings\BackupCoordinator.kt:365`、`:373`、`data\LegacyDocumentMigration.kt:224`
 * 以及 `ExtensionPacks.packs` 都只按内容遍历，不依赖具体数量。
 */
object ExtensionPacksProvider {

    val ALL_PACKS: List<ExtensionPack> = validateExtensionPacks(
        listOf(
            ScriptDepsPack,
            PythonRuntimePack,
        )
    )
}
