package cn.hxy.kiora.hook.notification

import androidx.core.graphics.drawable.IconCompat
import cn.hxy.kiora.utils.reflect.getObjectOrNull

/** 会话聚合通知的目标（会话主体、所属渠道、私聊/群聊）。 */
internal data class ConversationTarget(
    val mainUin: Long,
    val mainName: String,
    val mainIcon: IconCompat?,
    val channel: NotifyChannel,
    val isGroupConversation: Boolean
) {
    val historyKey: String get() = "$channel+$mainUin"
}

internal data class RecentContactSnapshot(
    val chatType: Int,
    val abstractContent: Iterable<*>?,
    val sendMemberName: String?,
    val sendRemarkName: String?,
    val sendNickName: String?,
    val senderUin: Long,
    val peerUin: Long,
    val peerName: String?,
    val specialCareFlag: Byte,
    val msgBoxEvents: Any?
)

internal fun Any.toRecentContactSnapshot(): RecentContactSnapshot {
    return RecentContactSnapshot(
        chatType = (getObjectOrNull("chatType") as? Number)?.toInt() ?: 0,
        abstractContent = getObjectOrNull("abstractContent") as? Iterable<*>,
        sendMemberName = getObjectOrNull("sendMemberName") as? String,
        sendRemarkName = getObjectOrNull("sendRemarkName") as? String,
        sendNickName = getObjectOrNull("sendNickName") as? String,
        senderUin = (getObjectOrNull("senderUin") as? Number)?.toLong() ?: 0L,
        peerUin = (getObjectOrNull("peerUin") as? Number)?.toLong() ?: 0L,
        peerName = getObjectOrNull("peerName") as? String,
        specialCareFlag = (getObjectOrNull("specialCareFlag") as? Number)?.toByte() ?: 0.toByte(),
        msgBoxEvents = getObjectOrNull("listOfSpecificEventTypeInfosInMsgBox")
    )
}

internal fun RecentContactSnapshot.isSupportedChat(): Boolean {
    return chatType == CHAT_TYPE_PRIVATE || chatType == CHAT_TYPE_GROUP
}

internal fun RecentContactSnapshot.extractContent(): String? {
    return abstractContent
        ?.joinToString(separator = "") { element ->
            element?.getObjectOrNull("content") as? String ?: "[未解析消息]"
        }
        ?.takeIf { it.isNotBlank() }
}

internal fun RecentContactSnapshot.senderName(): String? {
    return sendMemberName?.takeIf { it.isNotBlank() }
        ?: sendRemarkName?.takeIf { it.isNotBlank() }
        ?: sendNickName?.takeIf { it.isNotBlank() }
}

internal fun RecentContactSnapshot.isSpecialCare(): Boolean {
    return specialCareFlag == 1.toByte() ||
            msgBoxEvents.toString().contains("eventTypeInMsgBox=1006")
}

internal const val CHAT_TYPE_PRIVATE = 1
internal const val CHAT_TYPE_GROUP = 2
