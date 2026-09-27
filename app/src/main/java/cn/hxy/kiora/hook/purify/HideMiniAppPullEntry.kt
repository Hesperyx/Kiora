package cn.hxy.kiora.hook.purify

import android.view.View
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.qq.HostInfo
import cn.hxy.kiora.utils.reflect.ClassUtils
import cn.hxy.kiora.utils.reflect.callMethod
import cn.hxy.kiora.utils.reflect.setObject
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * 隐藏下拉小程序（移植自 TCQT 的 HideMiniAppPullEntry）。
 *
 * 消息列表往下拉时，QQ 会在刷新动画上方露出小程序入口。这里把它藏掉，
 * 只留正常的刷新头。
 *
 * 与 TCQT 有一处刻意的差别：那边用一张 `[minVersion, maxVersionExclusive)` 规则表
 * 挑「二级刷新开关」的布尔字段名（t / I / E / D）；这里改成**把候选名挨个试** ——
 * 同一个对象上至多只有一个名字存在，失败的那几次无害。少一张要跟着 QQ 版本维护的
 * 映射表，将来改名也只需要往列表里加一项。
 *
 * TCQT 的告诫照搬：不要去 hook / 替换 Conversation 里的小程序初始化方法，
 * 它同时绑定了原生下拉刷新 listener，整段换掉会让刷新看得见却触发不了。
 */
@HookItemAnnotation(
    "隐藏下拉小程序",
    "去掉消息列表下拉时露出的小程序入口，保留正常刷新动画",
    HookCategory.PURIFY
)
object HideMiniAppPullEntry : BaseSwitchHookItem() {

    /** 已挂过回调的类，避免重复挂钩。 */
    private val hookedCallbackClasses = mutableSetOf<String>()

    override fun onInit(): Boolean {
        // TIM 没有这个入口
        if (HostInfo.isTIM) return false

        // 至少要摸到一个相关类，否则这个功能在当前宿主里没有落点
        val present = ALL_HOST_CLASSES.filter { ClassUtils.loadClassOrNull(it) != null }
        if (present.isEmpty()) {
            LogUtils.w("[$name] QQ ${HostInfo.versionCode} 下找不到任何相关类，功能不启用")
            return false
        }
        LogUtils.i("[$name] 命中 ${present.size}/${ALL_HOST_CLASSES.size} 个相关类")
        return true
    }

    override fun onHook() {
        hookRefreshPart()
        hookPullAlphaCallbacks()
        hookLegacyHeaders()
        hookHeaderBaseCallbacks()
    }

    /**
     * 新版路径：`MiniAppRefreshPart` 上返回二级刷新头的方法。
     *
     * 只取返回值、不替换方法体 —— 原生刷新的触发链路还挂在这个 header 上。
     */
    private fun hookRefreshPart() {
        val refreshPart = ClassUtils.loadClassOrNull(REFRESH_PART)
        if (refreshPart == null) {
            LogUtils.w("[$name] 找不到 $REFRESH_PART，只走旧版 header 路径")
            return
        }

        val targets = refreshPart.declaredMethods
            .filter { method ->
                !Modifier.isStatic(method.modifiers) &&
                        method.parameterTypes.isEmpty() &&
                        method.returnType.isTwoLevelHeader()
            }

        if (targets.isEmpty()) {
            LogUtils.w("[$name] $REFRESH_PART 上没有返回二级刷新头的方法，宿主结构可能变了")
            return
        }

        targets.forEach { method ->
            method.isAccessible = true
            method.hookAfter(this) { param ->
                val header = param.result ?: return@hookAfter
                disableTwoLevelSwitch(header)
                replaceMiniAppContainer(header)
            }
        }
    }

    /**
     * 下拉过程中 QQ 会把小程序视图的 alpha 压下去，这里拉回来。
     *
     * 不处理的话，容器藏掉后下拉会出现一段空白 —— 本该有动画的地方什么都没有。
     */
    private fun hookPullAlphaCallbacks() {
        val refreshPart = ClassUtils.loadClassOrNull(REFRESH_PART) ?: return
        if (!hookedCallbackClasses.add("alpha:${refreshPart.name}")) return

        refreshPart.declaredMethods
            .filter { method ->
                !Modifier.isStatic(method.modifiers) &&
                        method.returnType == Void.TYPE &&
                        method.parameterTypes.size == 4 &&
                        View::class.java.isAssignableFrom(method.parameterTypes[0]) &&
                        method.parameterTypes[1] == Float::class.javaPrimitiveType &&
                        method.parameterTypes[2] == Float::class.javaPrimitiveType &&
                        method.parameterTypes[3] == Float::class.javaPrimitiveType
            }
            .forEach { method ->
                method.isAccessible = true
                method.hookAfter(this) { param ->
                    val view = param.args.firstOrNull() as? View ?: return@hookAfter
                    keepVisible(view)
                    // 可见性后续还会被原生逻辑改回去，补一次
                    view.post { keepVisible(view) }
                }
            }
    }

