package cn.hxy.kiora.hook.notification

import android.app.NotificationManager
import androidx.compose.runtime.Composable
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.qq.conf.NotificationChannelConfig
import cn.hxy.kiora.hook.base.BaseClickableHookItem
import cn.hxy.kiora.qq.ui.NotificationChannelPage
import cn.hxy.kiora.host.HostInfo

/**
 * 通知渠道管理入口，移植自 TCQT NotificationChannelManager。
 * TCQT 自建渠道管理 Activity，这里降级为渠道清单 + 跳转系统通知设置。
 */
@HookItemAnnotation(
    "通知渠道管理",
    "创建并管理模块的通知渠道组（联系人/特别关心/群/空间），点击进入渠道管理",
    HookCategory.NOTIFICATION,
    "All"
)
object NotificationChannelManager :
    BaseClickableHookItem<NotificationChannelConfig>(NotificationChannelConfig.serializer()) {

    override val defaultConfig: NotificationChannelConfig = NotificationChannelConfig

    override fun onHook() {
        runCatching {
            val notificationManager =
                HostInfo.hostContext.getSystemService(NotificationManager::class.java)
            ensureNotificationChannels(notificationManager)
        }
    }

    @Composable
    override fun ConfigContent(onDismiss: () -> Unit) {
        NotificationChannelPage(onDismiss = onDismiss)
    }
}
