package cn.hxy.kiora.wx.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cn.hxy.kiora.wx.conf.WxMsgFormatConfig
import cn.hxy.kiora.wx.conf.WxAutoLoginConfig
import cn.hxy.kiora.ui.components.listitems.ActionItem
import cn.hxy.kiora.ui.components.listitems.InputItem
import cn.hxy.kiora.ui.components.listitems.PreferenceSection
import cn.hxy.kiora.ui.components.listitems.SelectionGroup
import cn.hxy.kiora.ui.components.listitems.SelectionItem
import cn.hxy.kiora.ui.components.listitems.SwitchItem
import cn.hxy.kiora.ui.components.scaffold.ConfigPageScaffold

/**
 * 微信侧各项功能的配置页。
 *
 * 全部走 [ConfigPageScaffold]，与 QQ 侧同一套组件 —— 用户看到的设置界面是同一套
 * 视觉与交互，只有条目内容不同。放在 `configs/wx` 子包而不是与 QQ 侧混在一起，
 * 是为了「一眼能看出这条只作用于微信」。
 */

@Composable
fun WxMsgFormatPage(
    currentConfig: WxMsgFormatConfig,
    onSave: (WxMsgFormatConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var temp by remember(currentConfig) { mutableStateOf(currentConfig) }

    ConfigPageScaffold(
        title = "发送文本格式",
        configData = temp,
        onSave = onSave,
        onDismiss = onDismiss
    ) {
        PreferenceSection(title = "格式") {
            InputItem(
                title = "文本格式",
                value = temp.textFormat,
                onValueChange = { temp = temp.copy(textFormat = it) },
                placeholder = "\${sendText}喵~"
            )
            InputItem(
                title = "时间格式",
                value = temp.timeFormat,
                onValueChange = { temp = temp.copy(timeFormat = it) },
                placeholder = "HH:mm:ss"
            )
        }

        PreferenceSection(title = "可用占位符") {
            ActionItem(
                title = "\${sendText}",
                description = "原本要发送的文本",
                onClick = { temp = temp.copy(textFormat = temp.textFormat + "\${sendText}") }
            )
            ActionItem(
                title = "\${line}",
                description = "换行",
                onClick = { temp = temp.copy(textFormat = temp.textFormat + "\${line}") }
            )
            ActionItem(
                title = "\${sendTime}",
                description = "发送时刻，按上面的时间格式渲染",
                onClick = { temp = temp.copy(textFormat = temp.textFormat + "\${sendTime}") }
            )
        }
    }
}

@Composable
fun WxAutoLoginPage(
    currentConfig: WxAutoLoginConfig,
    onSave: (WxAutoLoginConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var temp by remember(currentConfig) { mutableStateOf(currentConfig) }

    ConfigPageScaffold(
        title = "自动点击登录",
        configData = temp,
        onSave = onSave,
        onDismiss = onDismiss
    ) {
        PreferenceSection(title = "登录页勾选项") {
            SwitchItem(
                title = "同步最近消息",
                checked = temp.autoSyncMsg,
                onCheckedChange = { temp = temp.copy(autoSyncMsg = it) }
            )
            SwitchItem(
                title = "显示登录设备",
                checked = temp.showLoginDevice,
                onCheckedChange = { temp = temp.copy(showLoginDevice = it) }
            )
            SwitchItem(
                title = "自动登录设备",
                checked = temp.autoLoginDevice,
                onCheckedChange = { temp = temp.copy(autoLoginDevice = it) },
                description = "勾选后进入登录页会自动点下登录按钮"
            )
        }
    }
}
