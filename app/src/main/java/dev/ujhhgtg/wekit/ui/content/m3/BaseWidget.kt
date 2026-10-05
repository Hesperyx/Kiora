package dev.ujhhgtg.wekit.ui.content.m3

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun BaseWidget(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color? = null,
    iconPlaceholder: Boolean = false,
    title: String,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
    description: String? = null,
    descriptionStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    descriptionColor: Color? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onTrailingClick: (() -> Unit)? = null,
    clickHaptic: HapticFeedbackType? = HapticFeedbackType.VirtualKey,
    trailingDivider: Boolean = false,
    headlineTrailingContent: @Composable RowScope.() -> Unit = {},
    foreContent: @Composable BoxScope.() -> Unit = {},
    trailingContent: @Composable BoxScope.(MutableInteractionSource) -> Unit = {},
) {
    val shape = LocalSegmentedItemShape.current
    val background = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceBright
    }
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val resolvedDescriptionColor = descriptionColor
        ?: if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

    val rowModifier = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(background)
        .then(
            if (onClick != null) {
                Modifier.clickable(enabled = enabled, onClick = onClick)
            } else {
                Modifier
            }
        )

    Row(
        modifier = rowModifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor ?: if (enabled) MaterialTheme.colorScheme.primary else contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        } else if (iconPlaceholder) {
            Spacer(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = contentColor,
                    style = titleStyle
                )
                headlineTrailingContent()
            }
            if (description != null) {
                Text(
                    text = description,
                    color = resolvedDescriptionColor,
                    style = descriptionStyle
                )
            }
        }

        Box {
            foreContent()
            trailingContent(MutableInteractionSource())
        }
    }
}
