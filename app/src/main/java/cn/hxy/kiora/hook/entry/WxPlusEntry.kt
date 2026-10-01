package cn.hxy.kiora.hook.entry

import android.content.Context
import android.content.Intent
import android.util.SparseArray
import android.view.View
import android.widget.AdapterView
import android.widget.BaseAdapter
import cn.hxy.kiora.R
import cn.hxy.kiora.activity.SettingActivity
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseApiHookItem
import cn.hxy.kiora.hook.base.Listener
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.hook.hookReplace
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.wx.WeChatDexKit
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * 微信主页「+」菜单 Kiora 入口（对标 QQ 侧 [QQPlusInject]）。
 *
 * 目标：在「发起群聊」**上边**插一行「Kiora」，点它打开 Kiora 设置页。
 *
 * ## 为什么只登记一个锚点，其余全部结构化反查
 *
 * [WeChatDexKit.PLUS_MENU_ASSEMBLE] 定位到装配方法 `HomeUI.o()V` 后，管理器
 * （`com.tencent.mm.ui.wg`，`MicroMsg.PlusSubMenuHelper`）、菜单项 `tg`、描述
 * `ug`、适配器 `rg`、点击回调 `wg.onItemClick`、关闭方法 `hd.a()` **全部在运行期
 * 按结构取**，不写死任何混淆名 —— 这是 WA 系模块应对微信每版一轮混淆的一贯做法，
 * 也是本项目 [DexKitFinder] 只支持「单类/单方法」查询这一能力边界下的必然选择。
 *
 * ## 注入点为什么选 `hd.d(int)` 而不是 `o()` 的末尾
 *
 * `recon/homeui_o.txt` 显示 `o()` 的顺序是：
 * `填 wg.s → wg.r.notifyDataSetChanged() → hd.d(dimen)`（显示弹窗）。
 * 若挂在 `o()` 的 hookAfter 上，弹窗**已经显示并测过宽高**，此时再改 `wg.s`
 * 只能让 ListView 重排，弹窗自身宽度不会重测，新行会被裁。
 *
 * 而 `hd.d(int)` 内部在真正 `show()` 之前会**拿适配器跑一遍测量**
 * （`recon/hd_d.txt` 0202~0252：`Adapter.getCount()` 循环 `getView(i)` 量宽度，
 * 取最大宽 `MMListPopupWindow.c(...)`）。所以在 `hd.d` 的 hookBefore 里改 `wg.s`，
 * 我们这一行会被正常测量、正常显示，弹窗高度也按新项数算。
 *
 * ## 数据结构（8.0.78 实测）
 *
 * - `wg.s : SparseArray<tg>`，**key 就是列表下标**（`o()` 里 `put(idx++, tg)`）；
 * - `tg` 字段 `a:Z`、`b:ug`；`ug` 字段全 final：`a:String`(标题)、`b:I`(图标 res)、
 *   `c:I`(动作 id)、`d:I`(文字色)、`e:String`；
 * - `ug.<init>(int id, String title, String ?, int iconRes, int color)`，第 3 个 String
 *   在现场调用里恒为 `""` 且不被渲染（`recon/wg_f_dump.txt` 逐块核对）；
 * - 渲染在 `rg.getView`：`ug.b <= 0` → 图标 GONE（所以图标传 0 是安全的），
 *   `ug.a` 非空才 setText，`ug.d > 0` 才改文字色；
 * - `wg.onItemClick(AdapterView,View,int,long)` 读 `wg.s.get(pos).b.c` 做 switch 分发，
 *   **末尾统一调 `hd.a()` 关弹窗**，所以拦截我们那一项后必须自己补一次 `hd.a()`。
 *
 * ## 我们的动作 id
 *
 * 取 [OUR_ID]，刻意避开「+」菜单用到的 1/2/10/20 与哨兵 2147483646。拦截走的是
 * 实例同一性（`wg.s` 里那个 `tg` 就是我们在 hook 里新建的那个对象），因此即便
 * 动作 id 撞车也不会误伤别的菜单项。
 *
 * ## 安全失败
 *
 * [loadHook] 把「管理器类 / SparseArray 字段 / 适配器字段 / 显示方法 / 点击方法 /
 * 关闭方法」六件套一次验完，任一缺失抛异常中止，整条不挂载 —— 宁可不出现入口，也不要半残。
 *
 * ## 为什么是 [BaseApiHookItem] 而不是开关项
 *
 * 入口的有无由宿主结构决定，不该让用户去「杂项」开关里找它。这里与 QQ 侧
 * [QQPlusInject] 保持一致：继承 [BaseApiHookItem] → `isEnable` 恒为 true（回调永远
 * 生效）、且不进 `MainHook.switchHookItemList`（设置页不出现、无需操作）。
 * 早期误写成开关项（默认 false）会导致入口默认不生效，是必须纠正的架构偏差。
 */
