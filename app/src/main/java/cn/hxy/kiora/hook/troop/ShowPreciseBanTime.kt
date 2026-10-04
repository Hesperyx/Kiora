package cn.hxy.kiora.hook.troop

import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.reflect.findMethodOrNull
import cn.hxy.kiora.utils.reflect.toClass

/** 显示精准禁言时间，移植自 TCQT ShowPreciseBanTime。 */
@HookItemAnnotation(
    "显示精准禁言时间",
    "禁言状态下在聊天页文字输入框中显示精确的禁言时间（X天X时X分X秒），而非只显示<天，分，秒>",
    HookCategory.GROUP
)
object ShowPreciseBanTime : BaseSwitchHookItem() {

    override fun onHook() {
        "com.tencent.qqnt.troop.impl.TroopGagUtils".toClass
            .findMethodOrNull {
                name = "remainingTimeToStringCountDown"
                paramTypes(long)
            }
            ?.hookBefore(this) { param ->
                val time = param.args[0] as Long
                param.result = if (time <= 0) "0秒" else formatDuration(time)
            }
    }

    private fun formatDuration(seconds: Long): String {
        val days = seconds / (24 * 3600)
        val hours = (seconds % (24 * 3600)) / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return buildString {
            if (days > 0) append("${days}天")
            if (hours > 0) append("${hours}时")
            if (minutes > 0) append("${minutes}分")
            if (secs > 0 || isEmpty()) append("${secs}秒")
        }
    }
}
