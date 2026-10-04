package cn.hxy.kiora.hook.misc

import android.os.Bundle
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.reflect.ClassUtils
import cn.hxy.kiora.utils.reflect.findMethodOrNull

/**
 * 修改小程序分享行为（移植自 TCQT 的 MiniAppShare）。
 *
 * 小程序拉起分享面板后，用户点「取消」也让它收到分享成功的回调，
 * 免得小程序卡在「分享后领取奖励」那一页反复让用户再分享一次。
 *
 * 两条链路都要堵：
 *
 * 1. 小程序进程通过 EIPC 回传结果，失败指令在 `callServer` 里被改写成成功指令；
 * 2. 宿主自身的转发回调 `endForwardCallback` 收尾时，把「用户已分享」的布尔量强制为 true。
 *
 * 全部走反射加载：这两条链路在 TIM 上未必存在，类找不到时只降级、不崩。
 */
@HookItemAnnotation(
    "修改小程序分享行为",
    "在小程序里取消分享时，也让它收到分享成功的回调",
    HookCategory.MISC,
    "All"
)
object MiniAppShareBehavior : BaseSwitchHookItem() {

    override fun onInit(): Boolean {
        val hasEipc = ClassUtils.loadClassOrNull(EIPC_CLIENT) != null
        val hasForward = ClassUtils.loadClassOrNull(FORWARD_BASE_OPTION) != null

        if (!hasEipc && !hasForward) {
            LogUtils.w("[$name] 宿主里既没有 ${EIPC_CLIENT} 也没有 ${FORWARD_BASE_OPTION}，功能不启用")
            return false
        }
        if (!hasEipc || !hasForward) {
            LogUtils.w("[$name] 只找到其中一条链路（eipc=$hasEipc, forward=$hasForward），按能挂上的装")
        }
        return true
    }

    override fun onHook() {
        hookEipcShareResult()
        hookForwardCallback()
    }

    /** 小程序 → 宿主的 IPC 回传：把失败指令改写成成功指令。 */
    private fun hookEipcShareResult() {
        val clientClass = ClassUtils.loadClassOrNull(EIPC_CLIENT) ?: run {
            LogUtils.w("[$name] 找不到 ${EIPC_CLIENT}，跳过 IPC 链路")
            return
        }

        // 第 4 个参数是回调接口，类名在各版本里不变但类型不适合写死，交给 paramTypes 的 null 兜底
        val callServer = clientClass.findMethodOrNull {
            name = "callServer"
            paramTypes(String::class.java, String::class.java, Bundle::class.java, null)
        } ?: run {
            LogUtils.w("[$name] ${clientClass.simpleName} 上没有 callServer(String, String, Bundle, ?)")
            return
        }

        callServer.hookBefore(this) { param ->
            if (param.args.getOrNull(0) as? String != MINI_IPC_SERVER) return@hookBefore

            when (param.args.getOrNull(1) as? String) {
                CMD_SHARE_FAIL -> param.args[1] = CMD_SHARE_SUCCESS

                CMD_REPORT_EVENT -> (param.args.getOrNull(2) as? Bundle)
                    ?.takeIf { it.getString(KEY_REPORT_RESERVE) == EVENT_FAIL }
                    ?.putString(KEY_REPORT_RESERVE, EVENT_SUCCESS)
            }
        }
    }

    /** 宿主自身的转发收尾：把「已分享」的布尔量强制为 true。 */
    private fun hookForwardCallback() {
        val forwardClass = ClassUtils.loadClassOrNull(FORWARD_BASE_OPTION) ?: run {
            LogUtils.w("[$name] 找不到 ${FORWARD_BASE_OPTION}，跳过转发链路")
            return
        }

        val endForward = forwardClass.findMethodOrNull {
            name = "endForwardCallback"
            paramTypes(boolean)
        } ?: run {
            LogUtils.w("[$name] ${forwardClass.simpleName} 上没有 endForwardCallback(boolean)")
            return
        }

        endForward.hookBefore(this) { param ->
            param.args[0] = true
        }
    }

    private const val EIPC_CLIENT = "eipc.EIPCClient"
    private const val FORWARD_BASE_OPTION = "com.tencent.mobileqq.forward.ForwardBaseOption"

    private const val MINI_IPC_SERVER = "MiniMsgIPCServer"
    private const val CMD_SHARE_FAIL = "cmd_mini_share_fail"
    private const val CMD_SHARE_SUCCESS = "cmd_mini_share_suc"
    private const val CMD_REPORT_EVENT = "cmd_mini_report_event"
    private const val KEY_REPORT_RESERVE = "key_mini_report_event_reserves2"
    private const val EVENT_FAIL = "fail"
    private const val EVENT_SUCCESS = "success"
}
