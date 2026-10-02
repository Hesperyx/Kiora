package cn.hxy.kiora.qq.conf

import kotlinx.serialization.Serializable

@Serializable
data class DeviceConfig(
    val fakeModel: String = ""
)
