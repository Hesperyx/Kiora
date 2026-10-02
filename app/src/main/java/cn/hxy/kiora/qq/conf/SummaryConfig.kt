package cn.hxy.kiora.qq.conf

import kotlinx.serialization.Serializable

@Serializable
data class SummaryConfig(
    val key: String = "",
    val summaryOrUrl: String = ""
)
