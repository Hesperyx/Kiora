package dev.ujhhgtg.wekit.utils

import dev.ujhhgtg.reflekt.reflekt
import dev.ujhhgtg.reflekt.utils.toClass
import dev.ujhhgtg.wekit.R
import dev.ujhhgtg.wekit.i18n.LocaleResourceMode
import dev.ujhhgtg.wekit.i18n.LocalizedContextFactory
import dev.ujhhgtg.wekit.i18n.WeKitLocaleController
import dev.ujhhgtg.wekit.utils.android.showToast
import kotlin.system.exitProcess

fun restartHost() {
    WeLogger.i("KillHostUtils", "restarting host")
    val context = LocalizedContextFactory.create(
        HostInfo.application,
        WeKitLocaleController.resolvedLocale,
        LocaleResourceMode.InjectedHost,
    )
    showToast(context, context.getString(R.string.noncompose_restarting_host))
    val instance = runCatching {
        "com.tencent.mm.process.KillProcessHelperActivity".toClass()
            .reflekt().firstField().getStatic()
    }.getOrNull()
    if (instance == null) {
        WeLogger.e("KillHostUtils", "KillProcessHelperActivity instance not found; cannot restart host")
        return
    }
    runCatching { instance.reflekt().firstMethod().invoke(HostInfo.application, true) }
        .onFailure { WeLogger.e("KillHostUtils", "failed to invoke restart on KillProcessHelperActivity", it) }
}

fun killHost() {
    WeLogger.i("KillHostUtils", "killing host")
    exitProcess(0)
}
