package cn.hxy.kiora.hook.troop

import androidx.compose.runtime.Composable
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.common.ModuleScope
import cn.hxy.kiora.qq.conf.TroopSetConfig
import cn.hxy.kiora.hook.base.BaseClickableHookItem
import cn.hxy.kiora.qq.ui.TroopSelectorPage
import cn.hxy.kiora.qq.util.TroopTool
import cn.hxy.kiora.utils.scheduler.PrecisionScheduler
import cn.hxy.kiora.utils.scheduler.ScheduledTask

@HookItemAnnotation(
    "群打卡",
    "点击选择群聊",
    HookCategory.GROUP
)
object AutoGroupClockIn : BaseClickableHookItem<TroopSetConfig>(TroopSetConfig.serializer()) {

    override val defaultConfig: TroopSetConfig = TroopSetConfig()

    private fun scheduleClockIn() {
        val targetMidnight = PrecisionScheduler.getNextMidnight()

        val preciseTask = ScheduledTask(
            "DailyClockIn_Exact",
            targetMidnight
        ) {
            if (!isEnable) return@ScheduledTask
            doClockIn()
        }

        val backupTask = ScheduledTask(
            "DailyClockIn_Backup",
            targetMidnight + 500
        ) {
            if (!isEnable) return@ScheduledTask
            doClockIn()
            scheduleClockIn()
        }

        PrecisionScheduler.addTask(preciseTask)
        PrecisionScheduler.addTask(backupTask)
    }

    private fun doClockIn() {
        config.selectedSet.forEach {
            ModuleScope.launchIO(name) {
                TroopTool.clockIn(it)
            }
        }
    }

    override fun onHook() {
        PrecisionScheduler.init()
        scheduleClockIn()
    }

    @Composable
    override fun ConfigContent(onDismiss: () -> Unit) {
        TroopSelectorPage(
            title = "选择你要打卡的群聊",
            currentConfig = config,
            onSave = ::updateConfig,
            onDismiss = onDismiss
        )
    }
}
