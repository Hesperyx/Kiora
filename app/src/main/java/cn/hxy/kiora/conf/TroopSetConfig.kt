package cn.hxy.kiora.conf

import kotlinx.serialization.Serializable

@Serializable
data class TroopSetConfig(
    val selectedSet: Set<String> = emptySet()
)
