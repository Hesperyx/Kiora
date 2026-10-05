// SPDX-License-Identifier: GPL-3.0-only
package dev.ujhhgtg.wekit.ui.content.m3

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Gap between two adjacent items of a segmented list. */
private val SegmentedGap = 8.dp

/** Spatial spring used for the segment corner-radius transitions. */
private val SegmentRadiusSpec = spring<Dp>(
    dampingRatio = 0.8f,
    stiffness = 1200f,
)

fun <T> LazyListScope.lazySegmentedItems(
    items: List<T>,
    key: (T) -> Any,
    itemContent: @Composable (T) -> Unit,
) {
    itemsIndexed(
        items = items,
        key = { _, item -> key(item) },
    ) { index, item ->
        val targetTop = if (index == 0) CornerRadius else ConnectionRadius
        val targetBottom = if (index == items.lastIndex) CornerRadius else ConnectionRadius
        val top by animateDpAsState(
            targetValue = targetTop,
            animationSpec = SegmentRadiusSpec,
            label = "segmentedTopRadius",
        )
        val bottom by animateDpAsState(
            targetValue = targetBottom,
            animationSpec = SegmentRadiusSpec,
            label = "segmentedBottomRadius",
        )
        val shape = RoundedCornerShape(
            topStart = top,
            topEnd = top,
            bottomStart = bottom,
            bottomEnd = bottom,
        )

        Box(
            Modifier
                .animateItem()
                .padding(top = if (index == 0) 0.dp else SegmentedGap),
        ) {
            CompositionLocalProvider(LocalSegmentedItemShape provides shape) {
                itemContent(item)
            }
        }
    }
}
