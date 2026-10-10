package dev.ujhhgtg.wekit.features.items.chat

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import dev.ujhhgtg.reflekt.reflekt
import dev.ujhhgtg.wekit.R
import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexConstructor
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.features.api.core.WeDatabaseApi
import dev.ujhhgtg.wekit.features.api.core.WeMessageApi
import dev.ujhhgtg.wekit.features.api.core.WeXmlParserApi
import dev.ujhhgtg.wekit.features.api.core.models.MessageInfo
import dev.ujhhgtg.wekit.features.api.core.models.MessageType
import dev.ujhhgtg.wekit.features.api.ui.WeChatMessageViewApi
import dev.ujhhgtg.wekit.features.core.ClickableFeature
import dev.ujhhgtg.wekit.features.core.FeatureCategoryIds
import dev.ujhhgtg.wekit.data.KvStore
import dev.ujhhgtg.wekit.data.KvStore.prefOption
import dev.ujhhgtg.wekit.ui.content.AlertDialogContent
import dev.ujhhgtg.wekit.ui.content.TextButton
import dev.ujhhgtg.wekit.ui.content.m3.BaseWidget
import dev.ujhhgtg.wekit.ui.content.m3.DropDownMenuWidget
import dev.ujhhgtg.wekit.ui.content.m3.DropdownOption
import dev.ujhhgtg.wekit.ui.content.m3.SegmentedColumn
import dev.ujhhgtg.wekit.ui.content.m3.SwitchWidget
import dev.ujhhgtg.wekit.ui.utils.DeleteIcon
import dev.ujhhgtg.wekit.ui.utils.dpToPx
import dev.ujhhgtg.wekit.ui.utils.findViewWhich
import dev.ujhhgtg.wekit.ui.utils.showComposeDialog
import dev.ujhhgtg.wekit.utils.HookParam
import dev.ujhhgtg.wekit.utils.WeLogger
import dev.ujhhgtg.wekit.utils.android.isDarkMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import cn.hxy.kiora.wx.util.WeChatDexKit
import java.util.Collections
import java.util.WeakHashMap

