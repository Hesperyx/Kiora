package cn.hxy.kiora.ui.pages.configs

import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.hxy.kiora.conf.NotificationChannelConfig
import cn.hxy.kiora.hook.notification.NOTIFICATION_CHANNEL_GROUP_ID
import cn.hxy.kiora.ui.components.listitems.ActionItem
import cn.hxy.kiora.ui.components.listitems.SelectionItem
import cn.hxy.kiora.ui.components.scaffold.ConfigPageScaffold
import cn.hxy.kiora.utils.qq.HostInfo

private fun importanceLabel(level: Int): String = when (level) {
    NotificationManager.IMPORTANCE_HIGH -> "高（有声音）"
    NotificationManager.IMPORTANCE_DEFAULT -> "默认"
    NotificationManager.IMPORTANCE_LOW -> "低（无声）"
    NotificationManager.IMPORTANCE_MIN -> "最低"
    NotificationManager.IMPORTANCE_NONE -> "已屏蔽"
    else -> "未知"
}

@Composable
fun NotificationChannelPage(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val channels = remember {
        runCatching {
            HostInfo.hostContext.getSystemService(NotificationManager::class.java)
                .notificationChannels
                .filter { it.group == NOTIFICATION_CHANNEL_GROUP_ID }
                .sortedBy { it.name?.toString() }
        }.getOrDefault(emptyList())
    }

    ConfigPageScaffold(
        title = "通知渠道管理",
        configData = NotificationChannelConfig,
        onSave = {},
        onDismiss = onDismiss
    ) { _ ->
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            channels.forEach { channel ->
                SelectionItem(
                    title = channel.name?.toString() ?: channel.id,
                    subtitle = "${channel.description ?: channel.id}｜重要性：${importanceLabel(channel.importance)}",
                    isSelected = false,
                    onClick = {}
                )
            }
            if (channels.isEmpty()) {
                Text(
                    text = "暂无模块通知渠道，开启「MessagingStyle 通知」后自动生成",
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            ActionItem(
                title = "打开系统通知设置",
                description = "调整渠道重要性、开关通知",
                onClick = {
                    runCatching {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, HostInfo.hostContext.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }
            )
        }
    }
}
