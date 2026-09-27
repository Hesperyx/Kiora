package cn.hxy.kiora.conf

import kotlinx.serialization.Serializable

@Serializable
data class SimplifyDrawerConfig(
    /** 要隐藏的侧滑栏条目 id 集合，如 d_album / d_qqwallet */
    val hiddenItems: Set<String> = emptySet()
)
