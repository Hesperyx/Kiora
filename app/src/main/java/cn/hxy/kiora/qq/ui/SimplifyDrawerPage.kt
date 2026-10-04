package cn.hxy.kiora.qq.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cn.hxy.kiora.qq.conf.SimplifyDrawerConfig
import cn.hxy.kiora.ui.components.listitems.SelectionGroup
import cn.hxy.kiora.ui.components.listitems.SelectionItem
import cn.hxy.kiora.ui.components.scaffold.ConfigPageScaffold

@Composable
fun SimplifyDrawerPage(
    currentConfig: SimplifyDrawerConfig,
    onSave: (SimplifyDrawerConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var hiddenItems by remember(currentConfig) { mutableStateOf(currentConfig.hiddenItems) }

    val items = listOf(
        "d_album" to "相册",
        "d_favorite" to "收藏",
        "d_document" to "文件",
        "d_qqwallet" to "钱包",
        "d_vip_identity" to "会员中心",
        "d_decoration" to "个性装扮",
        "d_vip_card" to "免流量"
    )

    fun buildConfig(): SimplifyDrawerConfig = SimplifyDrawerConfig(hiddenItems = hiddenItems)

    ConfigPageScaffold(
        title = "侧滑栏精简",
        configData = buildConfig(),
        onSave = onSave,
        onDismiss = onDismiss
    ) { _ ->
        SelectionGroup {
            items.forEach { (id, label) ->
                SelectionItem(
                    title = label,
                    subtitle = if (id in hiddenItems) "点击恢复显示" else "点击隐藏该项",
                    isSelected = id in hiddenItems,
                    onClick = {
                        hiddenItems = if (id in hiddenItems) hiddenItems - id else hiddenItems + id
                    }
                )
            }
        }
    }
}
