package dev.ujhhgtg.wekit.features.items.entertain

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import dev.ujhhgtg.wekit.R
import dev.ujhhgtg.wekit.features.api.net.WePacketHelper
import dev.ujhhgtg.wekit.features.api.net.models.protobuf.ModProfileProto
import dev.ujhhgtg.wekit.features.api.net.models.protobuf.OpLog
import dev.ujhhgtg.wekit.features.api.net.models.protobuf.OpLogRespProto
import dev.ujhhgtg.wekit.features.core.ClickableFeature
import dev.ujhhgtg.wekit.features.core.FeatureCategoryIds
import dev.ujhhgtg.wekit.ui.content.AlertDialogContent
import dev.ujhhgtg.wekit.ui.content.Button
import dev.ujhhgtg.wekit.ui.content.TextButton
import dev.ujhhgtg.wekit.ui.utils.showComposeDialog
import dev.ujhhgtg.wekit.utils.WeLogger

object ClearProfileDetails : ClickableFeature() {

    override val technicalId = "清空资料信息"
    override val nameRes = R.string.feature_clear_profile_details_name
    override val categoryIds = listOf(FeatureCategoryIds.ENTERTAIN)
    override val descriptionRes = R.string.feature_clear_profile_details_description

    private const val TAG = "ClearProfileDetails"

    override fun onClick(context: ComponentActivity) {
        showComposeDialog(context) {
            AlertDialogContent(
                title = { Text(stringResource(R.string.feature_clear_profile_details_name)) },
                text = { Text(stringResource(R.string.clear_profile_details_confirmation)) },
                dismissButton = {
                    TextButton(onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
                },
                confirmButton = {
                    Button(onClick = {
                        val reqBytes = OpLog.encodeSingle(
                            OpLog.CMD_MOD_PROFILE,
                            ModProfileProto()
                        )

                        WePacketHelper.sendCgi(
                            "/cgi-bin/micromsg-bin/oplog",
                            681, 0, 0,
                            reqBytes = reqBytes
                        ) {
                            onSuccess { bytes ->
                                val resp = bytes?.let { OpLogRespProto.decode(it) }
                                WeLogger.i(TAG, "success: ret=${resp?.ret}")
                                showComposeDialog(context) {
                                    AlertDialogContent(
                                        title = {
                                            Text(stringResource(R.string.clear_profile_details_send_success))
                                        },
                                        text = {
                                            Text(
                                                stringResource(
                                                    R.string.clear_profile_details_server_response_code,
                                                    resp?.ret?.toString()
                                                        ?: stringResource(R.string.unknown),
                                                )
                                            )
                                        },
                                        confirmButton = {
                                            TextButton(onClick = onDismiss) {
                                                Text(stringResource(R.string.dialog_close))
                                            }
                                        }
                                    )
                                }
                            }

                            onFailure { type, code, msg ->
                                showComposeDialog(context) {
                                    AlertDialogContent(
                                        title = {
                                            Text(stringResource(R.string.clear_profile_details_send_failure))
                                        },
                                        text = {
                                            Text(
                                                stringResource(
                                                    R.string.clear_profile_details_send_failure_details,
                                                    type.toString(),
                                                    code.toString(),
                                                    msg,
                                                )
                                            )
                                        },
                                        confirmButton = {
                                            TextButton(onClick = onDismiss) {
                                                Text(stringResource(R.string.dialog_close))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        onDismiss()
                    }) { Text(stringResource(R.string.dialog_confirm)) }
                })
        }
    }

    override val noSwitchWidget: Boolean
        get() = true
}
