package dev.ujhhgtg.wekit.ui.content.m3

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun IntNumberPickerWidget(
    title: String,
    value: Int,
    startInt: Int,
    endInt: Int,
    stepSize: Int = 1,
    valueSuffix: String = "",
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** 值处于「未设置」等次要状态时置灰显示（WeKit 原版 `:58`）。 */
    subduedValue: Boolean = false,
    /** 非空时值文本可点击，用于「点击切换是否设置」（WeKit 原版 `:59`）。 */
    onValueClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = value.toFloat().coerceIn(startInt.toFloat(), endInt.toFloat()),
                onValueChange = { raw ->
                    val stepped = if (stepSize <= 0) {
                        raw.roundToInt()
                    } else {
                        ((raw / stepSize).roundToInt() * stepSize)
                    }.coerceIn(startInt, endInt)
                    onValueChange(stepped)
                },
                valueRange = startInt.toFloat()..endInt.toFloat(),
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$value$valueSuffix",
                color = if (subduedValue) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = if (onValueClick != null) {
                    Modifier.clickable(onClick = onValueClick)
                } else {
                    Modifier
                }
            )
        }
    }
}
