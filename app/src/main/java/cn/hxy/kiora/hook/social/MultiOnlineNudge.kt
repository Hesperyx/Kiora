package cn.hxy.kiora.hook.social

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.loader.hookapi.Chain
import cn.hxy.kiora.ui.components.atoms.DialogTextField
import cn.hxy.kiora.ui.components.dialogs.CenterDialogContainer
import cn.hxy.kiora.ui.core.compatibility.KioraCenterDialog
import cn.hxy.kiora.utils.hook.hookReplace
import cn.hxy.kiora.utils.hook.invokeOriginal
import cn.hxy.kiora.utils.hook.returnConstant
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.utils.qq.QQCurrentEnv
import cn.hxy.kiora.utils.reflect.findMethod
import cn.hxy.kiora.utils.reflect.toClass
import java.lang.reflect.Method

@HookItemAnnotation(
    "在线好友连拍",
    "点击好友在线列表拍一拍与私聊回拍时可输入次数",
    HookCategory.SOCIAL
)
object MultiOnlineNudge : BaseSwitchHookItem() {

    private lateinit var sendNudge: Method

    private lateinit var rateLimitCheck: Method

    override fun onInit(): Boolean {
        if (HostInfo.isTIM || (HostInfo.isQQ && HostInfo.versionCode < 14880)) return false
        val managerClass = "com.tencent.mobileqq.onlinestatus.proto.OnlineStatusNudgeManager".toClass

        sendNudge = managerClass.findMethod {
            returnType = void
            paramTypes(string, null, null)
        }

        rateLimitCheck = managerClass.findMethod {
            returnType = long
            paramTypes(string)
        }
        return super.onInit()
    }

    override fun onHook() {
        rateLimitCheck.returnConstant(this, 0L)
        sendNudge.hookReplace(this) {
            showDialog(it)
            return@hookReplace null
        }
    }

    private fun showDialog(param: Chain) {
        val activity = QQCurrentEnv.activity ?: return

        KioraCenterDialog(activity) { dismiss ->
            var count by remember { mutableStateOf("1") }

            CenterDialogContainer(title = "请输入次数", onDismiss = dismiss, onConfirm = {
                dismiss()
                val num = count.toIntOrNull() ?: 1
                repeat(times = num) { param.invokeOriginal() }
            }) {
                DialogTextField(
                    value = count,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isDigit() }
                        val num = filtered.toIntOrNull() ?: 0
                        count = when {
                            filtered.isEmpty() -> ""
                            num < 1 && filtered.isNotEmpty() -> "1"
                            else -> filtered
                        }
                    },
                    label = "拍一拍次数",
                    hint = "请输入发送次数",
                    modifier = Modifier,
                    keyboardType = KeyboardType.Number
                )
            }
        }.show()
    }
}