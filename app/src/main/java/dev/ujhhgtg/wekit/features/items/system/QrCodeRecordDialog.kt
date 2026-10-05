package dev.ujhhgtg.wekit.features.items.system

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import dev.ujhhgtg.wekit.R
import dev.ujhhgtg.wekit.utils.android.copyToClipboard
import dev.ujhhgtg.wekit.utils.android.showToast
import java.text.DateFormat
import java.util.Date

fun showQrCodeRecordDialog(context: Context) {
    val records = QrCodeRecord.recordsSnapshot()

    if (records.isEmpty()) {
        AlertDialog.Builder(context)
            .setTitle(context.localizedSystemString(R.string.feature_qr_code_record_name))
            .setMessage(context.localizedSystemString(R.string.system_qr_code_record_empty))
            .setPositiveButton(android.R.string.ok, null)
            .show()
        return
    }

    val dateFormat = DateFormat.getDateTimeInstance()
    val labels = records.map { "${dateFormat.format(Date(it.time))} ${it.url}" }.toTypedArray()

    AlertDialog.Builder(context)
        .setTitle(context.localizedSystemString(R.string.feature_qr_code_record_name))
        .setItems(labels) { _, which ->
            val record = records[which]
            AlertDialog.Builder(context)
                .setMessage(record.url)
                .setPositiveButton(context.localizedSystemString(R.string.system_qr_code_record_open)) { _, _ ->
                    val activity = context as? Activity ?: return@setPositiveButton
                    runCatching { QrCodeRecord.openInWeChat(activity, record) }
                        .onFailure {
                            showToast(context, context.localizedSystemString(R.string.qr_code_record_open_failed))
                        }
                }
                .setNeutralButton(context.localizedSystemString(R.string.system_qr_code_record_copy)) { _, _ ->
                    copyToClipboard(context, record.url)
                    showToast(context, context.localizedSystemString(R.string.copied_to_clipboard))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        .setPositiveButton(context.localizedSystemString(R.string.system_qr_code_record_clear)) { _, _ ->
            AlertDialog.Builder(context)
                .setTitle(context.localizedSystemString(R.string.system_qr_code_record_clear))
                .setMessage(context.localizedSystemString(R.string.system_qr_code_record_clear_description))
                .setPositiveButton(context.localizedSystemString(R.string.system_qr_code_record_clear)) { _, _ ->
                    QrCodeRecord.clearAllRecords()
                    showToast(context, context.localizedSystemString(R.string.system_qr_code_record_cleared))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}