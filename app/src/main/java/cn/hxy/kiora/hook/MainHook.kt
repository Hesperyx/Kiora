package cn.hxy.kiora.hook

import cn.hxy.kiora.host.HostEnv
import android.util.Log
import androidx.core.content.edit
import cn.hxy.kiora.generated.HookRegistry
import cn.hxy.kiora.hook.base.BaseApiHookItem
import cn.hxy.kiora.hook.base.BaseClickableHookItem
import cn.hxy.kiora.hook.base.BaseHookItem
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.hook.base.Listener
import cn.hxy.kiora.loader.hookapi.HookEngineManager
import cn.hxy.kiora.plugin.MainPlugin
import cn.hxy.kiora.ui.pages.configs.ConfigUiRegistry
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.host.HostInfo

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
                if (it.shouldLoad()) it.loadHook()
            } catch (t: Throwable) {
                LogUtils.e(it, t)
            }
        }

    fun initAllConfigUI() {
        if (initialized) return
        initialized = true

        // 只注册当前宿主的项：设置页寄生在宿主进程内，装配的 adapter 就是当前
        // 宿主，不该把 QQ 的配置项摆到微信的设置页上。
        // 这里用 isInTargetHost() 而非 shouldLoad()：配置项是否注册与进程无关，
        // 只与宿主有关。
        clickableHookItemList
            .filter(BaseHookItem::isInTargetHost)
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
     *
     * 必须先按宿主收窄：本方法会**写** prefs（关掉组内多余项），若不过滤，
     * 在微信里会把 QQ 这些互斥项的开关写进 `Kiora_Config_<account>` ——
     * 而账号未就绪时那个文件名恰好与 QQ 侧一致，等于替 QQ 改配置。
     */
    private fun convergeExclusiveGroups() {
        switchHookItemList
            .filter(BaseHookItem::isInTargetHost)
            .filter { it.exclusiveGroup != null }
            .groupBy { it.exclusiveGroup }
            .forEach { (_, group) ->
                group.filter { it.isEnable }.drop(1).forEach { it.isEnable = false }
            }
    }

    fun processDataForCurrent(tag: String) = clickableHookItemList
        .filter(BaseHookItem::isInTargetHost)
        .filter(BaseSwitchHookItem::isAvailable)
        .forEach {
            when (tag) {
                "init" -> it.initData()
                "save" -> it.saveData()
            }
        }


    private fun hookAccountChange() {
        // 账号锚点由宿主适配器给出。微信当前返回 null —— 其账号体系与 QQ
        // 差异较大，尚未接通，此处整体跳过而不影响其余 hook。
        val anchor = HostInfo.adapter?.accountAnchor ?: return

        val ctor = anchor.resolveConstructor() ?: run {
            HookEngineManager.engine.log(
                Log.WARN,
                "[Kiora]",
                "账号锚点未命中: ${anchor.className}，账号切换逻辑跳过"
            )
            return
        }

        ctor.hookAfter {
            val account = HostInfo.adapter?.currentAccount ?: return@hookAfter
            HostEnv.globalPreference.edit {
                putString("currentUin", account)
            }
            processDataForCurrent("init")
            MainPlugin.initAllPluginForCurrent()
        }
    }

}