@HookItemAnnotation(
    tag = "微信加号入口",
    desc = "在微信主页「+」菜单的「发起群聊」上边加一个 Kiora 入口（固定生效，无开关）",
    hosts = ["wechat"]
)
object WxPlusEntry : BaseApiHookItem<Listener>() {

    private const val ENTRY_TITLE = "Kiora"

    /**
     * 标题左侧的 Kiora 图标，与 QQ 侧 [QQPlusInject] 用的是同一个 `R.drawable.ic_launcher`。
     *
     * `ug` 构造第 4 个参数就是图标资源 id，`com.tencent.mm.ui.rg.getView` 里：
     * ```
     * 011a  iget  v2, ug->b:I          // 图标 res
     * 0122  if-lez v2, -> 013c         // <=0 直接 GONE
     * 0134  imageView.setImageResource(v2)   // 资源 id 交给视图 context 解析
     * ```
     * 模块资源包 id 是 0x44（与微信的 0x7F 不撞号），且 `Startup` 已把模块 resources.arsc
     * 注入宿主 Resources，所以 `setImageResource` 能取到真图。
     */
    private val ENTRY_ICON = R.drawable.ic_launcher

    /** 自建菜单项的动作 id。刻意避开菜单实际用到的 1/2/10/20 与 2147483646。 */
    private const val OUR_ID = 0x4B10

    /** `onItemClick(AdapterView,View,int,long)` 里位置参数的下标（[HookParam.args] 不含 this）。 */
    private const val POS_INDEX = 2

    // ---- 运行期结构化解析结果（供 [injectEntry] / [readMenuAt] 使用）----

    private var menuMapField: Field? = null
    private var adapterField: Field? = null

    // ---- 菜单项构造（首次注入时用样本对象反推）----

    private var tgCtor: Constructor<*>? = null
    private var ugCtor: Constructor<*>? = null

    /** 我们插进去的那个 `tg` 对象，点击时按同一性识别。 */
    private var entryTg: Any? = null

    override fun loadHook() {
        // 一次性验完「管理器类 / SparseArray 字段 / 适配器字段 / 显示方法 / 点击方法 / 关闭方法」
        // 六件套，任一缺失抛异常中止，整条不挂（异常由 MainHook.loadApiHook 兜住并记日志）。
        val assemble = WeChatDexKit.requireMethod(WeChatDexKit.PLUS_MENU_ASSEMBLE)
        val helper = checkNotNull(assemble.declaringClass.findPlusHelperClass()) {
            "没有找到「+」菜单管理器（HomeUI 中带 SparseArray + BaseAdapter 字段的那个字段）"
        }
        menuMapField = checkNotNull(
            helper.declaredFields.firstOrNull { it.type == SparseArray::class.java }
        ) { "管理器没有 SparseArray 字段" }.apply { isAccessible = true }
        adapterField = checkNotNull(helper.helperAdapterField()) { "管理器没有 BaseAdapter 字段" }
        val show = checkNotNull(helper.findShowMethod()) { "管理器没有 (int)->boolean 显示方法" }
        val click = checkNotNull(helper.findItemClickMethod()) { "管理器没有 onItemClick 点击方法" }
        val close = checkNotNull(show.declaringClass.findCloseMethod()) { "弹窗基类没有无参 void 关闭方法" }

        // 显示前改 wg.s：此刻弹窗还没测量，新行会被正常量进去
        show.hookBefore(this) { param ->
            val target = param.thisObject
            if (helper.isInstance(target)) injectEntry(target)
        }

        // 点击：只拦我们那一项，其余原样放行
        click.hookReplace(this) { chain ->
            val target = chain.thisObject
            if (!helper.isInstance(target)) return@hookReplace chain.proceed(chain.args)

            val pos = (chain.args.getOrNull(POS_INDEX) as? Number)?.toInt() ?: -1
            if (readMenuAt(target, pos) === entryTg) {
                openSettings(chain.args)
                runCatching { close.invoke(target) }.onFailure { LogUtils.e(this, it) }
                null
            } else {
                chain.proceed(chain.args)
            }
        }
    }

    // ---- 注入 ----

