package cn.hxy.kiora.hook.api

import com.tencent.mobileqq.troop.api.impl.TroopMemberInfoServiceImpl
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseApiHookItem
import cn.hxy.kiora.hook.base.Listener
import cn.hxy.kiora.utils.hook.hookAfter

@HookItemAnnotation("监听用户退群")
object OnTroopQuit : BaseApiHookItem<TroopQuitListener>() {
    override fun loadHook() {

        TroopMemberInfoServiceImpl::class.java
            .getDeclaredMethod(
                "deleteTroopMember",
                String::class.java,
                String::class.java,
                Boolean::class.javaPrimitiveType
            )
            .hookAfter(this) { param ->
                val troopUin = param.args[0] as String
                val memberUin = param.args[1] as String
                forEachChecked { it.onQuit(troopUin, memberUin) }
            }


    }
}

fun interface TroopQuitListener : Listener {
    fun onQuit(troopUin: String, memberUin: String)
}