package cn.hxy.kiora.hook.social

import androidx.compose.runtime.Composable
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.common.ModuleScope
import cn.hxy.kiora.qq.conf.SparkConfig
import cn.hxy.kiora.hook.base.BaseClickableHookItem
import cn.hxy.kiora.qq.ui.AutoKeepSparkPage
import cn.hxy.kiora.qq.util.MsgTool
import cn.hxy.kiora.utils.scheduler.PrecisionScheduler
import cn.hxy.kiora.utils.scheduler.ScheduledTask
import java.util.Calendar

@HookItemAnnotation(
    "自动续火",
    "点击选择群聊和好友，支持脚本图文复合消息，可自定义具体执行时间",
    HookCategory.AUTO
)
object AutoKeepSpark : BaseClickableHookItem<SparkConfig>(SparkConfig.serializer()) {

    override val defaultConfig: SparkConfig = SparkConfig()

    private const val TASK_ID = "AutoKeepSpark_Task"

    private fun getNextTargetTime(): Long {
        val now = PrecisionScheduler.currentServerTime()
        val c = Calendar.getInstance()
        c.timeInMillis = now
        c.set(Calendar.HOUR_OF_DAY, config.hour)
        c.set(Calendar.MINUTE, config.minute)
        c.set(Calendar.SECOND, config.second)
        c.set(Calendar.MILLISECOND, 0)

        if (c.timeInMillis <= now) {
            c.add(Calendar.DAY_OF_YEAR, 1)
        }
        return c.timeInMillis
    }

    fun scheduleSend() {
        val targetTime = getNextTargetTime()
        val task = ScheduledTask(
            TASK_ID,
            targetTime
        ) {
            if (!isEnable) return@ScheduledTask
            sendMsg()
            scheduleSend()
        }
        PrecisionScheduler.addTask(task)
    }

    private fun sendMsg() {
        config.contacts.forEach { contactStr ->
            ModuleScope.launchIO(name) {
                val uin = contactStr.dropLast(4)
                val chatType = if (contactStr.endsWith("(好友)")) 1 else 2
                MsgTool.sendMsg(uin, config.message, chatType)
            }
        }
    }

    override fun onHook() {
        PrecisionScheduler.init()
        scheduleSend()
    }

    @Composable
    override fun ConfigContent(onDismiss: () -> Unit) {
        AutoKeepSparkPage(
            currentConfig = config,
            onSave = {
                updateConfig(it)
                scheduleSend()
            },
            onDismiss = onDismiss
        )
    }
}
