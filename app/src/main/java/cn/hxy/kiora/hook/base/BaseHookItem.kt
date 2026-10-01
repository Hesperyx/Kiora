package cn.hxy.kiora.hook.base

import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.utils.qq.HostInfo
import cn.hxy.kiora.utils.reflect.TAG

abstract class BaseHookItem {

    val name: String = TAG

    open var isEnable = true

    /**
     * 互斥分组标识，取值见 [ExclusiveGroup]。
     *
     * 同组内最多只允许开启一项：开启新的会自动关掉组内其它项。
     * 用于多个功能都要认领同一个宿主控件的场景 —— 同时生效会互相拆台。
     * 默认 null 表示不参与互斥。
     */
    open val exclusiveGroup: String? = null

    protected val annotation: HookItemAnnotation? by lazy {
        this::class.java.getAnnotation(HookItemAnnotation::class.java)
    }

    fun isInTargetProcess(): Boolean {
        val target = annotation?.process ?: return false
        if (target == "All") return true
        val currentProcess = HostInfo.processName
        return currentProcess == "${HostInfo.packageName}$target"
    }

    /**
     * 本 hook 是否应在当前宿主生效。
     *
     * 依据 [HookItemAnnotation.hosts] 判定：注解缺失或 hosts 为空视为不限宿主。
     * 适配器未装配时返回 false —— 宁可不加载，也不要误跑。
     *
     * 之所以需要这道门禁：`process` 为空串时 [isInTargetProcess] 会退化成
     * 「主进程即命中」，若宿主换成微信，存量 QQ hook 会全部尝试执行。
     */
    fun isInTargetHost(): Boolean {
        val hosts = annotation?.hosts ?: return true
        if (hosts.isEmpty()) return true
        return HostInfo.adapter?.matchesTag(hosts) ?: false
    }

    /** 加载门禁：宿主与进程须同时命中。所有加载路径都应走这里。 */
    fun shouldLoad(): Boolean = isInTargetHost() && isInTargetProcess()
}