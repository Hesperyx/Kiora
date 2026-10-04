package cn.hxy.kiora.hook.debug

import com.google.protobuf.UnknownFieldSet

internal fun ByteArray.toHexString(): String =
    joinToString(separator = "") { "%02X".format(it) }

internal fun String.hexToByteArray(): ByteArray {
    val clean = trim().replace(" ", "")
    return ByteArray(clean.length / 2) { i ->
        clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}

/**
 * 无 schema 解析 protobuf，取字段 1 的字节串转 UTF-8（即 bcmd）。
 * 解析失败返回空串。
 */
internal fun parseFieldOneAsString(body: ByteArray): String = runCatching {
    val fields = UnknownFieldSet.parseFrom(body).getField(1)
    fields.lengthDelimitedList.joinToString(separator = "") { it.toStringUtf8() }
}.getOrDefault("")
