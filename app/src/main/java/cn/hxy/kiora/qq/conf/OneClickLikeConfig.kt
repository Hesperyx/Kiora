package cn.hxy.kiora.qq.conf

import kotlinx.serialization.Serializable

@Serializable
data class OneClickLikeConfig(
    /** 1 = 协议层（改请求里的点赞数量），2 = UI 点击层（重复点赞动作） */
    val mode: Int = MODE_PROTOCOL
) {
    companion object {
        const val MODE_PROTOCOL = 1
        const val MODE_UI = 2
    }
}