    /** 旧版路径：小程序 header 自身的构造。 */
    private fun hookLegacyHeaders() {
        MINI_APP_HEADER_CLASSES
            .mapNotNull { ClassUtils.loadClassOrNull(it) }
            .forEach { headerClass ->
                headerClass.declaredConstructors.forEach { ctor ->
                    ctor.isAccessible = true
                    ctor.hookAfter(this) { param ->
                        disableTwoLevelSwitch(param.thisObject)
                        replaceMiniAppContainer(param.thisObject)
                    }
                }
                hookVisibilityCallbacks(headerClass)
            }
    }

    /** header 基类上的可见性回调：刷新状态一变就把小程序容器重新藏回去。 */
    private fun hookHeaderBaseCallbacks() {
        (MINI_APP_HEADER_BASE_CLASSES + MINI_APP_HEADER_CLASSES)
            .mapNotNull { ClassUtils.loadClassOrNull(it) }
            .forEach { hookVisibilityCallbacks(it) }
    }

    private fun hookVisibilityCallbacks(clazz: Class<*>) {
        if (!hookedCallbackClasses.add("visibility:${clazz.name}")) return

        clazz.declaredMethods
            .filter { it.isVisibilityCallback() }
            .forEach { method ->
                method.isAccessible = true
                method.hookAfter(this) { param ->
                    val header = param.thisObject
                    if (!header.isMiniAppHeader()) return@hookAfter
                    replaceMiniAppContainer(header)
                    (header as? View)?.post { replaceMiniAppContainer(header) }
                }
            }
    }

    /**
     * 关掉「二级刷新」开关。
     *
     * 这个布尔字段声明在 `TwoLevelHeader` 类里，不在 header 子类上 —— 而
     * [setObject] 只在该类**自身声明**的字段里找，不搜父类，所以 owner 必须指对层。
     * 旧版 QQ 没有 `TwoLevelHeader`，字段落在继承链某处，照 TCQT 的做法往上数三层。
     */
    private fun disableTwoLevelSwitch(header: Any) {
        val owner = ClassUtils.loadClassOrNull(TWO_LEVEL_HEADER)
            ?: header.javaClass.superclass?.superclass?.superclass
            ?: run {
                LogUtils.w("[$name] 定不到二级刷新开关的字段宿主类，开关没能关掉")
                return
            }

        val hit = TWO_LEVEL_SWITCH_FIELDS.filter { fieldName ->
            runCatching { header.setObject(fieldName, false, owner) }.isSuccess
        }

        if (hit.isEmpty()) {
            LogUtils.w(
                "[$name] 在 ${owner.simpleName} 上没找到候选开关字段 $TWO_LEVEL_SWITCH_FIELDS，" +
                        "宿主可能改了字段名"
            )
        }
    }

    /** 隐藏小程序容器，并确保真正的刷新头仍然可见。 */
    private fun replaceMiniAppContainer(header: Any) {
        val container = miniAppContainerOf(header)
        container?.let { setGone(it) }
        keepRefreshHeaderVisible(header, container)
    }

    /** 小程序容器的取值方法名在不同版本里是 `s` 或 `o`。 */
    private fun miniAppContainerOf(header: Any): Any? =
        runCatching { header.callMethod("s") }
            .recoverCatching { header.callMethod("o") }
            .getOrNull()

    /** 把二级刷新头里除小程序容器之外的 View 全部拉回可见，刷新动画才不会被一起藏掉。 */
    private fun keepRefreshHeaderVisible(header: Any, container: Any?) {
        // 正常情况字段声明在 TwoLevelHeader 上；旧版没这个类，就在 header 继承链前几层里找
        val layers = ClassUtils.loadClassOrNull(TWO_LEVEL_HEADER)?.let(::listOf)
            ?: header.javaClass.hierarchy().take(MAX_HEADER_LAYERS)

        layers.forEach { layer ->
            layer.declaredFields
                .filter { !Modifier.isStatic(it.modifiers) }
                .forEach { field ->
                    val view = runCatching {
                        field.isAccessible = true
                        field.get(header)?.callMethod("getView") as? View
                    }.getOrNull() ?: return@forEach
                    if (view !== container) keepVisible(view)
                }
        }
    }

