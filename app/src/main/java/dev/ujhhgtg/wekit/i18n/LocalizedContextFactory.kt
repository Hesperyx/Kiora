package dev.ujhhgtg.wekit.i18n

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList

enum class LocaleResourceMode {
    InjectedHost,
    ModuleApp,
}

/**
 * 按 locale 创建本地化上下文。
 *
 * **简化说明**：原版还做了两件事 —— 用 `ResourcesInjector` 把模块资源注入宿主，
 * 再用 lsparanoid 的 `LspResourceContext` 做资源混淆解码。Kiora 的 WeKit 血统资源
 * 直接放进模块自身的 `strings.xml`（明文、无需解码），也不走宿主注入，
 * 所以这两步都省略了。`LocaleResourceMode` 两个取值因此在当前实现里等价。
 */
object LocalizedContextFactory {
    fun create(
        base: Context,
        locale: SupportedLocale,
        mode: LocaleResourceMode,
    ): Context {
        val configuration = Configuration(base.resources.configuration).apply {
            setLocales(LocaleList.forLanguageTags(locale.androidTag))
        }
        val configured = base.createConfigurationContext(configuration)
        return if (locale == SupportedLocale.MEOW_CHINESE) {
            MeowResourcesContext(configured)
        } else {
            configured
        }
    }
}
