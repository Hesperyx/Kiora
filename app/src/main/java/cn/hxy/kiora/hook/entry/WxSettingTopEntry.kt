package cn.hxy.kiora.hook.entry

import android.content.Context
import android.content.Intent
import android.view.View
import cn.hxy.kiora.activity.SettingActivity
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseApiHookItem
import cn.hxy.kiora.hook.base.Listener
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.hook.hookReplace
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.wx.WeChatDexKit
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 微信设置页**最上方**的 Kiora 入口。
 *
 * 做法：克隆一条既有设置行的实例，插到列表靠前的位置，再把克隆体的标题/点击换成 Kiora。
 * **既有行一个都不动** —— 这是与已废弃的「改写设置 → 通用 → 插件那一行」方案的本质区别，
 * 后者正是把「插件」行顶掉的元凶。
 *
 * ## 为什么能「新增行」——微信自己的 `clone()` 机制
 *
 * 设置页是「条目类注册表 + 前驱链」，不是 `provider + 有序列表`。条目基类
 * `r94/i` 实现 `java.lang.Cloneable` 且**每个子类都覆写 `clone()`**（8.0.78 实测，
 * 见 `recon/settings_items.txt` 的 152 个条目类，`clone()` 全在）。所以「拿一个
 * 现成实例克隆一份」是微信自己的合法构造路径：克隆体的类与原条目一致，
 * 适配器的 view type 解析（按类）照样命中，不需要新建子类、不触碰注册表一致性约束。
 *
 * ## 克隆体的 id 必须改掉（否则「插件」行会被顶没）
 *
 * 光克隆还不够。设置页 RecyclerView 开了 **stable id**，而条目 stableId = `M7().hashCode()`
 * （链路 `zs3/a.getItemId` ← `t94/f.v` ← `r94/i.M7`，见 [WeChatDexKit.SETTING_PLUGIN_ID]）。
 * 克隆体若沿用克隆源的 `M7()`，列表里就有**两条同 stableId** 的条目 —— RecyclerView 只排
 * 一条，另一条（**原「插件」行**）直接从界面消失。这就是「插件行又没了」的根因，
 * 与早先「改写原行」无关。故克隆体用 [ENTRY_ID] 覆写 `M7()`。
 *
 * ## 注入点：`la4/g.d` 的返回值
 *
 * [WeChatDexKit.SETTING_LIST_ORDER] 就是「按前驱链算出展示顺序」的方法
 * （详见该锚点的 KDoc）。它在 `BaseSettingPrefUI.onCreate` 里算完顺序、
 * **紧接着**才把结果包成 `ia4/e` 投给 MVVM 状态中心
 * （`recon/BaseSettingPrefUI_onCreate.txt` 0078~0096）。在 `d()` 的 hookAfter
 * 里改那个列表，等于抢在渲染之前改完 —— 不需要碰适配器、不需要碰 View 树。
 *
 * 返回值是 `c96/l(第一项=root, 第二项=顺序List)`；两个字段声明类型都是 `Object`，
 * 所以**不按字段名、按运行期值是不是 `MutableList` 来认**。
 *
 * ## 克隆源为什么选 `SETTING_PLUGIN_ENTRY` 那一类
 *
 * 8.0.78 主页（`SettingGroupMain`）的 20 项顺序已用前驱链还原（`recon/settings_items.txt`）：
 *
 * ```
 * 0 SettingAdditionHeaderSearch            <- 顶部搜索行（ja4/b，表头型）
 * 1 SettingGroupPersonalInfo               <- 第一个分组
 * ...
 * 13 other.SettingGroupPlugin              <- 本表锚点定位的「插件」行（ja4/f，正常可点行）
 * ...
 * 19 SettingAdditionBottom
 * ```
 *
 * 早先版本取「列表里第一个有点击方法的条目」，命中的是第 0 项 **搜索行**，克隆出来
 * 会在顶部多出一条搜索框形状的行 —— 不是正常设置行的样子。改成锚点定位的
 * `SettingGroupPlugin`：它本身就在这个列表里（保证适配器有 holder），且是
 * 「标题 + 右箭头」的普通行，克隆出来外观与其它行一致。
 *
 * ## 插入位置
 *
 * 插在**搜索行下方、所有分组之上**（index 1，见 [insertEntry] 的 `at`）——
 * 对应「设置页账号分组最上方」。列表只有 1 项时才退化成 index 0。
 *
 * ## 标题与点击怎么认人
 *
 * 标题走基类 `r94/i.O7()`（[WeChatDexKit.SETTING_ITEM_TITLE]）。**8.0.78 没有任何
 * 条目类覆写 `O7()`**（`recon/settings_items.txt` 全量 grep `O7` 命中 0），
 * 所以从基类拦一次即可，再用**实例同一性**只放行我们克隆出来的那一行。
 * 点击同理：拦克隆体那条继承链上的 `c8(Context,View,int)`，同一性过滤。
 *
 * `d()` 在 `onCreate` 与 `onNewIntent` 里各跑一遍，各产出一个新列表，所以
 * 克隆体可能有多个，用**同一性集合**（`IdentityHashMap`）而不是单个引用来认。
 *
 * ## 安全失败
 *
 * [loadHook] 验三个锚点；取不到就抛异常中止，整条不挂载（异常由 `MainHook.loadApiHook` 兜住）。
 * 任何一步（克隆失败 / 列表不可变 / 找不到可克隆的条目）只记日志返回，不影响设置页本身。
 *
 * ## 为什么是 [BaseApiHookItem] 而不是开关项
 *
 * 入口的有无由宿主结构决定，不该让用户去「杂项」开关里找它。这里与 QQ 侧
 * [QQSettingInject] 保持一致：继承 [BaseApiHookItem] → `isEnable` 恒为 true（回调永远
 * 生效）、且不进 `MainHook.switchHookItemList`（设置页不出现、无需操作）。
 * 早期误写成开关项（默认 false）会导致入口默认不生效，是必须纠正的架构偏差。
 */
