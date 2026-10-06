package dev.ujhhgtg.wekit.ui.content.m3

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Check
import com.composables.icons.materialsymbols.outlined.Close

@Composable
fun SwitchWidget(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconPlaceholder: Boolean = false,
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailingDivider: Boolean = false,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val handleCheckedChange: (Boolean) -> Unit = { newValue ->
        if (newValue) {
            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
        }
        onCheckedChange(newValue)
    }

    val leftClickAction: () -> Unit = if (onClick == null) {
        {
            if (enabled) {
                handleCheckedChange(!checked)
            }
        }
    } else {
        {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        }
    }

    val separateClickAreas = onClick != null || trailingDivider

    BaseWidget(
        modifier = modifier.semantics(mergeDescendants = true) {
            role = Role.Switch
            toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
        },
        icon = icon,
        iconPlaceholder = iconPlaceholder,
        title = title,
        description = description,
        enabled = enabled,
        isError = isError,
        onClick = leftClickAction,
        clickHaptic = null,
        trailingDivider = trailingDivider,
    ) { interactionSource ->
        Switch(
            modifier = Modifier.clearAndSetSemantics {},
            checked = checked,
            enabled = enabled,
            interactionSource = interactionSource,
            colors = SwitchDefaults.colors(
                checkedIconColor = MaterialTheme.colorScheme.primary,
                uncheckedIconColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            thumbContent = {
                Icon(
                    imageVector = if (checked) {
                        MaterialSymbols.Outlined.Check
                    } else {
                        MaterialSymbols.Outlined.Close
                    },
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            },
            onCheckedChange = if (separateClickAreas) handleCheckedChange else null,
        )
    }
}
