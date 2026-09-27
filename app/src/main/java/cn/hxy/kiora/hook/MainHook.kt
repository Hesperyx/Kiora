package cn.hxy.kiora.hook

import androidx.core.content.edit
import cn.hxy.kiora.generated.HookRegistry
import cn.hxy.kiora.hook.base.BaseApiHookItem
import cn.hxy.kiora.hook.base.BaseClickableHookItem
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.hook.base.Listener
import cn.hxy.kiora.plugin.MainPlugin
import cn.hxy.kiora.ui.pages.configs.ConfigUiRegistry
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.qq.QQCurrentEnv
import cn.hxy.kiora.utils.reflect.toClass

object MainHook {

    private var initialized = false
    private val allHookItem = HookRegistry.hookItems
    private val apiHookItemList =
        allHookItem.filterIsInstance<BaseApiHookItem<Listener>>()
    val switchHookItemList =
        allHookItem.filterIsInstance<BaseSwitchHookItem>()
    val clickableHookItemList =
        switchHookItemList.filterIsInstance<BaseClickableHookItem<*>>()


    fun loadHook() {

        loadApiHook()
        initSwitchHookItem()
        hookAccountChange()


    }

    private fun loadApiHook() = apiHookItemList
        .forEach {
            try {
                if (it.isInTargetProcess()) it.loadHook()
            } catch (t: Throwable) {
                LogUtils.e(it, t)
            }
        }

    fun initAllConfigUI() {
        if (initialized) return
        initialized = true

        clickableHookItemList
            .forEach { item ->
                ConfigUiRegistry.register(item.name) { onDismiss ->
                    item.ConfigContent(onDismiss)
                }
            }
    }

    private fun initSwitchHookItem() {
        convergeExclusiveGroups()
        switchHookItemList.forEach(BaseSwitchHookItem::init)
    }

    /**
     * 互斥组收敛：每组最多保留一个开启项。
     *
     * 设置页切换时会自动关掉同组的对侧，但配置还可能来自旧版本备份、
     * 被手工改写的 prefs，或跨版本的功能调整。这里在注册钩子前再兜一次底，
     * 免得两个实现同时认领宿主底栏、互相拆台。
     */
    private fun convergeExclusiveGroups() {
        switchHookItemList
            .filter { it.exclusiveGroup != null }
            .groupBy { it.exclusiveGroup }
            .forEach { (_, group) ->
                group.filter { it.isEnable }.drop(1).forEach { it.isEnable = false }
            }
    }

    fun processDataForCurrent(tag: String) = clickableHookItemList
        .filter(BaseSwitchHookItem::isAvailable)
        .forEach {
            when (tag) {
                "init" -> it.initData()
                "save" -> it.saveData()
            }
        }


    private fun hookAccountChange() {

        "com.tencent.imcore.message.BaseQQMessageFacade".toClass
            .declaredConstructors.first()
            .hookAfter {
                QQCurrentEnv.globalPreference.edit {
                    putString(
                        "currentUin",
                        QQCurrentEnv.currentUin
                    )
                }
                processDataForCurrent("init")
                MainPlugin.initAllPluginForCurrent()

            }
    }

}