package cn.hxy.kiora.conf

import kotlinx.serialization.Serializable

@Serializable
data class PhoneConfig(
    val phone: String = ""
)
