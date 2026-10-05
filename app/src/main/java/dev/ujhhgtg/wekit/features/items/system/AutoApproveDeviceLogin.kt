package dev.ujhhgtg.wekit.features.items.system

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tencent.mm.plugin.webwx.ui.ExtDeviceWXLoginUI
import dev.ujhhgtg.reflekt.reflekt
import dev.ujhhgtg.wekit.R
import dev.ujhhgtg.wekit.features.core.ClickableFeature
import dev.ujhhgtg.wekit.features.core.FeatureCategoryIds
import dev.ujhhgtg.wekit.data.KvStore.prefOption
import dev.ujhhgtg.wekit.ui.content.AlertDialogContent
import dev.ujhhgtg.wekit.ui.content.TextButton
import dev.ujhhgtg.wekit.ui.content.m3.SegmentedColumn
import dev.ujhhgtg.wekit.ui.content.m3.SwitchWidget
import dev.ujhhgtg.wekit.ui.utils.showComposeDialog

object AutoApproveDeviceLogin : ClickableFeature() {

    override val technicalId = "自动批准设备登录"
    override val nameRes = R.string.feature_auto_approve_device_login_name
    override val categoryIds = listOf(FeatureCategoryIds.SYSTEM_PRIVACY)
    override val descriptionRes = R.string.feature_auto_approve_device_login_description

    private const val AUTO_SYNC_MESSAGES = 0x1
    private const val SHOW_LOGIN_DEVICE = 0x2
    private const val AUTO_LOGIN_DEVICE = 0x4

    private var syncMessages by prefOption("auto_approve_device_login_sync", true)
    private var autoLoginDevice by prefOption("auto_approve_device_login_auto_login", true)

    override fun onEnable() {
        val targetClass = ExtDeviceWXLoginUI::class.java

        targetClass.hookBeforeOnCreate {
            val activity = thisObject as Activity
            var functionControl = 0
            if (syncMessages) functionControl = functionControl or AUTO_SYNC_MESSAGES
            if (autoLoginDevice) {
                functionControl = functionControl or SHOW_LOGIN_DEVICE
                functionControl = functionControl or AUTO_LOGIN_DEVICE
            }
            activity.intent.putExtra("intent.key.function.control", functionControl)
            activity.intent.putExtra("intent.key.need.show.privacy.agreement", false)
        }

        targetClass.reflekt().firstMethod { name = "initView" }.hookAfter {
            val instance = thisObject!!.reflekt()

            if (syncMessages) {
                instance.fields { type = CheckBox::class }
                    .mapNotNull { it.get() as CheckBox? }
                    .filter { it.isEffectivelyVisible() }
                    .forEach { it.isChecked = true }
            }

            val button = instance.firstField {
                type = Button::class
            }.get()!! as Button
            button.performClick()
        }
    }

    private fun View.isEffectivelyVisible(): Boolean {
        var view: View? = this
        while (view != null) {
            if (view.visibility != View.VISIBLE) return false
            view = view.parent as? View
        }
        return true
    }

    override fun onClick(context: ComponentActivity) {
        showComposeDialog(context) {
            var syncMessagesInput by remember { mutableStateOf(syncMessages) }
            var autoLoginDeviceInput by remember { mutableStateOf(autoLoginDevice) }

            AlertDialogContent(
                title = { Text(stringResource(R.string.feature_auto_approve_device_login_name)) },
                text = {
                    SegmentedColumn(contentPadding = PaddingValues(0.dp)) {
                        item {
                            SwitchWidget(
                                iconPlaceholder = false,
                                title = stringResource(R.string.system_auto_approve_sync),
                                description = stringResource(R.string.system_auto_approve_sync_summary),
                                checked = syncMessagesInput,
                                onCheckedChange = {
                                    syncMessagesInput = it
                                    syncMessages = it
                                },
                            )
                        }
                        item {
                            SwitchWidget(
                                iconPlaceholder = false,
                                title = stringResource(R.string.system_auto_approve_auto_login),
                                description = stringResource(R.string.system_auto_approve_auto_login_summary),
                                checked = autoLoginDeviceInput,
                                onCheckedChange = {
                                    autoLoginDeviceInput = it
                                    autoLoginDevice = it
                                },
                            )
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_close)) }
                },
            )
        }
    }
}
