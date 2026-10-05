package dev.ujhhgtg.wekit.ui.content.m3

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalSegmentedItemShape = compositionLocalOf<Shape> { RoundedCornerShape(CornerRadius) }

@DslMarker
annotation class SegmentedColumnDsl

class SegmentedColumnScope {
    private val items = mutableListOf<@Composable (Shape) -> Unit>()

    fun item(
        key: Any? = null,
        animatedVisibility: Boolean = true,
        topPadding: Dp? = null,
        forceFlatTop: Boolean = false,
        forceFlatBottom: Boolean = false,
        content: @Composable (Shape) -> Unit
    ) {
        items.add(content)
    }

    internal fun contentList(): List<@Composable (Shape) -> Unit> = items
}

@Composable
fun SegmentedColumn(
    modifier: Modifier = Modifier,
    title: String? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    titlePadding: PaddingValues = PaddingValues(start = 16.dp, top = 8.dp, bottom = 8.dp),
    content: SegmentedColumnScope.() -> Unit
) {
    val scope = SegmentedColumnScope().apply(content)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding)
    ) {
        title?.let {
            Text(
                text = it,
                modifier = Modifier.padding(titlePadding),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
            )
        }

        scope.contentList().forEachIndexed { index, itemContent ->
            val shape = RoundedCornerShape(CornerRadius)
            CompositionLocalProvider(LocalSegmentedItemShape provides shape) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (index == 0) 0.dp else 8.dp)
                ) {
                    itemContent(shape)
                }
            }
        }
    }
}
