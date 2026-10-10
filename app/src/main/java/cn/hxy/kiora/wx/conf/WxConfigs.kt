package cn.hxy.kiora.wx.conf

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

/**
 * 自动点击登录（WA `AutoLoginWinHook`）：三个勾选项。
 *
 * 这是微信端**没有 WeKit 对应项**的原生功能（WeKit 只有「自动批准设备登录」，
 * 语义不同），因此不能随重复项一起删除，否则整条功能会从设置页消失。
 */
@Serializable
data class WxAutoLoginConfig(
    val autoSyncMsg: Boolean = true,
    val showLoginDevice: Boolean = true,
    val autoLoginDevice: Boolean = false
)
