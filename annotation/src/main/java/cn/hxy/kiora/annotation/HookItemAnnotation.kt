package cn.hxy.kiora.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HookItemAnnotation(
    val tag: String,
    val desc: String = "",
    val category: String = HookCategory.OTHER,
    val process: String = "",
    /**
     * 该 hook 生效的宿主，取值见 app 模块的 `cn.hxy.kiora.host.HostTag`。
     *
     * 默认值**刻意**取 `["qq", "tim"]`：改造前已存在的 hook 全部只作用于
     * QQ/TIM，用这个默认值可以让它们一行都不用改就保持原有语义。
     * 微信 hook 必须显式写 `hosts = ["wechat"]`；跨宿主通用功能写三个。
     * 空数组视为不限宿主。
     *
     * 注意：注解模块是独立 Gradle 模块，无法引用 `HostTag.LEGACY_DEFAULT`，
     * 故此处以字面量写出。改那边时务必同步这里。
     */
    val hosts: Array<String> = ["qq", "tim"]
)