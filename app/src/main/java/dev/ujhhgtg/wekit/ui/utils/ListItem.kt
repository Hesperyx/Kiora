@file:Suppress("NOTHING_TO_INLINE")

package dev.ujhhgtg.wekit.ui.utils

import androidx.compose.material3.ListItem as MaterialListItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
inline fun ListItem(
    modifier: Modifier = Modifier,
    noinline leadingContent: @Composable (() -> Unit)? = null,
    noinline trailingContent: @Composable (() -> Unit)? = null,
    noinline overlineContent: @Composable (() -> Unit)? = null,
    noinline supportingContent: @Composable (() -> Unit)? = null,
    noinline content: @Composable () -> Unit,
) {
    MaterialListItem(
        headlineContent = content,
        modifier = modifier,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
    )
}
