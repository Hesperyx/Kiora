// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package dev.ujhhgtg.wekit.ui.content.m3

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

data class DropdownOption<T>(val value: T, val label: String)

/**
 * Expressive single-choice popup. Material3 1.4.0 (the version pinned by the BOM this slice
 * builds against) has no `DropdownMenuPopup` / `DropdownMenuGroup` / `SelectableDropdownMenuItem`,
 * so the grouping shapes are folded into the popup shape and the selection mark is rendered as
 * the item's leading icon.
 */
@Composable
fun <T> ExpressiveOptionDropdown(
    expanded: Boolean,
    value: T,
    options: List<DropdownOption<T>>,
    onDismissRequest: () -> Unit,
    onValueChange: (T) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        options.forEach { option ->
            val selected = option.value == value
            DropdownMenuItem(
                text = {
                    Text(
                        text = option.label,
                        fontWeight = if (selected) FontWeight.SemiBold else null,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                },
                onClick = { onValueChange(option.value) },
            )
        }
    }
}

@Composable
fun <T> DropDownMenuWidget(
    icon: ImageVector? = null,
    iconPlaceholder: Boolean = false,
    title: String,
    description: String?,
    value: T,
    options: List<DropdownOption<T>>,
    enabled: Boolean = true,
    onValueChange: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var pressPosition by remember { mutableStateOf(Offset.Zero) }
    val selected = options.first { it.value == value }

    // The popup anchors to its parent layout, so place a zero-size anchor
    // at the press position instead of a fixed spot inside the row.
    Box(
        modifier = Modifier.pointerInput(enabled) {
            awaitEachGesture {
                pressPosition = awaitFirstDown(requireUnconsumed = false).position
            }
        }
    ) {
        BaseWidget(
            icon = icon,
            iconPlaceholder = iconPlaceholder,
            title = title,
            description = description ?: selected.label,
            enabled = enabled,
            onClick = if (enabled) ({ expanded = !expanded }) else null,
        )
        Box(
            Modifier.offset {
                IntOffset(pressPosition.x.roundToInt(), pressPosition.y.roundToInt())
            }
        ) {
            ExpressiveOptionDropdown(
                expanded = expanded,
                value = value,
                options = options,
                onDismissRequest = { expanded = false },
                onValueChange = {
                    onValueChange(it)
                    expanded = false
                },
            )
        }
    }
}