@HookItemAnnotation(
    tag = "微信设置顶部入口",
    desc = "在微信设置页最上方插入一个 Kiora 入口（固定生效，无开关）",
    hosts = ["wechat"]
)
object WxSettingTopEntry : BaseApiHookItem<Listener>() {

    private const val ENTRY_TITLE = "Kiora"

    /**
     * 克隆体的**唯一 id**（= `M7()` 的返回值）。
     *
     * 不能沿用克隆源的 id：那会让两条同 id 条目在列表里撞 stableId（见
     * [WeChatDexKit.SETTING_PLUGIN_ID] 的 KDoc），RecyclerView 于是只排一条，
     * **原「插件」行被顶没** —— 这正是本 hook 之前一直没修好的根因。
     */
    private const val ENTRY_ID = "Kiora_Setting_Top_Entry"

    // 设置页**不加**标题左侧图标（用户已明确要求）。
    //
    // 早先尝试给克隆行加 Kiora app 图标（复用 `R.drawable.ic_launcher`），事后查清不可行：
    // 设置页所有普通行用的是 `v48` 布局（`r/p/byv.xml`），结构是
    //   `v48(#7f0a49b2) > cgi(#7f0a1633) > oct(#7f0a6b45) > [title, summary, a_4]`
    // **没有任何左侧图标槽**；绑定器给左图标用的 `h9o`(#7f0a39bd) 只存在于 13 个 `jlx`
    // 布局里，本页一个都没用到 —— 所以 `j8()` 就算返回资源 id，绑定器里
    // `holder.findViewById(h9o)` 也是 null 就直接 return，什么都画不出来。

    /** 我们克隆出来的条目实例（可能多个）。用同一性而不是 equals 认人。 */
    private val entries: MutableSet<Any> = Collections.newSetFromMap(IdentityHashMap())

    private var orderMethod: Method? = null
    private var titleMethod: Method? = null

    /** 克隆源类的 `M7()`（条目 id）。克隆体要覆写成 [ENTRY_ID] 来避开 stableId 冲突。 */
    private var idMethod: Method? = null

    /** 克隆源的类（= 「插件」行类）。只用它比较 `javaClass`，不实例化。 */
    private var cloneSourceClass: Class<*>? = null

    /** 克隆体那条链上的点击方法，首次插入时惰性解析并只挂一次。 */
    private var clickMethod: Method? = null

    /** 装饰 getter（标题右侧那排小图标的资源 id 提供者）是否已挂。 */
    private var decorSuppressed = false

