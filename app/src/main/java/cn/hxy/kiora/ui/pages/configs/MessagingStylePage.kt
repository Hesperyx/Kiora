package cn.hxy.kiora.ui.pages.configs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cn.hxy.kiora.conf.MessagingStyleConfig
import cn.hxy.kiora.ui.components.listitems.SwitchItem
import cn.hxy.kiora.ui.components.scaffold.ConfigPageScaffold

@Composable
fun MessagingStylePage(
    currentConfig: MessagingStyleConfig,
    onSave: (MessagingStyleConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var disableSubChannel by remember(currentConfig) { mutableStateOf(currentConfig.disableSubChannel) }
    var disableBubble by remember(currentConfig) { mutableStateOf(currentConfig.disableBubble) }
    var autoClearSubChannel by remember(currentConfig) { mutableStateOf(currentConfig.autoClearSubChannel) }

    fun buildConfig(): MessagingStyleConfig = MessagingStyleConfig(
        disableSubChannel = disableSubChannel,
        disableBubble = disableBubble,
        autoClearSubChannel = autoClearSubChannel
    )

    ConfigPageScaffold(
        title = "MessagingStyle 通知",
        configData = buildConfig(),
        onSave = onSave,
        onDismiss = onDismiss
    ) { _ ->
        SwitchItem(
            title = "禁用子渠道发送通知",
            description = "所有会话共用 4 个基础通知渠道，不再为每个会话创建子渠道",
            checked = disableSubChannel || autoClearSubChannel,
            onCheckedChange = {
                disableSubChannel = it
                if (!it) autoClearSubChannel = false
            }
        )
        SwitchItem(
            title = "禁用通知气泡",
            description = "通知不再以气泡形式浮动展示",
            checked = disableBubble,
            onCheckedChange = { disableBubble = it }
        )
        SwitchItem(
            title = "自动清除多余通知子渠道",
            description = "发送通知后自动删除历史会话产生的子渠道（隐含禁用子渠道）",
            checked = autoClearSubChannel,
            onCheckedChange = {
                autoClearSubChannel = it
                if (it) disableSubChannel = true
            }
        )
    }
}