    @Suppress("UNCHECKED_CAST")
    private fun injectEntry(target: Any) {
        val map = menuMapField?.get(target) as? SparseArray<Any?> ?: return
        if (map.size() == 0) return

        val sample = map.valueAt(0) ?: return
        if (!ensureItemTypes(sample)) {
            LogUtils.w("$name 未能从样本 ${sample.javaClass.name} 反推出 tg/ug 构造器，入口不插入")
            return
        }

        val tg = runCatching {
            tgCtor!!.newInstance(ugCtor!!.newInstance(OUR_ID, ENTRY_TITLE, "", ENTRY_ICON, 0))
        }.getOrElse {
            LogUtils.e(this, it)
            return
        }
        entryTg = tg

        // 重建：我们排 index 0（「发起群聊」之前），其余整体右移一位。
        // key 必须等于下标（rg.getView(i) = wg.s.get(i)），所以不能只 put 不搬。
        val n = map.size()
        val old = ArrayList<Any?>(n)
        for (i in 0 until n) old.add(map.valueAt(i))
        map.clear()
        map.put(0, tg)
        for (i in old.indices) map.put(i + 1, old[i])

        // o() 里的 notifyDataSetChanged 发生在我们改 map 之前，补一次让适配器刷新
        runCatching { (adapterField?.get(target) as? BaseAdapter)?.notifyDataSetChanged() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun readMenuAt(target: Any, pos: Int): Any? {
        if (pos < 0) return null
        return (menuMapField?.get(target) as? SparseArray<Any?>)?.get(pos)
    }

    /**
     * 用现场第一个菜单项当样本，反推 `tg` / `ug` 的构造器。
     *
     * 判据全部是结构信息，不依赖混淆名：`ug` 的特征是「有 5 参构造器
     * `(int,String,String,int,int)`」；`tg` 的特征是「有单个参数、且该参数类型正是 `ug`
     * 的构造器」。样本一定存在（首行就是「发起群聊」）。
     */
    private fun ensureItemTypes(sample: Any): Boolean {
        if (tgCtor != null && ugCtor != null) return true
        return runCatching {
            val tgClass = sample.javaClass
            val ugField = tgClass.declaredFields.firstOrNull { f ->
                !f.type.isPrimitive && f.type != String::class.java &&
                    f.type.declaredConstructors.any { it.isUgSignature() }
            } ?: return false
            val ugClass = ugField.type
            tgCtor = tgClass.declaredConstructors
                .first { it.parameterCount == 1 && it.parameterTypes[0] == ugClass }
                .apply { isAccessible = true }
            ugCtor = ugClass.getDeclaredConstructor(
                Int::class.javaPrimitiveType,
                String::class.java,
                String::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            ).apply { isAccessible = true }
            true
        }.getOrElse {
            LogUtils.e(this, it)
            false
        }
    }

    private fun Constructor<*>.isUgSignature(): Boolean =
        parameterTypes.map { it.name } ==
            listOf("int", "java.lang.String", "java.lang.String", "int", "int")

    private fun openSettings(args: Array<Any?>) {
        val context: Context? = (args.getOrNull(1) as? View)?.context
            ?: (args.getOrNull(0) as? AdapterView<*>)?.context
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
     * `HomeUI` 里类型为「含 SparseArray 字段 + 含 BaseAdapter 字段」的那个字段，
     * 即 `+` 菜单管理器（8.0.78 = `wg`）。两个条件同时命中在 HomeUI 的字段里唯一。
     */
    private fun Class<*>.findPlusHelperClass(): Class<*>? = declaredFields.firstOrNull { f ->
        val t = f.type
        !t.isPrimitive && t != String::class.java && t != Context::class.java &&
            t.declaredFields.any { it.type == SparseArray::class.java } &&
            t.declaredFields.any { BaseAdapter::class.java.isAssignableFrom(it.type) }
    }?.type

    private fun Class<*>.helperAdapterField(): Field? = declaredFields
        .firstOrNull { BaseAdapter::class.java.isAssignableFrom(it.type) }
        ?.apply { isAccessible = true }

    /** 沿继承链找 `(int) -> boolean` 的显示方法（8.0.78 = `hd.d(I)Z`）。 */
    private fun Class<*>.findShowMethod(): Method? = walkUp { c ->
        c.declaredMethods.firstOrNull {
            it.parameterCount == 1 &&
                it.parameterTypes[0] == Int::class.javaPrimitiveType &&
                it.returnType == Boolean::class.javaPrimitiveType
        }
    }

    /** 沿继承链找 `onItemClick`（8.0.78 声明在 `wg` 自身）。 */
    private fun Class<*>.findItemClickMethod(): Method? = walkUp { c ->
        c.declaredMethods.firstOrNull {
            it.parameterCount == 4 &&
                it.parameterTypes[0] == AdapterView::class.java &&
                it.parameterTypes[1] == View::class.java &&
                it.parameterTypes[2] == Int::class.javaPrimitiveType &&
                it.parameterTypes[3] == Long::class.javaPrimitiveType &&
                it.returnType == Void.TYPE
        }
    }

    /**
     * 关闭方法 = 该类里「无参 + 返回 void + 名字不是接口强制保留名」的那个
     * （8.0.78 = `hd.a()V`）。`onDismiss` / `onGlobalLayout` 是
     * `PopupWindow$OnDismissListener` / `ViewTreeObserver$OnGlobalLayoutListener`
     * 的方法，名字不会被混淆，排除掉就只剩 `a()`。
     */
    private fun Class<*>.findCloseMethod(): Method? = declaredMethods.firstOrNull {
        it.parameterCount == 0 &&
            it.returnType == Void.TYPE &&
            it.name != "onDismiss" &&
            it.name != "onGlobalLayout"
    }

    private inline fun Class<*>.walkUp(find: (Class<*>) -> Method?): Method? {
        var c: Class<*>? = this
        while (c != null && c != Any::class.java) {
            find(c)?.let { return it }
            c = c.superclass
        }
        return null
    }
}
