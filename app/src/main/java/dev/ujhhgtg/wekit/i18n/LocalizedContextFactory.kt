package dev.ujhhgtg.wekit.i18n

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import dev.ujhhgtg.wekit.loader.utils.ResourcesInjector

enum class LocaleResourceMode {
    InjectedHost,
    ModuleApp,
}

/**
 * 按 locale 创建本地化上下文。
 *
 * **简化说明**：原版最后还用 lsparanoid 的 `LspResourceContext` 做了一轮资源混淆解码。
 * Kiora 的 WeKit 血统资源直接放进模块自身的 `strings.xml`（明文、无需解码），
 * 故省略这一步；`ResourcesInjector` 的宿主注入保留（见下）。
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
        val localized = when (mode) {
            // 宿主模式下，本地化上下文被用来在微信自己的界面里取字符串；模块资源必须
            // 先注入该 Resources，否则按模块 id 取值会抛 Resources.NotFoundException。
            LocaleResourceMode.InjectedHost -> configured.also {
                ResourcesInjector.injectModuleRes(it.resources)
            }

            LocaleResourceMode.ModuleApp -> configured
        }
        return if (locale == SupportedLocale.MEOW_CHINESE) {
            MeowResourcesContext(localized)
        } else {
            localized
        }
    }
}
