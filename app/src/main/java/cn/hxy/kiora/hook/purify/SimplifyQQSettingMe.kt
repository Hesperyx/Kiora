package cn.hxy.kiora.hook.purify

import androidx.compose.runtime.Composable
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.qq.conf.SimplifyDrawerConfig
import cn.hxy.kiora.hook.base.BaseClickableHookItem
import cn.hxy.kiora.qq.ui.SimplifyDrawerPage
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.reflect.findFieldOrNull
import cn.hxy.kiora.utils.reflect.findMethodOrNull
import cn.hxy.kiora.utils.reflect.getObjectOrNull
import cn.hxy.kiora.utils.reflect.toClass

/** 侧滑栏精简，移植自 TCQT SimplifyQQSettingMe。 */
@HookItemAnnotation(
    "侧滑栏精简",
    "对侧滑栏功能入口进行精简隐藏（相册/收藏/文件/钱包/会员中心/个性装扮/免流量）",
    HookCategory.PURIFY
)
object SimplifyQQSettingMe : BaseClickableHookItem<SimplifyDrawerConfig>(SimplifyDrawerConfig.serializer()) {

    override val isNeedRestart: Boolean = true

    override val defaultConfig: SimplifyDrawerConfig = SimplifyDrawerConfig()

    override fun onHook() {
        val clazz = "com.tencent.mobileqq.parts.QQSettingMeMenuPanelPartV3".toClass

        clazz.findMethodOrNull {
            name = "onInitView"
        }?.hookAfter(this) { param ->
            val selectedItemIds = config.hiddenItems
            if (selectedItemIds.isEmpty()) return@hookAfter

            val obj = param.thisObject

            @Suppress("UNCHECKED_CAST")
            val bizDataList = clazz.findFieldOrNull { type = ArrayList::class.java }
                ?.get(obj) as? ArrayList<Any> ?: return@hookAfter

            // 移除选中条目后，还必须把列表回灌给 adapter 才会触发 DiffUtil 重渲染
            bizDataList.removeAll { item ->
                val bean = item.getObjectOrNull("a") ?: return@removeAll false
                needRemove(bean, selectedItemIds)
            }

            val listItemAdapter = clazz.declaredFields
                .firstOrNull { it.type.name.contains("adapter") }
                ?.apply { isAccessible = true }
                ?.get(obj) ?: return@hookAfter

            "com.tencent.biz.richframework.part.adapter.AsyncListDifferDelegationAdapter".toClass
                .findMethodOrNull {
                    name = "setItems"
                    paramCount = 1
                }
                ?.invoke(listItemAdapter, bizDataList)
        }
    }

    private fun needRemove(bean: Any, selectedItemIds: Set<String>): Boolean =
        bean::class.java.declaredFields
            .filter { it.type == String::class.java }
            .any { f ->
                f.isAccessible = true
                f.get(bean) as? String in selectedItemIds
            }

    @Composable
    override fun ConfigContent(onDismiss: () -> Unit) {
        SimplifyDrawerPage(
            currentConfig = config,
            onSave = ::updateConfig,
            onDismiss = onDismiss
        )
    }
}
