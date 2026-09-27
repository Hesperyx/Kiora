package cn.hxy.kiora.conf

import kotlinx.serialization.Serializable

@Serializable
data class MessagingStyleConfig(
    /** 禁用子渠道发送通知（所有会话共用 4 个基础渠道） */
    val disableSubChannel: Boolean = false,
    /** 禁用通知气泡 */
    val disableBubble: Boolean = false,
    /** 自动清除多余通知子渠道（隐含 disableSubChannel） */
    val autoClearSubChannel: Boolean = false
)
