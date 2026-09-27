package cn.hxy.kiora.hook.notification

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager

const val NOTIFICATION_CHANNEL_GROUP_ID = "qq_evolution"
const val NOTIFICATION_CHANNEL_GROUP_NAME = "Kiora 通知优化"

/** 通知渠道分类与渠道创建，移植自 TCQT（思路参考 QAuxiliary）。 */
enum class NotifyChannel {
    FRIEND,
    FRIEND_SPECIAL,
    GROUP,
    QZONE
}

internal fun NotifyChannel.channelId(): String = when (this) {
    NotifyChannel.FRIEND -> "QQ_Friend"
    NotifyChannel.FRIEND_SPECIAL -> "QQ_Friend_Special"
    NotifyChannel.GROUP -> "QQ_Group"
    NotifyChannel.QZONE -> "QQ_Zone"
}

/**
 * 确保渠道组与四个会话渠道存在（幂等）。
 *
 * 创建逻辑只保留这一份：MessagingStyleNotification 与 NotificationChannelManager
 * 两个开关互相独立，任一启用都需要渠道就绪，因此各自在 onHook 里调用本函数，
 * 不再重复维护 group/channel 的创建代码。
 */
internal fun ensureNotificationChannels(notificationManager: NotificationManager) {
    notificationManager.createNotificationChannelGroup(
        NotificationChannelGroup(NOTIFICATION_CHANNEL_GROUP_ID, NOTIFICATION_CHANNEL_GROUP_NAME)
    )

    val channels = listOf(
        buildChannel(NotifyChannel.FRIEND, "联系人消息", "QQ 私聊消息通知", NotificationManager.IMPORTANCE_HIGH),
        buildChannel(NotifyChannel.FRIEND_SPECIAL, "特别关心消息", "QQ 特别关心好友私聊消息通知", NotificationManager.IMPORTANCE_HIGH),
        buildChannel(NotifyChannel.GROUP, "群消息", "QQ 群消息通知", NotificationManager.IMPORTANCE_HIGH),
        buildChannel(NotifyChannel.QZONE, "空间动态", "QQ 空间动态通知", NotificationManager.IMPORTANCE_DEFAULT)
    )

    if (channels.any { notificationManager.getNotificationChannel(it.id) == null }) {
        notificationManager.createNotificationChannels(channels)
    }
}

private fun buildChannel(
    channel: NotifyChannel,
    name: String,
    desc: String,
    importance: Int
): NotificationChannel {
    val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    return NotificationChannel(channel.channelId(), name, importance).apply {
        group = NOTIFICATION_CHANNEL_GROUP_ID
        description = desc
        setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes)
        enableVibration(true)
        enableLights(true)
    }
}