object AntiMessageRecall : ClickableFeature(), IResolveDex, WeXmlParserApi.IAfterParseListener,
    WeChatMessageViewApi.ICreateViewListener {

    override val technicalId = "防撤回"
    override val nameRes = R.string.feature_anti_message_recall_name
    override val categoryIds = listOf(FeatureCategoryIds.CHAT)
    override val descriptionRes = R.string.feature_anti_message_recall_description

    private const val TAG = "AntiMessageRecall"

    private var recallOutgoing by prefOption("recall_outgoing", false)
    private var badgeType by prefOption("recall_badge_type", BADGE_TYPE_ICON)
    // WeChat only marks a message as revoked AFTER overwriting its row in place (destroying the
    // content). Our blocked recalls leave the row intact and unmarked, so the recalled list must be
    // tracked here, persisted as "<talker>\u001F<msgSvrId>" entries.
    private var recalledRecords by prefOption("recall_records", emptySet())
    private var recallRecordSchema by prefOption(RECALL_RECORD_SCHEMA_KEY, 0)

    // In-memory mirror of [recalledRecords] for the per-bind hot path.
    @Volatile
    private var recalledKeyCache: Set<String> = emptySet()

    // Guards the read-modify-write of [recalledKeyCache]/[recalledRecords]: `onParse` may run on a
    // worker thread while the scene-end hook runs on another, so concurrent recalls must be
    // serialized, otherwise one of them silently overwrites the other's record.
    private val recordLock = Any()

    // Content views currently dimmed by us, keyed by their bound row, so a rebind restores the
    // exact view we changed instead of clobbering other features' animations (e.g.
    // MessageEntranceAnimation).
    private val dimmedContentViews =
        Collections.synchronizedMap(WeakHashMap<View, View>())
    private val badges = Collections.synchronizedMap(WeakHashMap<View, RecallBadge>())

    // Scenes currently recalling a self-sent message, keyed by the NetSceneRevokeMsg instance, so
    // the scene-end hook can tell which message row the host is about to destroy.
    private val pendingRecallMsgs = Collections.synchronizedMap(WeakHashMap<Any, MessageInfo>())

    // `enable()` 已经是幂等的，但 `onEnable()` 里的钩子挂在单例上，若宿主/加载器
    // 意外重入仍可能叠加多条 doRevokeMsg 拦截器（每条都会置一次 result，重复消耗且日志刷屏）。
    // 用这个标记挡住重复挂载；[onDisable] 复位。
    @Volatile
    private var revokeHandlerHooked = false

    // Unique to NetSceneRevokeMsg's notice converter across the supported host range; anchors the
    // scene class without hard-coding its obfuscated name.
    private const val REVOKE_INVOKE_MESSAGE_XML =
        "<sysmsg type=\"invokeMessage\"><invokeMessage><text><![CDATA[%s]]></text><timestamp><![CDATA[%s]]></timestamp><link><text><![CDATA[%s]]></text></link><preContent><![CDATA[%s]]></preContent><type><![CDATA[%s]]></type><msgSource><![CDATA[%s]]></msgSource></invokeMessage></sysmsg>"

    private const val TYPE_KEY = $$".sysmsg.$type"
    // The revoke sysmsg carries the originating chat in <session>; it is the strongest guard
    // against matching an unrelated row that happens to share the (rare) message server id.
    private const val REVOKE_SESSION_KEY = ".sysmsg.revokemsg.session"
    private const val RECORD_SEPARATOR = "\u001F"
    private const val MAX_RECALL_RECORDS = 2000
    private const val RECALL_RECORD_SCHEMA_KEY = "recall_records_schema"
    private const val RECALL_RECORD_SCHEMA = 2
    private const val RECALL_ALPHA = 0.4f
    private const val BADGE_SIZE_DP = 28
    private const val BADGE_PADDING_DP = 6
    private const val BADGE_GAP_DP = 4

    private const val BADGE_TYPE_ICON = "icon"
    private const val BADGE_TYPE_TEXT = "text"

    private val badgeIconLight = "#C62828".toColorInt()
    private val badgeCircleLight = "#FFCDD2".toColorInt()
    private val badgeIconDark = "#EF9A9A".toColorInt()
    private val badgeCircleDark = "#3E1C1C".toColorInt()

    private class RecallBadge(
        val view: View,
        val isSelfSender: Boolean,
        var anchor: View,
        val preDrawListener: ViewTreeObserver.OnPreDrawListener,
        val rowLocation: IntArray = IntArray(2),
        val anchorLocation: IntArray = IntArray(2),
    )

    private data class ResolvedRevokeTarget(
        val message: MessageInfo,
        val keyValidated: Boolean,
    )

    private fun encodeKey(talker: String, serverId: Long) = "$talker$RECORD_SEPARATOR$serverId"

    /**
     * A persisted record is only trustworthy when it carries both a non-blank talker and a real
     * (positive) server id. Anything else is a leftover from a mis-matched lookup and must never
     * drive the "recalled" badge.
     */
    private fun isUsableRecallKey(key: String): Boolean {
        val sep = key.lastIndexOf(RECORD_SEPARATOR)
        if (sep <= 0) return false
        if (key.substring(0, sep).isBlank()) return false
        return (key.substring(sep + 1).toLongOrNull() ?: 0L) > 0L
    }

    private fun boundedRecallKeys(keys: Set<String>): Set<String> =
        if (keys.size <= MAX_RECALL_RECORDS) {
            keys
        } else {
            // `Set + element` yields an insertion-ordered LinkedHashSet (oldest first), so the
            // trimmed list keeps the most recent records.
            keys.toList().takeLast(MAX_RECALL_RECORDS).toSet()
        }

    /**
     * Records a blocked recall, atomically and with a bounded persisted set.
     *
     * Called from two different threads (`onParse` worker + scene-end hook), so the whole
     * read-modify-write-persist sequence runs under [recordLock]. The persisted set is capped so a
     * long-lived install cannot grow `recall_records` without bound.
     */
    private fun recordRecall(key: String) {
        synchronized(recordLock) {
            recalledKeyCache = boundedRecallKeys(recalledKeyCache + key)
            recalledRecords = recalledKeyCache
        }
    }

    // NetSceneRevokeMsg (scene 594, /cgi-bin/micromsg-bin/revokemsg): recalling a message sent from
    // this device never parses a revoke sysmsg — the scene rewrites the message row in place at
    // scene end (notice converter, isSend reset, row update), so the XML parser hook never sees it.
    // The constructor receives the target message; capture it for the scene-end hook.
    private val methodRevokeMsgSceneInit by dexConstructor {
        matcher {
            declaredClass {
                usingEqStrings(REVOKE_INVOKE_MESSAGE_XML)
            }
            paramCount = 3
        }
    }

    private val methodRevokeMsgSceneEnd by dexMethod {
        matcher {
            declaredClass {
                usingEqStrings(REVOKE_INVOKE_MESSAGE_XML)
            }
            name = "onGYNetEnd"
            paramCount = 6
        }
    }

    override fun onEnable() {
        synchronized(recordLock) {
            // Pre-schema-2 builds could persist a key after an ambiguous revoke lookup. Those
            // stale false positives survive restarts and make unrelated messages look recalled,
            // so discard the legacy cache once. Schema-2 records are only written after the
            // target row's server id has been checked against the revoke id.
            if (recallRecordSchema < RECALL_RECORD_SCHEMA) {
                val discarded = recalledRecords.size
                recalledRecords = emptySet()
                recalledKeyCache = emptySet()
                recallRecordSchema = RECALL_RECORD_SCHEMA
                if (discarded > 0) {
                    WeLogger.w(TAG, "discarded $discarded legacy recall record(s) during schema upgrade")
                }
            }

            // Older builds could persist rows without a server id (or with a blank talker) when a
            // revoke sysmsg happened to match an unrelated local row. Those entries would keep
            // lighting up "recalled" on unrelated messages forever, so drop them on load. New
            // records never pass through here with an unusable key (guarded in [onParse]).
            val sanitized = recalledRecords.filterTo(LinkedHashSet()) { isUsableRecallKey(it) }
            if (sanitized.size != recalledRecords.size) {
                WeLogger.w(TAG, "pruned ${recalledRecords.size - sanitized.size} unusable recall record(s)")
                recalledRecords = sanitized
            }
            recalledKeyCache = sanitized
        }
        WeXmlParserApi.addListener(this)
        WeChatMessageViewApi.addListener(this)
        hookRevokeHandler()

        installHook("AntiMessageRecall#1") {
            methodRevokeMsgSceneInit.hookBefore {
                val scene = thisObject ?: return@hookBefore
                val target = args.getOrNull(0) ?: return@hookBefore
                pendingRecallMsgs[scene] = MessageInfo(target)
            }
        }

        installHook("AntiMessageRecall#2") {
            methodRevokeMsgSceneEnd.hookBefore {
                val scene = thisObject ?: return@hookBefore
                if (!recallOutgoing) {
                    WeLogger.i(TAG, "revoke scene end skipped: recall outgoing disabled")
                    return@hookBefore
                }
                val msgInfo = pendingRecallMsgs.remove(scene)
                if (msgInfo == null) {
                    WeLogger.i(TAG, "revoke scene end skipped: no captured message")
                    return@hookBefore
                }
                // Recall rejected (e.g. the 2-minute window expired): keep WeChat's own failure path,
                // the message row was never rewritten anyway.
                val errType = args.getOrNull(1) as? Int ?: return@hookBefore
                val errCode = args.getOrNull(2) as? Int ?: return@hookBefore
                if (errType != 0 || errCode != 0) {
                    WeLogger.i(TAG, "revoke scene end skipped: scene failed")
                    return@hookBefore
                }
                if (!msgInfo.isSelfSender) {
                    WeLogger.i(TAG, "revoke scene end skipped: not self sender")
                    return@hookBefore
                }
                if (msgInfo.serverId <= 0L || msgInfo.talker.isBlank()) {
                    WeLogger.w(
                        TAG,
                        "revoke scene end skipped: talker=${msgInfo.talker}, svrId=${msgInfo.serverId}",
                    )
                    return@hookBefore
                }

                // Resolve the scene queue wrapper first: if that fails we bail out without recording,
                // so the original body keeps running and the recall just behaves unblocked.
                val (sceneWrapper, sceneEnd) = runCatching {
                    val sceneEndField = scene.reflekt().firstField {
                        type { it.isInterface && it.declaredMethods.any { m -> m.name == "onSceneEnd" } }
                    }
                    val wrapper = sceneEndField.get(scene) ?: error("scene wrapper is null")
                    @Suppress("UNCHECKED_CAST")
                    val sceneWrapperType = sceneEndField.self.type as Class<Any>
                    val method = sceneWrapperType.reflekt()
                        .firstMethod { name = "onSceneEnd"; parameterCount = 4 }
                    wrapper to method
                }.getOrElse { throwable ->
                    WeLogger.w(TAG, "revoke scene end skipped: cannot resolve scene end", throwable)
                    return@hookBefore
                }

                // Replicate the trailing queue completion of the skipped body so the NetScene queue and
                // the chat UI (progress dialog dismissal, refresh) finish the scene normally.
                sceneEnd.invoke(sceneWrapper, errType, errCode, args[3], scene)

                val key = encodeKey(msgInfo.talker, msgInfo.serverId)
                recordRecall(key)
                WeLogger.i(TAG, "kept recalled self message: $key")
                refreshBoundViews(msgInfo)
                result = null
            }
        }
    }

    /**
     * 「消息保留」的硬保险：直接把宿主的撤回处理方法吞掉。
     *
     * [onParse] 走的是「把撤回 sysmsg 的解析结果置空」，属上游 WeKit 的语义，只有在
     * 宿主仍处于「解析 sysmsg」阶段时才拦得住；一旦微信改版让撤回在别处落地，这条会
     * 静默失效 —— 用户看到的就是「开关开着，消息照样被撤回」。
     *
     * 这里额外挂一条 DexKit 方法替换（`doRevokeMsg` 直接不执行），撤回指令根本落不了地，
     * 保证最基本的「消息不被撤回」在任何一条通路失效时仍然成立。
     *
     * 两条通路互补而不是二选一：XML 通路负责在解析阶段就掐掉 sysmsg，本通路负责兜住
     * 解析没拦住、已经走到撤回处理函数的那些撤回。
     *
     * DexKit 结果缺失（用户还没跑过「查找方法」）时只记日志，不影响 XML 通路。
     */
    private fun hookRevokeHandler() {
        if (revokeHandlerHooked) {
            WeLogger.i(TAG, "撤回处理方法已挂载，跳过重复挂载")
            return
        }
        runCatching { WeChatDexKit.requireMethod(WeChatDexKit.ANTI_REVOKE_1) }
            .onSuccess { method ->
                runCatching {
                    // 走 BaseFeature 的 `Executable.hookBefore`（内部 registerUnhook），
                    // 这样 disable() → unhookAll() 时能真正卸载；否则关闭开关后仍会拦截撤回。
                    method.hookBefore(50) {
                        // Resolve the target row from the method arguments. The legacy comment
                        // described the signature as (talker, xmlSvrMsgId, ...); instead of
                        // trusting that order (it drifted between host builds), take the first
                        // positive id that actually maps to a real message row.
                        val resolved = resolveRevokeTarget(args)

                        // "Recall outgoing" is an explicit opt-in: when it is off the user wants
                        // their *own* recalls to go through. The scene-end hook already honours
                        // this, but this safety net used to swallow self-recalls unconditionally,
                        // so recalling your own message silently kept it. Let it pass here too.
                        if (resolved?.message?.isSelfSender == true && !recallOutgoing) {
                            WeLogger.i(TAG, "doRevokeMsg passthrough: self recall, recall_outgoing disabled")
                            return@hookBefore
                        }

                        // 置 result 即跳过原实现（见 IHookBridge.IMemberHookParam 的语义）。
                        if (resolved?.keyValidated == true) {
                            val msgInfo = resolved.message
                            val key = encodeKey(msgInfo.talker, msgInfo.serverId)
                            if (!recalledKeyCache.contains(key)) {
                                recordRecall(key)
                                WeLogger.i(TAG, "recorded & blocked revoke via doRevokeMsg: $key")
                                refreshBoundViews(msgInfo)
                            } else {
                                WeLogger.i(TAG, "blocked revoke via doRevokeMsg (already recorded): $key")
                            }
                        } else if (resolved == null) {
                            WeLogger.w(TAG, "blocked revoke via doRevokeMsg without a validated target")
                        } else {
                            WeLogger.w(TAG, "blocked revoke via doRevokeMsg without talker validation")
                        }
                        // 返回值必须贴合原方法签名：doRevokeMsg 若声明为 boolean，直接返回
                        // null 会让调用方在拆箱时抛 NPE（表现为聊天页闪退），而不是「撤回被吞」。
                        result = defaultReturnFor(method)
                    }
                    revokeHandlerHooked = true
                    WeLogger.i(TAG, "已挂载撤回处理方法替换：${method.declaringClass.name}#${method.name}")
                }.onFailure {
                    WeLogger.w(TAG, "挂载撤回处理方法失败，仅依赖 XML 解析拦截", it)
                }
            }
            .onFailure {
                WeLogger.w(TAG, "未取到撤回处理方法，仅依赖 XML 解析拦截", it)
            }
    }

    /**
     * 跳过原实现时使用的中性返回值：按原方法返回类型给出「不做事」的等价值，
     * 避免把 null 返回给基本类型方法导致拆箱 NPE。
     */
    private fun defaultReturnFor(method: java.lang.reflect.Method): Any? = when (method.returnType) {
        Void.TYPE -> null
        java.lang.Boolean.TYPE -> false
        java.lang.Integer.TYPE -> 0
        java.lang.Long.TYPE -> 0L
        java.lang.Short.TYPE -> 0.toShort()
        java.lang.Byte.TYPE -> 0.toByte()
        java.lang.Character.TYPE -> '\u0000'
        java.lang.Float.TYPE -> 0f
        java.lang.Double.TYPE -> 0.0
        else -> null
    }

    /**
     * Best-effort resolution of the message a `doRevokeMsg` call targets.
     *
     * The obfuscated signature carries the revoke id and the talker, but their positions are not
     * stable across host builds, so we scan every argument for a positive id that maps to a real
     * message row. Returns null when nothing validates — callers then fall back to the
     * "block unconditionally" safety-net behaviour rather than risking a false badge.
     */
    private fun resolveRevokeTarget(args: Array<Any?>): ResolvedRevokeTarget? {
        // The revoke id reaches doRevokeMsg as a boxed long in current builds, but historically it
        // has also arrived as an Int or a decimal String depending on the host overload. Accept all
        // of them so a valid row still resolves instead of degrading to an unbadged hard-block.
        val candidateIds = args.mapNotNull { value ->
            when (value) {
                is Long -> value
                is Int -> value.toLong()
                is String -> value.toLongOrNull()
                else -> null
            }
        }.filter { it > 0L }.distinct()

        // doRevokeMsg's diagnostic format is
        // "doRevokeMsg xmlSrvMsgId=%d talker=%s isGet=%s". Treat the first non-numeric string as
        // the talker hint and require the database row to match it before persisting a badge.
        val talkerHint = args.asSequence()
            .filterIsInstance<String>()
            .map(String::trim)
            .firstOrNull { it.isNotEmpty() && it.toLongOrNull() == null }

        for (id in candidateIds) {
            val resolved = runCatching {
                val sql = if (talkerHint != null) {
                    "SELECT type,content,talker,createTime,lvbuffer,msgId,msgSvrId,isSend FROM message WHERE msgSvrId = ? AND talker = ?"
                } else {
                    "SELECT type,content,talker,createTime,lvbuffer,msgId,msgSvrId,isSend FROM message WHERE msgSvrId = ?"
                }
                val bindArgs = if (talkerHint != null) {
                    arrayOf<Any>(id.toString(), talkerHint)
                } else {
                    arrayOf<Any>(id.toString())
                }
                WeDatabaseApi.rawQuery(sql, bindArgs).use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    MessageInfo(WeMessageApi.convertMsgInfoInstanceFromCursor(cursor))
                }
            }.getOrNull() ?: continue

            if (resolved.serverId == id && resolved.serverId > 0L && resolved.talker.isNotBlank() &&
                (talkerHint == null || resolved.talker == talkerHint)
            ) {
                return ResolvedRevokeTarget(
                    message = resolved,
                    keyValidated = talkerHint != null,
                )
            }
        }
        return null
    }

    override fun onDisable() {
        WeXmlParserApi.removeListener(this)
        WeChatMessageViewApi.removeListener(this)
        // unhookAll() 已由 disable() 先行调用，这里只复位标记，便于再次启用时重新挂载。
        revokeHandlerHooked = false
        // Styles already applied to on-screen rows would otherwise linger until each row happens to
        // rebind (or the chat is reopened), so they are cleared here.
        val rows = synchronized(dimmedContentViews) { dimmedContentViews.keys.toList() } +
            synchronized(badges) { badges.keys.toList() }
        rows.forEach { row -> row.post { clearRecallStyle(row) } }
    }

    override fun onParse(param: HookParam, result: MutableMap<String, Any?>) {
        val args = param.args
        val xmlContent = args[0] as? String ?: ""
        val rootTag = args[1] as? String ?: ""

        if (rootTag != "sysmsg" || !xmlContent.contains("revokemsg")) {
            return
        }

        if (result[TYPE_KEY] == "revokemsg") {
            val targetId = when (val rawTarget = result[".sysmsg.revokemsg.newmsgid"]) {
                is Number -> rawTarget.toLong()
                is String -> rawTarget.toLongOrNull()
                else -> null
            }?.takeIf { it > 0L } ?: run {
                WeLogger.w(TAG, "skipped revoke with invalid newmsgid")
                return
            }
            // Prefer the session embedded in the revoke sysmsg; scoping the lookup to that
            // chat removes the last way an unrelated row could be tagged as recalled.
            val sessionHint = (result[REVOKE_SESSION_KEY] as? String)?.trim().orEmpty()
            val cursor = if (sessionHint.isNotEmpty()) {
                WeDatabaseApi.rawQuery(
                    "SELECT type,content,talker,createTime,lvbuffer,msgId,msgSvrId,isSend FROM message WHERE msgSvrId = ? AND talker = ?",
                    arrayOf(targetId.toString(), sessionHint)
                )
            } else {
                WeDatabaseApi.rawQuery(
                    "SELECT type,content,talker,createTime,lvbuffer,msgId,msgSvrId,isSend FROM message WHERE msgSvrId = ?",
                    arrayOf(targetId.toString())
                )
            }

            cursor.use { cursor ->
                if (cursor.moveToFirst()) {
                    val msgInfo = MessageInfo(WeMessageApi.convertMsgInfoInstanceFromCursor(cursor))

                    // A revoke sysmsg always targets a real server-side message, so a row without a
                    // server id (0) or a talker means the lookup matched something unrelated. Marking
                    // it would light up "recalled" on every serverId==0 row of that chat — the classic
                    // false-badge the module used to show. Bail out instead of recording.
                    val talkerMismatch = sessionHint.isNotEmpty() && msgInfo.talker != sessionHint
                    if (msgInfo.serverId != targetId || msgInfo.serverId <= 0L || msgInfo.talker.isBlank() || talkerMismatch) {
                        WeLogger.w(
                            TAG,
                            "skipped revoke with unusable row: target=$targetId, talker=${msgInfo.talker}, svrId=${msgInfo.serverId}",
                        )
                        return@onParse
                    }

                    if (msgInfo.isSelfSender && !recallOutgoing) {
                        WeLogger.i(TAG, "sender is self and not recall outgoing, skipping")
                        return
                    }

                    result[TYPE_KEY] = null

                    val key = encodeKey(msgInfo.talker, msgInfo.serverId)
                    recordRecall(key)

                    WeLogger.i(TAG, "recorded message revoke: $key")
                    refreshBoundViews(msgInfo)
                }
            }
        }
    }

    // The recall usually arrives while the message row is on screen; restyle it immediately
    // instead of waiting for the next rebind.
    private fun refreshBoundViews(msgInfo: MessageInfo) {
        WeChatMessageViewApi.findBoundViews {
            it.talker == msgInfo.talker && it.serverId == msgInfo.serverId
        }.forEach { (view, boundMsgInfo) ->
            // onParse may run off the main thread; re-check the binding inside the posted block so
            // a view recycled in between is never styled for the wrong message.
            view.post {
                val current = WeChatMessageViewApi.getBoundMessage(view) ?: return@post
                if (current.instance === boundMsgInfo.instance) {
                    applyRecallStyle(view, current)
                }
            }
        }
    }

    override fun onCreateView(param: HookParam, view: View) {
        val msgInfo = WeChatMessageViewApi.getMsgInfoFromParam(param)
        clearRecallStyle(view)
        if (isRecalled(msgInfo)) {
            applyRecallStyle(view, msgInfo)
        }
    }

    private fun isRecalled(msgInfo: MessageInfo): Boolean =
        msgInfo.serverId > 0L &&
            msgInfo.talker.isNotBlank() &&
            encodeKey(msgInfo.talker, msgInfo.serverId) in recalledKeyCache

    // ── recalled-message styling ──────────────────────────────────────────────

    private fun applyRecallStyle(view: View, msgInfo: MessageInfo) {
        val contentView = findMessageContent(view) ?: return
        val previous = dimmedContentViews.put(view, contentView)
        if (previous != null && previous !== contentView) previous.alpha = 1f
        contentView.alpha = RECALL_ALPHA

        attachRecallBadge(view, msgInfo.isSelfSender, contentView)
    }

    private fun clearRecallStyle(view: View) {
        dimmedContentViews.remove(view)?.alpha = 1f
        removeRecallBadge(view)
    }

    // Every classic chat holder in the supported host range inherits getMainContainerView(), and
    // most holders override it with the semantic bubble/card container. Some Mvvm holders leave
    // the inherited clickArea null and attach their ItemDataTag to the dynamically-created content
    // view instead, so fall back to the first visible tagged view across the whole row. Searching
    // the whole row avoids depending on message-specific LinearLayout ordering while visibility
    // excludes hidden send-status views that can carry the same tag.
    private fun findMessageContent(view: View): View? {
        val holder = view.tag ?: return null
        val mainContainer = holder.reflekt()
            .firstMethod {
                name = "getMainContainerView"
                parameterCount = 0
                superclass()
            }
            .invoke() as? View
        return mainContainer ?: view.findViewWhich {
            it.isVisible && isTaggedBubbleView(it)
        }
    }

    private fun isTaggedBubbleView(view: View): Boolean =
        view.tag?.javaClass?.name?.startsWith("com.tencent.mm.ui.chatting.viewitems") == true

    private fun attachRecallBadge(row: View, isSelfSender: Boolean, anchor: View) {
        if (badges.containsKey(row)) return

        val rowGroup = row as? ViewGroup ?: return
        val context = row.context
        val dark = context.isDarkMode
        val iconColor = if (dark) badgeIconDark else badgeIconLight
        val circleColor = if (dark) badgeCircleDark else badgeCircleLight
        val badgeView: View = if (badgeType == BADGE_TYPE_TEXT) {
            TextView(context).apply {
                text = context.localizedChatString(R.string.chat_anti_recall_badge_text_content)
                textSize = 12f
                setTextColor(iconColor)
                includeFontPadding = false
                val hPad = BADGE_PADDING_DP.dpToPx(context)
                val vPad = (BADGE_PADDING_DP / 2).dpToPx(context)
                setPadding(hPad, vPad, hPad, vPad)
                background = GradientDrawable().apply {
                    cornerRadius = (BADGE_SIZE_DP / 2).dpToPx(context).toFloat()
                    setColor(circleColor)
                }
            }
        } else {
            RecallBadgeIconView(context, iconColor, circleColor)
        }
        // Not a child of the dimmed content row, so it must dim itself.
        badgeView.alpha = RECALL_ALPHA
        // Start hidden: the badge is added at the row's origin and only moved into place on the
        // first successful reconcile. If it were visible from the start it would flash at the
        // row's top-left corner ("mis-placed" badge) whenever the anchor is not ready yet.
        badgeView.visibility = View.INVISIBLE
        val size = BADGE_SIZE_DP.dpToPx(context)
        val params = if (badgeType == BADGE_TYPE_TEXT) {
            RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        } else {
            RelativeLayout.LayoutParams(size, size)
        }
        rowGroup.addView(badgeView, params)

        val preDrawListener = ViewTreeObserver.OnPreDrawListener {
            badges[row]?.let { positionRecallBadge(row, it) }
            true
        }
        badges[row] = RecallBadge(badgeView, isSelfSender, anchor, preDrawListener)
        row.viewTreeObserver.addOnPreDrawListener(preDrawListener)
    }

    private fun removeRecallBadge(row: View) {
        val badge = badges.remove(row) ?: return
        row.viewTreeObserver.removeOnPreDrawListener(badge.preDrawListener)
        (row as? ViewGroup)?.removeView(badge.view)
    }

    // The badge sits fully outside the bubble, bottom-aligned with it, clamped inside the row:
    // on the bubble's right for received messages, mirrored to the bubble's left for self-sent
    // ones (bubble and avatar are right-aligned there). Reconciled before every draw from the
    // anchor's absolute position — never from cached field offsets, because other features move
    // the content column within the row (avatar/name show-hide) without touching the anchor's
    // own coordinates. The per-frame cost is two matrix walks over preallocated arrays plus a
    // couple of float compares; the write only happens on an actual change, and the DFS
    // re-anchor only runs when the current anchor view went away.
    private fun positionRecallBadge(row: View, badge: RecallBadge) {
        val badgeView = badge.view
        // Whenever the badge cannot be anchored to a real bubble we hide it instead of leaving it
        // at its default (0, 0) translation, which is what made it appear stuck to the row top.
        if (!row.isAttachedToWindow || row.width == 0) {
            badgeView.visibility = View.INVISIBLE
            return
        }
        var anchor = badge.anchor
        if (!anchor.isAttachedToWindow || anchor.width == 0) {
            val resolved = findMessageContent(row)
            if (resolved == null) {
                badgeView.visibility = View.INVISIBLE
                return
            }
            anchor = resolved
            badge.anchor = anchor
        }
        val badgeWidth = badgeView.measuredWidth
        val badgeHeight = badgeView.measuredHeight
        if (badgeWidth == 0 || badgeHeight == 0) {
            badgeView.visibility = View.INVISIBLE
            return
        }

        row.getLocationOnScreen(badge.rowLocation)
        anchor.getLocationOnScreen(badge.anchorLocation)
        val anchorLeft = badge.anchorLocation[0] - badge.rowLocation[0]
        val anchorRight = anchorLeft + anchor.width
        val anchorBottom = badge.anchorLocation[1] - badge.rowLocation[1] + anchor.height
        val gap = BADGE_GAP_DP.dpToPx(row.context).toFloat()
        val rowRightLimit = (row.width - badgeWidth).toFloat()
        val targetX = if (badge.isSelfSender) {
            (anchorLeft - gap - badgeWidth).coerceIn(0f, rowRightLimit)
        } else {
            (anchorRight + gap).coerceIn(0f, rowRightLimit)
        }
        val targetY = (anchorBottom - badgeHeight)
            .toFloat()
            .coerceIn(0f, (row.height - badgeHeight).toFloat())
        badgeView.visibility = View.VISIBLE
        if (badgeView.translationX != targetX || badgeView.translationY != targetY) {
            badgeView.translationX = targetX
            badgeView.translationY = targetY
        }
    }

    // Same icon style as SwipeMessageOperations' swipe action icons: a tinted vector glyph on a
    // circular backdrop, in red to mark the message as deleted.
    @SuppressLint("AppCompatCustomView")
    private class RecallBadgeIconView(
        context: Context,
        iconColor: Int,
        circleColor: Int,
    ) : ImageView(context) {
        init {
            scaleType = ScaleType.CENTER_INSIDE
            // Tint a fresh copy so the shared icon singleton keeps its default color.
            val drawable = DeleteIcon.constantState?.newDrawable()?.mutate() ?: DeleteIcon.mutate()
            drawable.colorFilter = PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
            setImageDrawable(drawable)
            imageTintList = null
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(circleColor)
            }
            val pad = BADGE_PADDING_DP.dpToPx(context)
            setPadding(pad, pad, pad, pad)
        }
    }

    // ── legacy notice migration ────────────────────────────────────────────────

    private sealed interface MigrationState {
        data object Idle : MigrationState
        data object Running : MigrationState
        data class Done(val migrated: Int, val unmatched: Int) : MigrationState
        data object Failed : MigrationState
    }

    private data class LegacyNotice(val msgId: Long, val talker: String, val createTime: Long)

    /**
     * Finds the system-message rows rendered from the old notice template, records their original
     * messages into the new persisted list, and deletes the notice rows.
     */
    private fun migrateLegacyNotices(): Pair<Int, Int> {
        // The 提示格式 option was removed; the last stored template survives in MMKV and is what
        // old notices were rendered with. The MMKV key only exists if the user ever saved a custom
        // value (defaults are applied at read time), so fall back to the historical default.
        val template = KvStore.getString("recall_pattern")
            ?: $$"「$sender」尝试撤回上一条消息 (已阻止)"
        val regex = buildLegacyNoticeRegex(template)
            ?: throw IllegalStateException("invalid stored recall pattern")

        val notices = mutableListOf<LegacyNotice>()
        WeDatabaseApi.rawQuery(
            "SELECT msgId, talker, content, createTime FROM message WHERE type = ${MessageType.SYSTEM.code}",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val content = cursor.getString(2) ?: continue
                if (!regex.matches(content)) continue
                notices += LegacyNotice(
                    msgId = cursor.getLong(0),
                    talker = cursor.getString(1) ?: continue,
                    createTime = cursor.getLong(3),
                )
            }
        }

        var migrated = 0
        var unmatched = 0
        val newKeys = mutableSetOf<String>()
        for (notice in notices) {
            val originalServerId = findOriginalMessage(notice)
            if (originalServerId == null) {
                unmatched++
                continue
            }
            newKeys += encodeKey(notice.talker, originalServerId)
            WeDatabaseApi.delete("message", "msgId = ?", arrayOf(notice.msgId.toString()))
            migrated++
        }

        if (newKeys.isNotEmpty()) {
            synchronized(recordLock) {
                recalledKeyCache = boundedRecallKeys(recalledKeyCache + newKeys)
                recalledRecords = recalledKeyCache
            }
        }
        return migrated to unmatched
    }

    // Legacy notices were injected at original.createTime + 1 (createTime is in seconds), so the
    // original lives one second before the notice in the same chat; fall back to the nearest
    // earlier regular message when that exact second has no surviving row.
    @Suppress("DEPRECATION")
    private fun findOriginalMessage(notice: LegacyNotice): Long? {
        val systemTypes = listOf(
            MessageType.SYSTEM.code,
            MessageType.SYSTEM_NOTICE.code,
            MessageType.RECALL.code,
            MessageType.SYSTEM_LOCATION.code,
        ).joinToString(",")
        val beforeNotice = (notice.createTime - 1).toString()

        WeDatabaseApi.rawQuery(
            "SELECT msgSvrId FROM message WHERE talker = ? AND createTime = ? AND type NOT IN ($systemTypes) ORDER BY msgId DESC LIMIT 1",
            arrayOf(notice.talker, beforeNotice)
        ).use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0).takeIf { it != 0L }
        }
        WeDatabaseApi.rawQuery(
            "SELECT msgSvrId FROM message WHERE talker = ? AND createTime < ? AND type NOT IN ($systemTypes) ORDER BY createTime DESC, msgId DESC LIMIT 1",
            arrayOf(notice.talker, beforeNotice)
        ).use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0).takeIf { it != 0L }
        }
        return null
    }

    private fun buildLegacyNoticeRegex(template: String): Regex? {
        if (template.isBlank()) return null

        val placeholders = listOf($$"$sender", $$"$sendTime", $$"$recallTime", $$"$content")
        val builder = StringBuilder("^")
        var rest = template
        while (rest.isNotEmpty()) {
            val match = placeholders
                .mapNotNull { placeholder ->
                    rest.indexOf(placeholder).takeIf { it >= 0 }?.let { placeholder to it }
                }
                .minByOrNull { it.second }
            if (match == null) {
                builder.append(Regex.escape(rest))
                break
            }
            val (placeholder, index) = match
            if (index > 0) builder.append(Regex.escape(rest.substring(0, index)))
            builder.append(if (placeholder == $$"$content") """[\s\S]*""" else """[\s\S]+?""")
            rest = rest.substring(index + placeholder.length)
        }
        builder.append('$')

        return runCatching { Regex(builder.toString()) }.getOrNull()
    }

    // ── config dialog ──────────────────────────────────────────────────────────

    override fun onClick(context: ComponentActivity) {
        showComposeDialog(context) {
            var recallOutgoingInput by remember { mutableStateOf(recallOutgoing) }
            var badgeTypeInput by remember { mutableStateOf(badgeType) }
            var migrationState by remember { mutableStateOf<MigrationState>(MigrationState.Idle) }

            AlertDialogContent(
                    title = { Text(stringResource(R.string.feature_anti_message_recall_name)) },
                    text = {
                        SegmentedColumn(contentPadding = PaddingValues(0.dp)) {
                            item {
                                SwitchWidget(
                                    iconPlaceholder = false,
                                    title = stringResource(R.string.chat_anti_recall_outgoing),
                                    description = stringResource(R.string.chat_anti_recall_outgoing_description),
                                    checked = recallOutgoingInput,
                                    onCheckedChange = {
                                        recallOutgoingInput = it
                                        recallOutgoing = it
                                    },
                                )
                            }
                            item {
                                DropDownMenuWidget(
                                    iconPlaceholder = false,
                                    title = stringResource(R.string.chat_anti_recall_badge_type),
                                    description = null,
                                    value = badgeTypeInput,
                                    options = listOf(
                                        DropdownOption(BADGE_TYPE_ICON, stringResource(R.string.chat_anti_recall_badge_type_icon)),
                                        DropdownOption(BADGE_TYPE_TEXT, stringResource(R.string.chat_anti_recall_badge_type_text)),
                                    ),
                                    onValueChange = {
                                        badgeTypeInput = it
                                        badgeType = it
                                    },
                                )
                            }
                            item {
                                val migrating = migrationState == MigrationState.Running
                                BaseWidget(
                                    iconPlaceholder = false,
                                    title = stringResource(R.string.chat_anti_recall_migrate),
                                    description = when (val state = migrationState) {
                                        MigrationState.Idle -> stringResource(R.string.chat_anti_recall_migrate_description)
                                        MigrationState.Running -> stringResource(R.string.chat_anti_recall_migrating)
                                        is MigrationState.Done -> stringResource(
                                            R.string.chat_anti_recall_migrated,
                                            state.migrated,
                                            state.unmatched,
                                        )
                                        MigrationState.Failed -> stringResource(R.string.chat_anti_recall_migration_failed)
                                    },
                                    isError = migrationState is MigrationState.Failed,
                                    enabled = !migrating,
                                    onClick = {
                                        migrationState = MigrationState.Running
                                        CoroutineScope(Dispatchers.IO).launch {
                                            migrationState = runCatching { migrateLegacyNotices() }
                                                .fold(
                                                    onSuccess = { (migrated, unmatched) ->
                                                        MigrationState.Done(migrated, unmatched)
                                                    },
                                                    onFailure = {
                                                        WeLogger.e(TAG, "legacy notice migration failed", it)
                                                        MigrationState.Failed
                                                    },
                                                )
                                        }
                                    },
                                    trailingContent = {
                                        if (migrating) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                                        }
                                    },
                                )
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onDismiss) { Text(stringResource(R.string.dialog_close)) }
                    },
            )
        }
    }
}
