package dev.ujhhgtg.wekit.ui.content.m3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

@Composable
fun BaseItemContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val baseShape = LocalSegmentedItemShape.current
    val backgroundColor = MaterialTheme.colorScheme.surfaceBright

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(baseShape)
            .background(backgroundColor),
    ) {
        content()
    }
}
