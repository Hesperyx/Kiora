package com.tencent.mm.api

/**
 * 单测夹具：模拟微信宿主里的 `com.tencent.mm.api.IEmojiInfo`。
 *
 * 包名与接口名必须**逐字符**等于 [cn.hxy.kiora.utils.wx.WeChatHookContracts.EmojiClick.EMOJI_INFO_TYPE]，
 * 否则 `readMd5` 里 `Class.forName` 的主分支不会命中。
 * 该接口只存在于 test 源集，不会进 APK。
 *
 * 返回类型写成可空：微信那边是 Java 接口（无空值强制），
 * 生产签名虽然是非空 String，运行期仍可能返回 null，这里如实建模。
 */
interface IEmojiInfo {
    fun getMd5(): String?
}
