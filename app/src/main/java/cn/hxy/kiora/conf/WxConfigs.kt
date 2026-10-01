package cn.hxy.kiora.conf

import kotlinx.serialization.Serializable

/**
 * 微信侧 hook 的结构化配置。
 *
 * 与 `conf/` 里其它文件同构（`@Serializable` 数据类 + `ObjectStore` 持久化），
 * 只有需要**一整组**相关值的功能才放这里；单个标量值用
 * [cn.hxy.kiora.utils.wx.WxPrefs] 的属性委托，不必为它造数据类。
 */

/**
 * 发送文本格式（WA `MsgFormatHook`）。
 *
 * 默认值与 WA 一致（`${sendText}喵~`），方便对照行为。
 * 占位符：`${sendText}`（原文）、`${line}`（换行）、`${sendTime}`（发送时刻）。
 */
@Serializable
data class WxMsgFormatConfig(
    val textFormat: String = "\${sendText}喵~",
    val timeFormat: String = "HH:mm:ss"
)

/** 语音时长（WA `VoiceLengthHook`）：发送的语音消息显示为多少秒。 */
@Serializable
data class WxVoiceLengthConfig(
    val seconds: Int = 1
)

/** 运动步数（WA `SportStepHook`）：展示值，内部还会与 98800 取小。 */
@Serializable
data class WxSportStepConfig(
    val step: Long = 88888L
)

/**
 * 虚拟定位（WA `LocationHook`）。
 *
 * 默认值沿用 WA：上海（31.135633, 121.66625）—— 这个点本身没有含义，
 * 只是明显与真实定位不同，方便确认功能生效。
 */
@Serializable
data class WxLocationConfig(
    val latitude: Float = 31.135633f,
    val longitude: Float = 121.66625f
)

/** 自动点击登录（WA `AutoLoginWinHook`）：三个勾选项。 */
@Serializable
data class WxAutoLoginConfig(
    val autoSyncMsg: Boolean = true,
    val showLoginDevice: Boolean = true,
    val autoLoginDevice: Boolean = false
)

/**
 * 屏蔽通话铃声（WA `DisableRingtonePlayHook`）。
 *
 * 两个开关分开配：呼出与呼入的场景常不一样（比如只想去掉自己的呼出铃声）。
 */
@Serializable
data class WxRingtoneConfig(
    val blockOutCall: Boolean = true,
    val blockInCall: Boolean = false
)
