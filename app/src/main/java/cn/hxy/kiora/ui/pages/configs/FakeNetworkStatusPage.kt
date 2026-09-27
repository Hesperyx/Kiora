package cn.hxy.kiora.ui.pages.configs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cn.hxy.kiora.conf.FakeNetworkConfig
import cn.hxy.kiora.ui.components.listitems.SelectionItem
import cn.hxy.kiora.ui.components.listitems.SelectionGroup
import cn.hxy.kiora.ui.components.scaffold.ConfigPageScaffold

@Composable
fun FakeNetworkStatusPage(
    currentConfig: FakeNetworkConfig,
    onSave: (FakeNetworkConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember(currentConfig) { mutableStateOf(currentConfig.mode) }

    fun buildConfig(): FakeNetworkConfig = FakeNetworkConfig(mode = mode)

    ConfigPageScaffold(
        title = "伪装网络状态",
        configData = buildConfig(),
        onSave = onSave,
        onDismiss = onDismiss
    ) { _ ->
        SelectionGroup {
            SelectionItem(
                title = "WIFI",
                subtitle = "伪装为 WIFI 网络",
                isSelected = mode == FakeNetworkConfig.MODE_WIFI,
                onClick = { mode = FakeNetworkConfig.MODE_WIFI }
            )
            SelectionItem(
                title = "5G",
                subtitle = "伪装为 5G 蜂窝网络",
                isSelected = mode == FakeNetworkConfig.MODE_5G,
                onClick = { mode = FakeNetworkConfig.MODE_5G }
            )
            SelectionItem(
                title = "4G",
                subtitle = "伪装为 4G 蜂窝网络",
                isSelected = mode == FakeNetworkConfig.MODE_4G,
                onClick = { mode = FakeNetworkConfig.MODE_4G }
            )
        }
    }
}
