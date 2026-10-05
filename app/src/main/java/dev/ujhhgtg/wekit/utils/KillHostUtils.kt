package dev.ujhhgtg.wekit.utils

import kotlin.system.exitProcess

fun killHost() {
    WeLogger.i("KillHostUtils", "killing host")
    exitProcess(0)
}