    override fun loadHook() {
        // 三个锚点一次取全；任一缺失抛异常中止，整条不挂（由 MainHook.loadApiHook 兜住）。
        orderMethod = WeChatDexKit.requireMethod(WeChatDexKit.SETTING_LIST_ORDER)
        titleMethod = WeChatDexKit.requireMethod(WeChatDexKit.SETTING_ITEM_TITLE)
        cloneSourceClass = WeChatDexKit.requireClass(WeChatDexKit.SETTING_PLUGIN_ENTRY)
        idMethod = WeChatDexKit.requireMethod(WeChatDexKit.SETTING_PLUGIN_ID)

        val order = orderMethod ?: return
        val title = titleMethod ?: return
        val id = idMethod ?: return

        // 标题：只认我们克隆出来的那些行
        title.hookReplace(this) { chain ->
            if (chain.thisObject in entries) ENTRY_TITLE else chain.proceed(chain.args)
        }

        // 条目 id：克隆体报唯一 id，避开 RecyclerView 的 stableId 冲突（否则挤掉原「插件」行）
        id.hookReplace(this) { chain ->
            if (chain.thisObject in entries) ENTRY_ID else chain.proceed(chain.args)
        }

        // 顺序算完后、渲染前，往列表头部塞一行
        order.hookAfter(this) { param ->
            val root = param.args.getOrNull(0) ?: return@hookAfter
            val pair = param.result ?: return@hookAfter
            if (!isTopRoot(root)) return@hookAfter
            val list = extractList(pair) ?: return@hookAfter
            if (list.any { entries.contains(it) }) return@hookAfter
            insertEntry(list)
        }
    }

    private fun insertEntry(list: MutableList<Any?>) {
        // 首选锚点定位的「插件」行类；万一它不在本列表中，退而取第 2 项起第一个可点行
        // （跳过 index 0 的搜索行，免得又克隆出一个搜索框形状的行）。
        val want = cloneSourceClass
        val source = list.firstOrNull { it != null && (want == null || it.javaClass == want) }
            ?: list.drop(1).firstOrNull { it != null && it.javaClass.clickTarget() != null }
            ?: list.firstOrNull { it != null && it.javaClass.clickTarget() != null }
            ?: run {
                LogUtils.w("$name 列表里没有可克隆的条目，跳过")
                return
            }
        val sourceClass = source.javaClass
        val cloneMethod = sourceClass.cloneMethodOrNull() ?: run {
            LogUtils.w("$name ${sourceClass.name} 没有可用的 clone()，跳过")
            return
        }
        val click = sourceClass.clickTarget() ?: run {
            LogUtils.w("$name ${sourceClass.name} 没有点击方法，跳过")
            return
        }

        val clone = runCatching { cloneMethod.invoke(source) }.getOrElse {
            LogUtils.e(this, it)
            return
        }

        // 克隆源行可能带推广摘要（「插件」行的 `p` 由 onResume 写入）。清掉，只留标题。
        sourceClass.summaryFieldOrNull()?.let { f ->
            runCatching { f.set(clone, null) }
        }

        // 点击 hook 只挂一次（同一性过滤，挂基类那条也不会误伤别的条目）
        if (clickMethod == null) {
            clickMethod = click
            click.hookReplace(this) { chain ->
                if (chain.thisObject in entries) {
                    openSettings(chain.args)
                    null
                } else {
                    chain.proceed(chain.args)
                }
            }
        }

        // index 0 = 顶部搜索行，插到它下面（所有分组之上）；列表太短才退化成 0
        val at = if (list.size >= 2) 1 else 0
        entries.add(clone)
        installDecorSuppressors()
        list.add(at, clone)
    }