    private fun keepVisible(view: View) {
        view.visibility = View.VISIBLE
        view.alpha = 1f
        // mutate 一下再改 alpha：背景 drawable 可能被多个 View 共用，
        // 直接改会连带影响别处
        view.background?.mutate()?.alpha = 255
    }

    private fun setGone(target: Any) {
        val view = target as? View
        if (view != null) {
            view.visibility = View.GONE
        } else {
            runCatching { target.callMethod("setVisibility", View.GONE) }
        }
    }

    private fun Any.isMiniAppHeader(): Boolean {
        val names = javaClass.hierarchy().map { it.name }
        return MINI_APP_HEADER_CLASSES.any { it in names }
    }

    /** 二级刷新头：类名固定或按后缀兜底，避免 QQ 换包名后整条链路静默失效。 */
    private fun Class<*>.isTwoLevelHeader(): Boolean =
        hierarchy().any {
            it.name == TWO_LEVEL_HEADER || it.simpleName.endsWith("TwoLevelHeader")
        }

    private fun Method.isVisibilityCallback(): Boolean {
        if (Modifier.isStatic(modifiers)) return false
        if (returnType != Void.TYPE) return false
        return isRefreshStateCallback() || isHeaderMovingCallback()
    }

    /** 刷新状态回调：`(?, RefreshState, RefreshState)`。 */
    private fun Method.isRefreshStateCallback(): Boolean =
        parameterTypes.size == 3 &&
                parameterTypes[1].name == REFRESH_STATE &&
                parameterTypes[2].name == REFRESH_STATE

    /** 下拉位移回调：布尔 + 三个 int + 一个 float，起始下标有两种排布。 */
    private fun Method.isHeaderMovingCallback(): Boolean =
        parameterTypes.matchesMovingTypes(0) || parameterTypes.matchesMovingTypes(1)

    private fun Array<Class<*>>.matchesMovingTypes(offset: Int): Boolean =
        size == offset + 5 &&
                this[offset] == Boolean::class.javaPrimitiveType &&
                this[offset + 1] == Float::class.javaPrimitiveType &&
                this[offset + 2] == Int::class.javaPrimitiveType &&
                this[offset + 3] == Int::class.javaPrimitiveType &&
                this[offset + 4] == Int::class.javaPrimitiveType

    private fun Class<*>.hierarchy(): List<Class<*>> {
        val result = mutableListOf<Class<*>>()
        var current: Class<*>? = this
        while (current != null && current != Any::class.java) {
            result.add(current)
            current = current.superclass
        }
        return result
    }

    private const val REFRESH_PART =
        "com.tencent.mobileqq.activity.home.chats.biz.MiniAppRefreshPart"

    private const val TWO_LEVEL_HEADER =
        "com.qqnt.widget.smartrefreshlayout.header.TwoLevelHeader"

    private const val REFRESH_STATE =
        "com.qqnt.widget.smartrefreshlayout.layout.constant.RefreshState"

    /** 「二级刷新」开关的字段名，按 QQ 版本由新到旧排列，挨个试即可。 */
    private val TWO_LEVEL_SWITCH_FIELDS = listOf("t", "I", "E", "D")

    /** 旧版没有 TwoLevelHeader 时，在 header 继承链上往下找几层字段。 */
    private const val MAX_HEADER_LAYERS = 6

    private val MINI_APP_HEADER_CLASSES = listOf(
        "com.tencent.qqnt.chats.view.MiniOldStyleHeaderNew",
        "com.tencent.qqnt.chats.view.MiniOldStyleHeader",
        "com.tencent.qqnt.chats.view.QQMiniProgramHeader",
        "com.tencent.qqnt.chats.view.MiniProgramHeader",
    )

    private val MINI_APP_HEADER_BASE_CLASSES = listOf(
        "com.tencent.qqnt.chats.view.QQChatListTwoLevelHeader",
        "com.tencent.qqnt.chats.view.ChatListTwoLevelHeader",
    )

    private val ALL_HOST_CLASSES =
        MINI_APP_HEADER_CLASSES + MINI_APP_HEADER_BASE_CLASSES + REFRESH_PART
}
