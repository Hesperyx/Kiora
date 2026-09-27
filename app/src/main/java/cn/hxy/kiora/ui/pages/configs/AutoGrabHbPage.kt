package cn.hxy.kiora.ui.pages.configs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import cn.hxy.kiora.conf.RedPacketConfig
import cn.hxy.kiora.plugin.bean.GroupInfo
import cn.hxy.kiora.ui.components.biz.AsyncSelectorDialog
import cn.hxy.kiora.ui.components.dialogs.CenterDialog
import cn.hxy.kiora.ui.components.dialogs.CenterDialogContainer
import cn.hxy.kiora.ui.components.dialogs.SelectionItem
import cn.hxy.kiora.ui.components.listitems.ActionItem
import cn.hxy.kiora.ui.components.listitems.InputItem
import cn.hxy.kiora.ui.components.listitems.SwitchItem
import cn.hxy.kiora.ui.components.listitems.SelectionItem as ListSelectionItem
import cn.hxy.kiora.ui.components.scaffold.ConfigPageScaffold
import cn.hxy.kiora.utils.qq.TroopTool

/**
 * 自动抢红包配置页，移植自 QFun AutoGrabHbPage。
 */
@Composable
fun AutoGrabHbPage(
    config: RedPacketConfig,
    onSave: (RedPacketConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    // 临时状态
    var delay by remember { mutableStateOf(if (config.delay == 0L) "" else config.delay.toString()) }
    var minAverage by remember {
        mutableStateOf(if (config.minAverage == 0) "" else config.minAverage.toString())
    }
    var keywords by remember { mutableStateOf(config.keywords.joinToString(",")) }
    var autoReply by remember { mutableStateOf(config.autoReply.joinToString(",")) }
    var whiteList by remember { mutableStateOf(config.whiteList) }
    var targetTypes by remember { mutableStateOf(config.targetTypes) }
    var sendVoiceAfterGrab by remember { mutableStateOf(config.sendVoiceAfterGrab) }

    var showWhiteListDialog by remember { mutableStateOf(false) }
    var showTypeDialog by remember { mutableStateOf(false) }

    fun buildConfig(): RedPacketConfig = RedPacketConfig(
        delay = delay.toLongOrNull() ?: 0L,
        minAverage = minAverage.toIntOrNull() ?: 0,
        keywords = keywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
        autoReply = autoReply.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        whiteList = whiteList,
        targetTypes = targetTypes,
        sendVoiceAfterGrab = sendVoiceAfterGrab,
    )

    if (showWhiteListDialog) {
        AsyncSelectorDialog(
            title = "选择白名单群聊",
            currentSelection = whiteList,
            dataLoader = { TroopTool.getGroupList() },
            mapper = { group: GroupInfo -> SelectionItem(group.group, group.groupName) },
            onDismiss = { showWhiteListDialog = false },
            onConfirm = {
                whiteList = it
                showWhiteListDialog = false
            }
        )
    }

    if (showTypeDialog) {
        CenterDialog(
            visible = true,
            onDismiss = { showTypeDialog = false },
        ) {
            CenterDialogContainer(
                title = "选择红包类型",
                onDismiss = { showTypeDialog = false },
                onConfirm = { showTypeDialog = false }
            ) {
                val types = listOf(
                    Triple(1, "普通/拼手气红包", "1"),
                    Triple(32, "口令红包", "32"),
                    Triple(1024, "专属红包", "1024"),
                    Triple(65536, "语音红包", "65536"),
                )
                types.forEach { (code, name, desc) ->
                    ListSelectionItem(
                        title = name,
                        subtitle = desc,
                        isSelected = code in targetTypes,
                        onClick = {
                            targetTypes = targetTypes.toMutableSet().apply {
                                if (code in this) remove(code) else add(code)
                            }
                        }
                    )
                }
            }
        }
    }

    ConfigPageScaffold(
        title = "领取配置",
        configData = buildConfig(),
        onSave = onSave,
        onDismiss = onDismiss,
    ) { current ->
        InputItem(
            title = "延迟时间（毫秒）",
            value = delay,
            onValueChange = { delay = it },
            placeholder = "0",
            keyboardType = KeyboardType.Number,
        )
        InputItem(
            title = "最低平均金额（分）",
            value = minAverage,
            onValueChange = { minAverage = it },
            placeholder = "0",
            keyboardType = KeyboardType.Number,
        )
        InputItem(
            title = "关键词过滤（逗号分隔）",
            value = keywords,
            onValueChange = { keywords = it },
            placeholder = "关键词1,关键词2",
        )
        InputItem(
            title = "自动回复（逗号分隔，随机选择）",
            value = autoReply,
            onValueChange = { autoReply = it },
            placeholder = "谢谢,感谢红包",
        )
        ActionItem(
            title = "红包类型",
            description = "已选择 ${targetTypes.size} 种类型",
            onClick = { showTypeDialog = true },
        )
        SwitchItem(
            title = "抢语音红包后发送语音",
            checked = sendVoiceAfterGrab,
            onCheckedChange = { sendVoiceAfterGrab = it },
            description = "抢到语音红包后在群里发送合成的口令语音",
        )
        ActionItem(
            title = "白名单群聊",
            description = "已选择 ${whiteList.size} 个群",
            onClick = { showWhiteListDialog = true },
        )
    }
}
