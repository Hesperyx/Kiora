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


}