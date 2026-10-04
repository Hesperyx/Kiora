package cn.hxy.kiora.hook.patch

import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.doNothing
import cn.hxy.kiora.utils.reflect.clazz
import cn.hxy.kiora.utils.reflect.findMethod
import cn.hxy.kiora.utils.reflect.toClass

@HookItemAnnotation(
    "屏蔽热更新",
    "禁用宿主云控热补丁/热更新",
    HookCategory.OTHER,
    "All"
)
object DisableHotPatch : BaseSwitchHookItem() {

    override val isNeedRestart: Boolean = true

    override fun onHook() {

        "com.tencent.rfix.lib.download.PatchDownloadTask".clazz
            ?.getDeclaredMethod("run")
            ?.doNothing(this)

        val configClass = "com.tencent.rfix.lib.config.PatchConfig".toClass
        "com.tencent.rfix.lib.engine.PatchEngineBase".clazz
            ?.findMethod {
                returnType = void
                paramTypes(String::class.java, configClass)
            }
            ?.doNothing(this)

        "com.tencent.mobileqq.msf.core.net.patch.PatchReporter".clazz
            ?.declaredMethods
            ?.filter {
                it.name.startsWith("report") && it.returnType == Void.TYPE
            }
            ?.forEach { it.doNothing(this) }


    }


}