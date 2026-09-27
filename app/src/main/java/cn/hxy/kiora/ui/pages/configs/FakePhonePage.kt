package cn.hxy.kiora.ui.pages.configs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cn.hxy.kiora.conf.PhoneConfig
import cn.hxy.kiora.ui.components.listitems.InputItem
import cn.hxy.kiora.ui.components.scaffold.ConfigPageScaffold

@Composable
fun FakePhonePage(
    currentConfig: PhoneConfig,
    onSave: (PhoneConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var phoneText by remember(currentConfig) { mutableStateOf(currentConfig.phone) }

    ConfigPageScaffold(
        title = "设置伪装号码",
        configData = PhoneConfig(phone = phoneText),
        onSave = onSave,
        onDismiss = onDismiss
    ) { _ ->
        InputItem(
            title = "手机号码",
            value = phoneText,
            onValueChange = { phoneText = it },
            placeholder = "留空则显示 1145141919810",
            modifier = Modifier.fillMaxWidth()
        )
    }
}
