package me.hd.wauxv.hook

import androidx.annotation.Keep
import cn.hxy.kiora.loader.hookapi.IHookBridge

@Keep
class HookHandle(val unhook: IHookBridge.MemberUnhookHandle) {
    override fun toString(): String {
        return "HookHandle(delegate=${unhook.member})"
    }
}