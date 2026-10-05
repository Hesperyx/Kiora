package dev.ujhhgtg.wekit.features.items.payment

import android.content.Context
import androidx.annotation.StringRes
import dev.ujhhgtg.wekit.utils.HostInfo

fun localizedPaymentString(@StringRes id: Int, vararg formatArgs: Any): String =
    HostInfo.application.getString(id, *formatArgs)

fun Context.localizedPaymentString(@StringRes id: Int, vararg formatArgs: Any): String =
    getString(id, *formatArgs)
