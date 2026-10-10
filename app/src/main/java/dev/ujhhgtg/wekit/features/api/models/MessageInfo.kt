@file:Suppress("NOTHING_TO_INLINE", "unused")

package dev.ujhhgtg.wekit.features.api.core.models

import android.content.ContentValues
import dev.ujhhgtg.reflekt.reflekt
import dev.ujhhgtg.wekit.features.api.core.WeApi
import dev.ujhhgtg.wekit.features.api.core.WeMessageApi
import dev.ujhhgtg.wekit.utils.serialization.NativeXmlParser
import dev.ujhhgtg.wekit.utils.serialization.XmlObject
import dev.ujhhgtg.wekit.utils.serialization.XmlPrimitive
import dev.ujhhgtg.wekit.utils.serialization.XmlUtils
import dev.ujhhgtg.wekit.utils.serialization.XmlValue
import dev.ujhhgtg.wekit.utils.serialization.asInt
import dev.ujhhgtg.wekit.utils.serialization.asLong
import dev.ujhhgtg.wekit.utils.serialization.asString
import dev.ujhhgtg.wekit.utils.serialization.get
import dev.ujhhgtg.wekit.utils.serialization.getByPath
import dev.ujhhgtg.wekit.utils.strings.isGroupChatWxId
import dev.ujhhgtg.wekit.utils.strings.stripWxId
import java.nio.ByteBuffer

class MessageInfo(val instance: Any) {

    val typeCode by lazy { getFieldByName<Int>(instance, "field_type") }
    val type by lazy { MessageType.fromCode(typeCode) }

    val id by lazy { getFieldByName<Long>(instance, "field_msgId") }
    val serverId by lazy { getFieldByName<Long>(instance, "field_msgSvrId") }
    val isSend by lazy { getFieldByName<Int>(instance, "field_isSend") }
    val createTime by lazy { getFieldByName<Long>(instance, "field_createTime") }
    // field_talker / field_content 均由 `cursor.getString()` 或 `contentValues.getAsString()` 填充,
    // 微信在缺失时会留 null, 故此处降级为空串而非抛 NPE
    val talker by lazy { getFieldByName<String?>(instance, "field_talker").orEmpty() }
    val content by lazy { getFieldByName<String?>(instance, "field_content").orEmpty() }

    val actualContent: String
        get() {
            var text = content
            if (isInGroupChat) {
                text = text.stripWxId()
            }
            return text
        }

    val quoteMsgActualContent: String?
        get() {
            val quoteMsg = toQuoteMessage() ?: return null

            var text = quoteMsg.title
            if (isInGroupChat) {
                text = text.stripWxId()
            }

            return text
        }

    val humanReadableRepr: String
        get() {
            val type = type ?: return "[${MessageType.UNKNOWN.displayName}]"

            return when {
                type.code == MessageType.QUOTE.code -> quoteMsgActualContent ?: actualContent
                type.isText -> actualContent
                type.isSystem -> actualContent
                else -> "[${type.displayName}]"
            }
        }

    val imagePath by lazy { getFieldByName<String?>(instance, "field_imgPath") }
    val stickerMd5 by lazy {
        imagePath?.takeIf { it.isNotBlank() }
            ?: XmlUtils.extractXmlAttr(content, "md5").takeIf { it.isNotBlank() }
            ?: XmlUtils.extractXmlTag(content, "md5").takeIf { it.isNotBlank() }
    }
    /** 微信在没有该列 (或 ContentValues 中无 `lvbuffer` 键) 时会留 null, 因此必须可空 */
    val lvBuffer by lazy { getFieldByName<ByteArray?>(instance, "field_lvbuffer") }
    val talkerId by lazy { getFieldByName<Int>(instance, "field_talkerId") }
    val seq by lazy { getFieldByName<Long>(instance, "field_msgSeq") }

    val msgSource: String by lazy {
        val buffer = lvBuffer ?: return@lazy ""
        if (buffer.isEmpty()) return@lazy ""
        if (buffer[0] != '{'.code.toByte() || buffer.last() != '}'.code.toByte()) return@lazy ""

        val bb = ByteBuffer.wrap(buffer)
        bb.position(1) // skip '{'

        // skip string field (2-byte length prefix + data)
        if (bb.remaining() >= 2) {
            val n1 = bb.short.toInt()
            if (n1 > 3072) error("Buffer String Length Error")
            if (n1 != 0 && bb.remaining() >= n1) bb.position(bb.position() + n1)
        }

        // skip 4-byte int field
        if (bb.remaining() >= 4) bb.position(bb.position() + 4)

        if (bb.remaining() < 2) return@lazy ""

        val n2 = bb.short.toInt()
        if (n2 > 3072) error("Buffer String Length Error")
        if (n2 == 0 || bb.remaining() < n2) return@lazy ""

        val bytes = ByteArray(n2)
        bb.get(bytes)
        String(bytes, Charsets.UTF_8)
    }

