package dev.ujhhgtg.wekit.ui.content.m3

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

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
    BaseWidget(
        modifier = modifier,
        icon = icon,
        iconPlaceholder = iconPlaceholder,
        title = title,
        description = description,
        enabled = enabled,
        isError = isError,
        onClick = onClick ?: {
            if (enabled) {
                onCheckedChange(!checked)
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = if (onClick == null) null else onCheckedChange,
                enabled = enabled,
            )
        }
    )
}
