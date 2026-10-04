package cn.hxy.kiora.qq.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cn.hxy.kiora.qq.conf.OneClickLikeConfig
import cn.hxy.kiora.ui.components.listitems.SelectionItem
import cn.hxy.kiora.ui.components.listitems.SelectionGroup
import cn.hxy.kiora.ui.components.scaffold.ConfigPageScaffold

@Composable
fun OneClickLikePage(
    currentConfig: OneClickLikeConfig,
    onSave: (OneClickLikeConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember(currentConfig) { mutableStateOf(currentConfig.mode) }

    fun buildConfig(): OneClickLikeConfig = OneClickLikeConfig(mode = mode)

    ConfigPageScaffold(
        title = "一键点赞",
        configData = buildConfig(),
        onSave = onSave,
        onDismiss = onDismiss
    ) { _ ->
        SelectionGroup {
            SelectionItem(
                title = "协议层",
                subtitle = "拦点赞发送请求：单次 10 赞 × 5 次，一次点满当日上限",
                isSelected = mode == OneClickLikeConfig.MODE_PROTOCOL,
                onClick = { mode = OneClickLikeConfig.MODE_PROTOCOL }
            )
            SelectionItem(
                title = "UI 点击层",
                subtitle = "拦访客页与资料卡的点赞点击，把点赞动作重复多次（SVIP 20 / 普通 10）",
                isSelected = mode == OneClickLikeConfig.MODE_UI,
                onClick = { mode = OneClickLikeConfig.MODE_UI }
            )
        }
    }
}
