@file:Suppress("NOTHING_TO_INLINE")

package dev.ujhhgtg.reflekt.utils

import java.lang.reflect.AccessibleObject

fun <T : AccessibleObject> T.makeAccessible(): T {
    @Suppress("DEPRECATION")
    if (!isAccessible) {
        runCatching { isAccessible = true }
    }
    return this
}
