package cn.hxy.kiora.host

import cn.hxy.kiora.common.ModuleMeta

/**
 * TIM 宿主适配器。
 *
 * TIM 与 QQ 同属 QQ 系架构，锚点与账号获取方式一致；
 * 但 TIM 对旧版架构仅部分兼容，功能差异由各 hook 内的 `HostInfo.isTIM` 判定，
 * 不在这里收敛。
 */
object TIMHostAdapter : QQFamilyHostAdapter(
    tag = HostTag.TIM,
    packageName = HostTag.PACKAGE_TIM,
    displayName = "TIM",
    scopePackages = setOf(HostTag.PACKAGE_TIM),
    adaptedVersions = ModuleMeta.ADAPTED_TIM_VERSION
)
