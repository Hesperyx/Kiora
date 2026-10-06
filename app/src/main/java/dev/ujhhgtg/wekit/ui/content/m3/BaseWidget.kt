package dev.ujhhgtg.wekit.ui.content.m3

import androidx.compose.foundation.LocalIndication
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
    val haptic = LocalHapticFeedback.current

    val interactionSource = remember { MutableInteractionSource() }
    val trailingInteractionSource = remember { MutableInteractionSource() }
    val trailingContentInteractionSource =
        if (onTrailingClick != null || trailingDivider) trailingInteractionSource else interactionSource

    val handleTrailingClick = onTrailingClick?.let { callback ->
        {
            clickHaptic?.let { haptic.performHapticFeedback(it) }
            callback()
        }
    }

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
                Modifier.clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = {
                        clickHaptic?.let { haptic.performHapticFeedback(it) }
                        onClick()
                    }
                )
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
            Box {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = contentColor,
                        style = titleStyle
                    )
                    headlineTrailingContent()
                }
                foreContent()
            }
            if (description != null) {
                Text(
                    text = description,
                    color = resolvedDescriptionColor,
                    style = descriptionStyle
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingDivider) VerticalDivider(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .then(
                        if (handleTrailingClick != null) {
                            Modifier.clickable(
                                enabled = enabled,
                                interactionSource = trailingInteractionSource,
                                indication = LocalIndication.current,
                                onClick = handleTrailingClick
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(start = if (trailingDivider) 16.dp else 0.dp),
                contentAlignment = Alignment.Center
            ) {
                trailingContent(trailingContentInteractionSource)
            }
        }
    }
}