    /**
     * 抑制克隆体的**装饰图标**：克隆源「插件」行标题右侧会带一个新功能提示小图标
     * （灯泡，`ka4/r.h` 里读 `item.o8()` 拿 drawable，塞进 `app:id/o4r`）。
     * 克隆体原样继承了它，于是 Kiora 后面也挂了个灯泡 —— 不该有。
     *
     * 做法：把克隆源类里**所有无参、返回 `Integer` 的方法**（8.0.78 实测就是
     * `o8`/`p8`/`q8` 三个装饰 getter）统一挂上，遇到我们的克隆体就返回 `null`，
     * 视图层拿到 null drawable 自然不显示。原生「插件」行不是我们的实例，原样放行。
     *
     * 为什么拦 getter 而不是清字段：`onResume`/newtips 框架会通过 setter 把字段
     * 再写回来，只清字段会被覆盖；拦 getter 才稳。
     */
    private fun installDecorSuppressors() {
        if (decorSuppressed) return
        val cls = cloneSourceClass ?: return
        val getters = cls.declaredMethods.filter {
            it.parameterCount == 0 &&
                !Modifier.isStatic(it.modifiers) &&
                it.returnType == Int::class.javaObjectType
        }
        if (getters.isEmpty()) {
            LogUtils.w("$name 没找到克隆源的装饰 getter，灯泡可能仍在")
            return
        }
        getters.forEach { g ->
            g.hookReplace(this) { chain ->
                if (chain.thisObject in entries) null else chain.proceed(chain.args)
            }
        }
        decorSuppressed = true
    }

    private fun openSettings(args: Array<Any?>) {
        val context = args.firstOrNull { it is Context } as? Context
        if (context == null) {
            LogUtils.w("$name 拿不到 Context，无法打开 Kiora 设置")
            return
        }
        runCatching {
            context.startActivity(
                Intent(context, SettingActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { LogUtils.e(this, it) }
    }

    // ---- 结构化反查 ----

    /**
     * `c96/l` 里那个「值是 `MutableList`」的字段。
     *
     * `c96/l` 只有 `d`/`e` 两个 `Object` 字段（第一项 = root，第二项 = 顺序 List），
     * 声明类型区分不了，所以按运行期值判类型，避免依赖混淆后的字段名。
     */
    @Suppress("UNCHECKED_CAST")
    private fun extractList(pair: Any): MutableList<Any?>? {
        for (f in pair.javaClass.declaredFields) {
            f.isAccessible = true
            val value = runCatching { f.get(pair) }.getOrNull() ?: continue
            if (value is MutableList<*>) return value as MutableList<Any?>
        }
        return null
    }

    /**
     * 是不是最外层设置页的 root：`root.N7()` 返回的 `r94/u` 里有 `Class` 字段等于
     * `root` 自己的类。`N7()` 靠结构找 —— 0 参、返回一个「恰好 2 个 `Class` 字段」
     * 的类（8.0.78 = `r94/u(a:Class, b:Class)`）。
     */
    private fun isTopRoot(root: Any): Boolean =
        runCatching {
            val rootClass = root.javaClass
            val n7 = rootClass.methods.firstOrNull { m ->
                m.parameterCount == 0 &&
                    !m.returnType.isPrimitive &&
                    m.returnType.declaredFields.count { it.type == Class::class.java } == 2
            } ?: return false

            val u = n7.invoke(root) ?: return false
            u.javaClass.declaredFields
                .filter { it.type == Class::class.java }
                .any { it.apply { isAccessible = true }.get(u) == rootClass }
        }.getOrDefault(false)

    /** 沿继承链找 `(Context, View, int) -> void`，即条目的点击实现。 */
    private fun Class<*>.clickTarget(): Method? {
        var c: Class<*>? = this
        while (c != null && c != Any::class.java) {
            c.declaredMethods.firstOrNull {
                it.parameterCount == 3 &&
                    it.parameterTypes[0] == Context::class.java &&
                    it.parameterTypes[1] == View::class.java &&
                    it.parameterTypes[2] == Int::class.javaPrimitiveType &&
                    it.returnType == Void.TYPE
            }?.let { return it }
            c = c.superclass
        }
        return null
    }

    /**
     * 该类自己声明的、唯一一个非 final `String` 字段（克隆源 = 「插件」行时即摘要 `p`）。
     *
     * 必须用 `declaredFields`（不含继承）：基类 `r94/i` 还声明了 `e`、`f` 两个非 final
     * String，用 `fields` 会一并捞进来，拿不到唯一解。
     */
    private fun Class<*>.summaryFieldOrNull(): Field? = declaredFields
        .firstOrNull { it.type == String::class.java && !Modifier.isFinal(it.modifiers) }
        ?.apply { isAccessible = true }

    /** 条目基类实现 `Cloneable`，子类都覆写为 public，故直接按名字取即可。 */
    private fun Class<*>.cloneMethodOrNull(): Method? =
        runCatching { getMethod("clone") }.getOrNull()
}
