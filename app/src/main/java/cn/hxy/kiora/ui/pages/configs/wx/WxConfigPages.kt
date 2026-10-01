package cn.hxy.kiora.ui.pages.configs.wx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import cn.hxy.kiora.conf.WxAutoLoginConfig
import cn.hxy.kiora.conf.WxLocationConfig
import cn.hxy.kiora.conf.WxMsgFormatConfig
import cn.hxy.kiora.conf.WxRingtoneConfig
import cn.hxy.kiora.conf.WxSportStepConfig
import cn.hxy.kiora.conf.WxVoiceLengthConfig
import cn.hxy.kiora.hook.wx.WxLocation
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
fun WxVoiceLengthPage(
    currentConfig: WxVoiceLengthConfig,
    onSave: (WxVoiceLengthConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var temp by remember(currentConfig) { mutableStateOf(currentConfig) }

    ConfigPageScaffold(
        title = "语音时长",
        configData = temp,
        onSave = onSave,
        onDismiss = onDismiss
    ) {
        PreferenceSection(title = "显示时长") {
            InputItem(
                title = "秒数",
                value = temp.seconds.toString(),
                onValueChange = { temp = temp.copy(seconds = it.toIntOrNull() ?: temp.seconds) },
                placeholder = "1",
                keyboardType = KeyboardType.Number
            )
        }
    }
}

@Composable
fun WxSportStepPage(
    currentConfig: WxSportStepConfig,
    onSave: (WxSportStepConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var temp by remember(currentConfig) { mutableStateOf(currentConfig) }

    ConfigPageScaffold(
        title = "运动步数",
        configData = temp,
        onSave = onSave,
        onDismiss = onDismiss
    ) {
        PreferenceSection(title = "步数") {
            InputItem(
                title = "展示步数",
                value = temp.step.toString(),
                onValueChange = { temp = temp.copy(step = it.toLongOrNull() ?: temp.step) },
                placeholder = "88888",
                keyboardType = KeyboardType.Number
            )
            ActionItem(
                title = "上限 98800",
                description = "超出微信自身上限会被压回去，这里也会先取小",
                onClick = { temp = temp.copy(step = 98800L) }
            )
        }
    }
}

@Composable
fun WxRingtonePage(
    currentConfig: WxRingtoneConfig,
    onSave: (WxRingtoneConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var temp by remember(currentConfig) { mutableStateOf(currentConfig) }

    ConfigPageScaffold(
        title = "屏蔽通话铃声",
        configData = temp,
        onSave = onSave,
        onDismiss = onDismiss
    ) {
        PreferenceSection(title = "场景") {
            SwitchItem(
                title = "呼出",
                checked = temp.blockOutCall,
                onCheckedChange = { temp = temp.copy(blockOutCall = it) },
                description = "自己发起语音/视频通话时不播放铃声"
            )
            SwitchItem(
                title = "呼入",
                checked = temp.blockInCall,
                onCheckedChange = { temp = temp.copy(blockInCall = it) },
                description = "别人打进来时不播放铃声"
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

/**
 * 虚拟定位配置页。
 *
 * 「从地图选点」走微信自己的选点页 `RedirectUI`，结果由 [WxLocation] 挂的
 * `onActivityResult` 钩子解析后写进 [WxLocation.pickedLocation] ——
 * 那是一个 Compose 状态，所以这里读到变化就会自动把输入框刷新掉，
 * 不需要页面自己去接 `onActivityResult`。
 */
@Composable
fun WxLocationPage(
    currentConfig: WxLocationConfig,
    onSave: (WxLocationConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var temp by remember(currentConfig) { mutableStateOf(currentConfig) }
    val picked = WxLocation.pickedLocation

    LaunchedEffect(picked) {
        picked?.let { temp = temp.copy(latitude = it.first, longitude = it.second) }
    }

    ConfigPageScaffold(
        title = "虚拟定位",
        configData = temp,
        onSave = onSave,
        onDismiss = onDismiss
    ) {
        PreferenceSection(title = "坐标（国测局 GCJ-02）") {
            InputItem(
                title = "纬度",
                value = temp.latitude.toString(),
                onValueChange = { temp = temp.copy(latitude = it.toFloatOrNull() ?: temp.latitude) },
                placeholder = "31.135633",
                keyboardType = KeyboardType.Decimal
            )
            InputItem(
                title = "经度",
                value = temp.longitude.toString(),
                onValueChange = { temp = temp.copy(longitude = it.toFloatOrNull() ?: temp.longitude) },
                placeholder = "121.66625",
                keyboardType = KeyboardType.Decimal
            )
        }

        PreferenceSection(title = "地图选点") {
            ActionItem(
                title = "打开地图选点",
                description = "在微信里选好位置后自动回填上面的坐标",
                onClick = { WxLocation.openMapPicker() }
            )
            ActionItem(
                title = "重置为默认（上海）",
                description = "31.135633, 121.66625",
                onClick = { temp = WxLocationConfig() }
            )
        }
    }
}
