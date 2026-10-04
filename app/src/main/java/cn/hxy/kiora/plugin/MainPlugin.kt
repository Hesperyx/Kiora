package cn.hxy.kiora.plugin

import cn.hxy.kiora.plugin.loader.PluginManager

object MainPlugin {
    fun initAllPluginForCurrent() {
        PluginManager.stopAllPlugins()
        PluginManager.plugins.clear()
        PluginManager.loadAll()
        PluginManager.autoStart()
    }

}