package cn.hxy.kiora.hook.device

import androidx.compose.runtime.Composable
import com.tencent.qmethod.pandoraex.monitor.DeviceInfoMonitor
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.conf.DeviceConfig
import cn.hxy.kiora.hook.base.BaseClickableHookItem
import cn.hxy.kiora.ui.pages.configs.CustomDevicePage
import cn.hxy.kiora.utils.hook.returnConstant

@HookItemAnnotation(
    "伪装设备在线状态",
    "点击设置机型，可用于设置在线状态机型（包含文字可能无效）",
    HookCategory.DEVICE,
    "All"
)
object CustomDevice : BaseClickableHookItem<DeviceConfig>(DeviceConfig.serializer()) {

    override val isNeedRestart: Boolean = true

    override val defaultConfig: DeviceConfig = DeviceConfig()

    override fun onHook() {
        DeviceInfoMonitor::class.java
            .getDeclaredMethod("getModel")
            .returnConstant(this, config.fakeModel)
    }

    @Composable
    override fun ConfigContent(onDismiss: () -> Unit) {
        CustomDevicePage(
            currentConfig = config,
            onSave = ::updateConfig,
            onDismiss = onDismiss
        )
    }
}
