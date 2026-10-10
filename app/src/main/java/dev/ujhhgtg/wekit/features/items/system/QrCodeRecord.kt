package dev.ujhhgtg.wekit.features.items.system

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import com.tencent.mm.ui.LauncherUI
import dev.ujhhgtg.wekit.R
import dev.ujhhgtg.wekit.data.KvStore
import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.features.api.ui.WeHomeScreenPopupMenuApi
import dev.ujhhgtg.wekit.features.core.ClickableFeature
import dev.ujhhgtg.wekit.features.core.FeatureCategoryIds
import dev.ujhhgtg.wekit.features.items.home_screen_menu.localizedHomeMenuString
import dev.ujhhgtg.wekit.ui.utils.QrCodeIcon
import dev.ujhhgtg.wekit.utils.HookParam
import dev.ujhhgtg.wekit.utils.HostInfo
import dev.ujhhgtg.wekit.utils.serialization.DefaultJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.luckypray.dexkit.DexKitBridge

object QrCodeRecord : ClickableFeature(), IResolveDex, WeHomeScreenPopupMenuApi.IMenuItemsProvider {

    override val technicalId = "二维码扫描记录"
    override val nameRes = R.string.feature_qr_code_record_name
    override val categoryIds = listOf(FeatureCategoryIds.SYSTEM_PRIVACY)
    override val descriptionRes = R.string.feature_qr_code_record_description

    private const val KEY_RECORDS = "qr_code_records"
    private const val EXTRA_REPLAY = "dev.ujhhgtg.wekit.qr_code_record_replay"

    @Serializable
    data class QrRecord(
        val url: String,
        val time: Long,
        val codeType: Int = 19,
        val codeVersion: Int = 0,
    )

    var showInHomeMenu by KvStore.prefOption("qr_code_record_home_menu_enabled", false)
    private var prefRecords: String?
        get() = KvStore.getString(KEY_RECORDS)
        set(value) = KvStore.putString(KEY_RECORDS, value)
    private val records = mutableListOf<QrRecord>()
    private var loaded = false

    override fun onEnable() {
        val codeTypeIndex = if (methodQBarString.method.parameterCount == 16) 6 else 5
        installHook("QrCodeRecord#1") {
            methodQBarString.hookBefore {
                if ((args[0] as Activity).intent.getBooleanExtra(EXTRA_REPLAY, false)) return@hookBefore
                val content = args[1] as String? ?: return@hookBefore
                record(content, args[codeTypeIndex] as Int, args[codeTypeIndex + 1] as Int)
            }
        }
        WeHomeScreenPopupMenuApi.addProvider(this)
    }

    override fun onDisable() {
        WeHomeScreenPopupMenuApi.removeProvider(this)
    }

    @Synchronized
    private fun record(content: String, codeType: Int, codeVersion: Int) {
        if (content.isEmpty()) return
        loadRecords()
        records.add(0, QrRecord(content, System.currentTimeMillis(), codeType, codeVersion))
        prefRecords = DefaultJson.encodeToString(records.toList())
    }

    override fun onClick(context: ComponentActivity) {
        showQrCodeRecordDialog(context)
    }

    override fun getMenuItems(param: HookParam): List<WeHomeScreenPopupMenuApi.MenuItem> {
        if (!showInHomeMenu) return emptyList()
        return listOf(
            WeHomeScreenPopupMenuApi.MenuItem(
                777031,
                localizedHomeMenuString(R.string.qr_code_record_home_menu_title),
                QrCodeIcon,
            ) {
                LauncherUI.getInstance()?.let { showQrCodeRecordDialog(it) }
            },
        )
    }

    fun openInWeChat(activity: Activity, record: QrRecord) {
        activity.startActivity(
            Intent().setClassName(
                HostInfo.packageName,
                "com.tencent.mm.plugin.webview.stub.WebviewScanImageActivity",
            )
                .putExtra("key_string_for_scan", record.url)
                .putExtra("key_codetype_for_scan", record.codeType)
                .putExtra("key_codeversion_for_scan", record.codeVersion)
                .putExtra(EXTRA_REPLAY, true),
        )
    }

    private fun loadRecords() {
        if (loaded) return
        prefRecords
            ?.let { runCatching { DefaultJson.decodeFromString<List<QrRecord>>(it) }.getOrNull() }
            ?.let { records.addAll(it) }
        loaded = true
    }

    @Synchronized
    fun recordsSnapshot(): List<QrRecord> {
        loadRecords()
        return records.toList()
    }

    @Synchronized
    fun clearAllRecords() {
        records.clear()
        loaded = true
        KvStore.remove(KEY_RECORDS)
    }

    val methodQBarString by dexMethod {
        matcher {
            usingEqStrings("MicroMsg.QBarStringHandler", "key_offline_scan_show_tips")
        }
    }

    override fun resolveDex(dexKit: DexKitBridge) {
    }
}