    val mentionedUsers: List<String> by lazy {
        if (msgSource.isEmpty()) return@lazy emptyList()
        val xml = try {
            NativeXmlParser.toXmlObject(msgSource)
        } catch (_: Exception) {
            return@lazy emptyList()
        }
        val atUserListStr = xml.getByPath("msgsource.atuserlist")?.asString ?: return@lazy emptyList()
        atUserListStr.split(",").filter { it.isNotEmpty() }
    }

    val isAtMe get() = mentionedUsers.contains(WeApi.selfWxId)

    val isAnnounceAll get() = mentionedUsers.contains("announcement@all")

    val isNotifyAll: Boolean
        get() {
            if (mentionedUsers.contains("notify@all") || mentionedUsers.contains("announcement@all")) {
                val contentText = actualContent
                return contentText.contains("@所有人") || contentText.contains("@ all people")
            }
            return false
        }

    val isInGroupChat get() = talker.isGroupChatWxId
    val isOfficialAccount get() = talker.startsWith("gh_")
    val sender by lazy {
        @Suppress("DEPRECATION")
        if (typeCode == MessageType.SYSTEM.code) {
            return@lazy "system"
        }

        if (typeCode == MessageType.PAT.code) {
            val patMsg = PatMessage(content)
            return@lazy patMsg.fromUser
        }

        if (isSelfSender) {
            return@lazy WeApi.selfWxId
        }

        if (!isInGroupChat) {
            return@lazy talker
        }

        return@lazy content.split(':')[0]
    }

    val isSelfSender get() = isSend != 0

    inline fun toPatMessage(): PatMessage? {
        if (typeCode != MessageType.PAT.code)
            return null

        return PatMessage(content)
    }

    fun toQuoteMessage(): QuoteMessage? {
        if (typeCode != MessageType.QUOTE.code)
            return null

        return QuoteMessage(content)
    }

    fun toTransferMessage(): TransferMessage? {
        if (type != MessageType.TRANSFER)
            return null

        return TransferMessage(content)
    }

    fun toFileMessage(): FileMessage? {
        if (type != MessageType.FILE)
            return null

        return FileMessage(content)
    }

    fun toImageMessage(): ImageMessage? {
        if (type != MessageType.IMAGE)
            return null

        return ImageMessage(content)
    }

    class FileMessage(xmlStr: String) {

        private val xml = NativeXmlParser.toXmlObject(xmlStr.cleanupXml())

        val title by lazy { xml.getByPath("msg.appmsg.title").xmlString() }
        val size by lazy { xml.getByPath("msg.appmsg.appattach.totallen").xmlLong() }
        val ext by lazy { xml.getByPath("msg.appmsg.appattach.fileext").xmlString() }
        val md5 by lazy { xml.getByPath("msg.appmsg.md5").xmlString() }
        val url by lazy { xml.getByPath("msg.appmsg.appattach.cdnattachurl").xmlString() }
        val key by lazy { xml.getByPath("msg.appmsg.appattach.aeskey").xmlString() }

        /**
         * appmsg 内层 `<type>`:
         * - 6 / 130: 文件已就绪, 可以下载
         * - 74 / 131: 对方仍在上传中, 此时触发下载必然失败
         */
        val appMsgType by lazy { xml.getByPath("msg.appmsg.type")?.asInt }

        /** 对方是否仍在上传 (气泡显示"对方上传中")。 */
        val isSenderUploading get() = appMsgType == 74 || appMsgType == 131

        /** 文件是否已就绪、可下载。 */
        val isDownloadable get() = appMsgType == 6 || appMsgType == 130
    }

    class ImageMessage(xmlStr: String) {

        private val xml = NativeXmlParser.toXmlObject(xmlStr.cleanupXml())

        val md5 by lazy { xml.getByPath("msg.img.md5").xmlString() }
        val bigImgUrl by lazy { xml.getByPath("msg.img.cdnbigimgurl").xmlString() }
        val midImgUrl by lazy { xml.getByPath("msg.img.cdnmidimgurl").xmlString() }
        val thumbUrl by lazy { xml.getByPath("msg.img.cdnthumburl").xmlString() }
        val aesKey by lazy { xml.getByPath("msg.img.aeskey").xmlString() }
    }

    class PatMessage(xmlStr: String) {

        private val xml = NativeXmlParser.toXmlObject(xmlStr.cleanupXml())

        val createTime by lazy { recordObj["createTime"].xmlLong() }
        val fromUser by lazy { recordObj["fromUser"].xmlString() }
        val pattedUser by lazy { recordObj["pattedUser"].xmlString() }
        val readStatus by lazy { recordObj["readStatus"].xmlInt() }
        val recordNum by lazy { xml.getByPath("msg.appmsg.patMsg.records.recordNum").xmlInt() }
        val showModifyTip by lazy { recordObj["showModifyTip"].xmlInt() }
        val svrId by lazy { recordObj["svrId"].xmlLong() }
        val talker by lazy { xml.getByPath("msg.appmsg.patMsg.chatUser").xmlString() }
        val template by lazy { recordObj["template"].xmlString() }
        val recordObj by lazy {
            xml.getByPath("msg.appmsg.patMsg.records.record") as? XmlObject ?: XmlObject(emptyMap())
        }
    }

