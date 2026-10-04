package cn.hxy.kiora.qq.host

import cn.hxy.kiora.host.HostTag
import cn.hxy.kiora.common.ModuleMeta

/**
 * QQ 宿主适配器。
 *
 * 适配版本取自 [ModuleMeta]，与 README 的「适配与运行环境」一节同源，
 * 避免两处各写一份导致漂移。
 */
object QQHostAdapter : QQFamilyHostAdapter(
    tag = HostTag.QQ,
    packageName = HostTag.PACKAGE_QQ,
    displayName = "QQ",
    scopePackages = setOf(HostTag.PACKAGE_QQ),
    adaptedVersions = ModuleMeta.ADAPTED_QQ_VERSION
)
