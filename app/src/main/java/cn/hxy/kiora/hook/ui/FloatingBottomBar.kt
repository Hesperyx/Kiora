package cn.hxy.kiora.hook.ui

import android.app.Activity
import android.app.Instrumentation
import android.view.View
import androidx.compose.runtime.Composable
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.qq.conf.FloatingBarConfig
import cn.hxy.kiora.hook.base.BaseClickableHookItem
import cn.hxy.kiora.hook.base.ExclusiveGroup
import cn.hxy.kiora.hook.ui.liquidglass.BottomBarImplementation
import cn.hxy.kiora.hook.ui.liquidglass.FloatingBottomBarConfigStore
import cn.hxy.kiora.hook.ui.liquidglass.GlassBarInstaller
import cn.hxy.kiora.hook.ui.liquidglass.NewViewBarInstaller
import cn.hxy.kiora.hook.ui.liquidglass.QQTabLocator
import cn.hxy.kiora.qq.ui.FloatingBottomBarPage
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.reflect.ClassUtils

/**
 * 悬浮底栏（自 TCQT 移植）。
 *
 * 与「液态玻璃导航栏」是两套独立实现：这里走「布局手术」，把宿主原生底栏整体
 * 搬入悬浮玻璃容器，原生红点 / 未读数 / 长按菜单全部保留。两者都要接管底栏，
 * 因此归入同一个 [ExclusiveGroup.HOST_BOTTOM_BAR]，同一时刻只能启用一个。
 */
@HookItemAnnotation(
    "悬浮底栏",
    "把 QQ 原生底部导航栏搬入悬浮玻璃容器，保留原生红点与长按菜单",
    HookCategory.APPEARANCE
)
object FloatingBottomBar : BaseClickableHookItem<FloatingBarConfig>(FloatingBarConfig.serializer()) {

    override val defaultConfig: FloatingBarConfig = FloatingBarConfig()

    override val isNeedRestart = true

    override val exclusiveGroup: String? = ExclusiveGroup.HOST_BOTTOM_BAR

    override fun onInit(): Boolean {
        // 与「液态玻璃导航栏」的互斥由 exclusiveGroup 承担：设置页切换时自动关掉对侧，
        // 注册钩子前 [MainHook] 还会把组内收敛到只剩一个。
        // 这里不再自己判断对侧开关 —— 那样一开液态玻璃，本项就被算成「不可用」，
        // 设置页会误报一个其实只是没启用的功能。

        // 至少要有一套已知的宿主底栏实现存在，否则本功能没有落点。
        return QQTabLocator.tabViewClasses.any { ClassUtils.loadClassOrNull(it) != null }
    }

    override fun initData() {
        super.initData()
        FloatingBottomBarConfigStore.update(config)
    }

    override fun onHook() {
        hookTabSwitch()
        hookActivityResume()
    }

    /**
     * 挂钩底栏的切换方法。
     *
     * 只挂钩底栏类自身声明的方法：挂到基类会波及进程内所有同类控件。
     * 新旧两套底栏并存于同一安装包，通常只有一套实际存在，缺失的一套跳过。
     */
    private fun hookTabSwitch() {
        QQTabLocator.tabViewClasses.forEach { className ->
            val cls = ClassUtils.loadClassOrNull(className) ?: return@forEach
            val method = runCatching {
                cls.getDeclaredMethod(QQTabLocator.SWITCH_METHOD, Int::class.javaPrimitiveType)
            }.getOrNull() ?: return@forEach

            method.hookBefore(this) { param ->
                val index = param.args.getOrNull(0) as? Int ?: return@hookBefore
                QQTabLocator.armSmoothTarget(index)
            }
            method.hookAfter(this) { param ->
                QQTabLocator.clearSmoothTarget()
                val view = param.thisObject as? View ?: return@hookAfter
                val index = param.args.getOrNull(0) as? Int ?: return@hookAfter
                if (FloatingBottomBarConfigStore.read().implementation == BottomBarImplementation.NEW_VIEW) {
                    NewViewBarInstaller.onTabChanged(view, index)
                } else {
                    GlassBarInstaller.onTabChanged(view, index)
                }
            }
        }
    }

    /**
     * 挂钩 Activity 恢复回调：主界面异步构建，底栏可能数秒后才出现，
     * 此处触发限时轮询兜底；底栏切换钩子才是首选的安装触发点。
     */
    private fun hookActivityResume() {
        runCatching {
            Instrumentation::class.java
                .getMethod("callActivityOnResume", Activity::class.java)
                .hookAfter(this) { param ->
                    val activity = param.args.getOrNull(0) as? Activity ?: return@hookAfter
                    if (activity.javaClass.name == QQTabLocator.LAUNCHER_ACTIVITY) {
                        if (FloatingBottomBarConfigStore.read().implementation == BottomBarImplementation.NEW_VIEW) {
                            NewViewBarInstaller.scheduleInstall(activity)
                        } else {
                            GlassBarInstaller.scheduleInstall(activity)
                        }
                    }
                }
        }.onFailure { LogUtils.e(this, it) }
    }

    @Composable
    override fun ConfigContent(onDismiss: () -> Unit) {
        FloatingBottomBarPage(
            currentConfig = config,
            onSave = {
                updateConfig(it)
                FloatingBottomBarConfigStore.update(it)
            },
            onDismiss = onDismiss
        )
    }
}