    class QuoteMessage(xmlStr: String) {
        private val xml = NativeXmlParser.toXmlObject(xmlStr.cleanupXml())

        val title by lazy { xml.getByPath("msg.appmsg.title").xmlString() }
        val chatusr by lazy { xml.getByPath("msg.appmsg.refermsg.chatusr").xmlString() }
        val displayname by lazy { xml.getByPath("msg.appmsg.refermsg.displayname").xmlString() }
        val msgsource by lazy { xml.getByPath("msg.appmsg.refermsg.msgsource").xmlString() }
        val svrid by lazy { xml.getByPath("msg.appmsg.refermsg.svrid").xmlLong() }
        val fromusr by lazy { xml.getByPath("msg.appmsg.refermsg.fromusr").xmlString() }
        val type by lazy { xml.getByPath("msg.appmsg.refermsg.type").xmlInt() }
        val content by lazy { xml.getByPath("msg.appmsg.refermsg.content").xmlString() }
    }

    class TransferMessage(xmlStr: String) {

        private val xml = NativeXmlParser.toXmlObject(xmlStr.cleanupXml())

        val title by lazy { xml.getByPath("msg.appmsg.title").xmlString() }
        val des by lazy { xml.getByPath("msg.appmsg.des").xmlString() }

        // 'transcationid' is WeChat's typo
        val transactionId by lazy { xml.getByPath("msg.appmsg.wcpayinfo.transcationid").xmlString() }
        val transferId by lazy { xml.getByPath("msg.appmsg.wcpayinfo.transferid").xmlString() }
        val beginTransferTime by lazy { xml.getByPath("msg.appmsg.wcpayinfo.begintransfertime").xmlLong() }
        val payerUsername by lazy { xml.getByPath("msg.appmsg.wcpayinfo.payer_username").xmlString() }
        val receiverUsername by lazy { xml.getByPath("msg.appmsg.wcpayinfo.receiver_username").xmlString() }
        val invalidTime by lazy { xml.getByPath("msg.appmsg.wcpayinfo.invalidtime").xmlInt() }
        val feedesc by lazy { xml.getByPath("msg.appmsg.wcpayinfo.feedesc").xmlString() }
        val totalFee by lazy {
            xml.getByPath("msg.appmsg.wcpayinfo.total_fee")?.asString?.toLongOrNull() ?: 0L
        }
        val feeType by lazy {
            xml.getByPath("msg.appmsg.wcpayinfo.fee_type")?.asString.orEmpty()
        }
        val payMemo by lazy {
            xml.getByPath("msg.appmsg.wcpayinfo.pay_memo")?.asString.orEmpty()
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        private inline fun <T> getFieldByName(instance: Any, name: String): T {
            return instance.reflekt().getField(name, true) as T
        }

        /**
         * Safely extracts tag content directly from raw XML strings.
         * Bypasses JSON type coercion overhead and prevents 32-digit string truncation.
         */
        private fun extractXmlTag(xml: String, tag: String): String? {
            val startTag = "<$tag>"
            val endTag = "</$tag>"
            if (!xml.contains(startTag) || !xml.contains(endTag)) return null

            val content = xml.substringAfter(startTag).substringBefore(endTag)
            return if (content.startsWith("<![CDATA[")) {
                content.substringAfter("<![CDATA[").substringBefore("]]>")
            } else {
                content
            }.trim()
        }

        private fun String.cleanupXml(): String {
            return "<msg>" + substringAfter("<msg>")
                .substringBeforeLast("</msg>")
                .replace("\r", "")
                .replace("\n", "")
                .replace("\t", "")
                .replace("<?xml version=\"1.0\"?>", "") + "</msg>"
        }

        fun fromContentValues(contentValues: ContentValues): MessageInfo {
            return MessageInfo(WeMessageApi.convertMsgInfoInstanceFromContentValues(contentValues))
        }
    }
}

// Lenient readers for message XML: WeChat omits optional tags and occasionally ships a value under
// a shape the parser cannot coerce to a primitive. The strict `asString`/`asLong`/`asInt` helpers
// throw in both cases, which used to crash any feature that read such a message; these degrade to a
// neutral value instead.
private fun XmlValue?.xmlString(): String = (this as? XmlPrimitive)?.value.orEmpty()

private fun XmlValue?.xmlLong(): Long = (this as? XmlPrimitive)?.value?.toLongOrNull() ?: 0L

private fun XmlValue?.xmlInt(): Int = (this as? XmlPrimitive)?.value?.toIntOrNull() ?: 0
