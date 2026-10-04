package cn.hxy.kiora.plugin.view

import cn.hxy.kiora.host.HostEnv
import android.annotation.SuppressLint
import com.tencent.mobileqq.activity.ScaleAIOActivity
import com.tencent.qqnt.aio.activity.AIODelegate
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.common.ModuleScope
import cn.hxy.kiora.hook.base.BaseApiHookItem
import cn.hxy.kiora.hook.base.Listener
import cn.hxy.kiora.plugin.loader.PluginManager
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.qq.util.FriendTool

@HookItemAnnotation("监听聊天界面")
object PluginViewLoader : BaseApiHookItem<Listener>() {

    data class PluginContact(
        val chatType: Int = 0,
        val peerUid: String = "",
        val peerUin: String = "",
        val guild: String = "",
        val peerName: String = ""
    )

    @SuppressLint("StaticFieldLeak")
    private var currentPluginView: PluginView? = null
    private var aioDelegate: AIODelegate? = null

    val currentContact: PluginContact
        get() = aioDelegate?.let {
            parseToPluginContact(it.aioContact.toString())
        } ?: PluginContact()

    override fun loadHook() {

        AIODelegate::class.java.getDeclaredMethod("show")
            .hookAfter(this) {
                aioDelegate = it.thisObject as AIODelegate

                if (currentContact.chatType != 0) {
                    hideView()
                    showView()
                }


            }
        AIODelegate::class.java.getDeclaredMethod("hide")
            .hookAfter(this) {
                if (HostEnv.activity !is ScaleAIOActivity) hideView()
            }

    }

    private fun parseToPluginContact(input: String): PluginContact {
        val regex = """(\w+)=([^,)]*)""".toRegex()
        val map = regex.findAll(input).associate {
            it.groupValues[1] to it.groupValues[2].trim('\'')
        }

        val chatType = map["chatType"]?.toIntOrNull() ?: 0
        val peerUid = map["peerUid"] ?: ""
        val guild = map["guildId"] ?: ""
        val peerName = map["nick"] ?: ""

        val peerUin = if (chatType == 2) {
            peerUid
        } else {
            FriendTool.getUinFromUid(peerUid).ifEmpty {
                HostEnv.activity?.intent?.getStringExtra("key_peerUin") ?: ""
            }
        }

        return PluginContact(
            chatType,
            peerUid,
            peerUin,
            guild,
            peerName
        )
    }

    private fun showView() {

        ModuleScope.launchMainDelayed(1) {
            val activity = HostEnv.activity ?: return@launchMainDelayed

            if (PluginManager.plugins.any { it.isRunning && it.compiler.menuItems.isNotEmpty() }) {
                currentPluginView = PluginView(activity)
                currentPluginView?.show()
            }
        }
    }

    private fun hideView() {
        currentPluginView?.dismiss()
        currentPluginView = null
    }

}