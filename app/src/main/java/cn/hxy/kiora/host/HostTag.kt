package cn.hxy.kiora.host

/**
 * 宿主标识与包名常量。
 *
 * 除 [IHostAdapter] 的实现类外，其余代码不应出现宿主包名字面量 ——
 * 接新宿主时只加一个实现类与这里一个常量，不改任何业务代码。
 */
object HostTag {

    const val QQ = "qq"
    const val TIM = "tim"
    const val WECHAT = "wechat"

    const val PACKAGE_QQ = "com.tencent.mobileqq"
    const val PACKAGE_TIM = "com.tencent.tim"
    const val PACKAGE_WECHAT = "com.tencent.mm"

    /** 全部已支持宿主的主包名。 */
    val PACKAGES = setOf(PACKAGE_QQ, PACKAGE_TIM, PACKAGE_WECHAT)

    /**
     * 历史 hook 的默认归属。
     *
     * [cn.hxy.kiora.annotation.HookItemAnnotation.hosts] 的默认值取这一组，
     * 使改造前已存在的 hook 无需任何改动，就保持「只在 QQ/TIM 生效」的原有语义。
     *
     * 注意：注解模块（`:annotation`）是独立 Gradle 模块，无法引用本常量，
     * 其默认值以字面量 `["qq", "tim"]` 写出。改这里时务必同步那边。
     */
    val LEGACY_DEFAULT = arrayOf(QQ, TIM)
}
