package cn.hxy.kiora.qq.ui

import androidx.compose.runtime.Composable
import cn.hxy.kiora.qq.conf.TroopSetConfig
import cn.hxy.kiora.ui.components.biz.AsyncSelectorDialog
import cn.hxy.kiora.ui.components.dialogs.SelectionItem
import cn.hxy.kiora.qq.util.TroopTool

@Composable
fun TroopSelectorPage(
    title: String,
    currentConfig: TroopSetConfig,
    onSave: (TroopSetConfig) -> Unit,
    onDismiss: () -> Unit
) {
    AsyncSelectorDialog(
        title = title,
        currentSelection = currentConfig.selectedSet,
        dataLoader = TroopTool::getGroupList,
        mapper = { SelectionItem(it.group, it.groupName) },
        onDismiss = onDismiss,
        onConfirm = {
            onSave(TroopSetConfig(it))
            onDismiss()
        }
    )
}
