package cn.hxy.kiora.wx.util

import cn.hxy.kiora.utils.dexkit.DexKitTask
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.base.BaseMatcher
import org.luckypray.dexkit.query.matchers.ClassMatcher

/**
 * 微信侧 DexKit 特征表。
 *
 * ## 为什么用字符串而不是类名
 *
 * 微信每个版本都有一轮激进的类名混淆（`com.tencent.mm.ui.chatting.component.cn`
 * 这种名字换版本就变），但**日志格式串和 sp key 几乎不动**。所以锚点只能靠
 * 字符串常量反查，不能写死类名 —— 这是 WA 系模块的一贯做法，本表沿用。
 *
 * 每条特征串的预期命中对象，都是用 [D:/code/fenxi3/recon/strref.py]
 * 对微信 8.0.78 (arm64) 安装包的 17 个 dex 做全量 `const-string` 反向定位后
 * **逐条核对过**的，不是推测。KDoc 里记的是 8.0.78 当时的混淆名，仅供排查时
 * 肉眼比对；运行时一律走字符串匹配，版本升级后哪怕类名全变也照样命中。
 *
 * ## 多串是 AND，不是 OR
 *
 * [DexKitTask.getQueryMap] 里一条 query 的多个 `usingEqStrings` 参数是**与**语义：
 * 同一个方法体里必须同时出现这些串。已对 8.0.78 逐一验证过「交集恰好一个方法」，
 * 否则 [cn.hxy.kiora.utils.dexkit.DexKitFinder] 里的 `singleOrNull()` 会拿到 null
 * 并记一条「没有匹配结果」的警告。新增条目务必同样验证唯一性。
 *
 * ## 版本分支
 *
 * `setImageHdImgBtnVisibility` 是 8.0.54 之前的旧串，8.0.54 起被
 * `setHdImageActionDownloadable` 取代（已确认旧串在 8.0.78 的字符串池里
 * 完全不存在）。本表只登记当前基线用的那条，不保留死串，以免平白多出
 * 必然失败的查询。
 */
object WeChatDexKit : DexKitTask {

    // ---- 聊天 ----

    /** 阻止消息撤回：`Lb41/t;->c(...)`。 */
    const val ANTI_REVOKE_1 = "AntiRevoke1.MethodDoRevokeMsg"

    /** 自动查看原图（高清图开关）：`Lcom/tencent/mm/ui/chatting/gallery/ImageGalleryUI;->y9()V`。 */
    const val AUTO_VIEW_ORIGINAL_HD = "AutoViewOriginalPhoto.MethodSetHdImageActionDownloadable"

    /** 自动查看原视频：`ImageGalleryUI;->M9()V`。 */
    const val AUTO_VIEW_ORIGINAL_VIDEO = "AutoViewOriginalPhoto.MethodCheckNeedShowOriginVideoBtn"

    /** 屏蔽提示音：`Lx44/u;->dj(Lx44/i;Landroid/os/Bundle;)Z`。 */
    const val DISABLE_RINGTONE = "DisableRingtonePlay.MethodPlaySound"

    /** 禁用发送状态：`com.tencent.mm.ui.chatting.component.cn;->u0(I)V`。 */
    const val DISABLE_SEND_STATUS = "DisableSendStatus.MethodDirectSend"

    /**
     * 发送文本格式化：**类**级锚点，`com.tencent.mm.ui.chatting.component.pm`。
     *
     * 这里查类而不是方法：WA 的原始做法是「先按类名串找到类，再在类里取
     * 参数个数 12~14 的**构造方法**」。本表只承担第一阶段，第二阶段在拿到的类上
     * 用反射取构造方法。**但第二阶段的参数假设已在 8.0.78 上实证修正过，勿照抄 WA**：
     *
     * - `pm` **只有 1 个构造方法**，参数 **15** 个（不是 12~14）。WA 的
     *   `paramCount(12..14)` 在 8.0.78 上匹配 0 个方法 → `singleOrNull()` 拿 null
     *   → 整条 hook 不挂载。取法用 `declaredConstructors.single()` 并断言 15。
     * - 待发文本在 **arg8**（字段 `n`）。依据：`pm.run()` 把它作为 p1 交给
     *   `Loh0/c;->a(...)`，而该方法的 Kotlin 形参名常量正是 `"content"`
     *   （p0 是 `"toUserName"`，来自字段 `m`）。
     * - **arg4（字段 `g`）与 arg8 同源同值**：`pm` 的唯一构造点
     *   `om;->w0(String,I,String,HashMap)Z` @0x464 是 `invoke-direct/range {v1..v16}`，
     *   `move-object v6, v10` 把文本写进 arg4，而 arg8 就是同一寄存器 `v10`
     *   （= `y8;->J(w0.p0)`）。两条发送路径分别读 `g` 与 `n`：
     *   `run()` 里 `d:Z`（ctor index 1）为 0 走主路径（读 `n`），
     *   `d != 0`（talker == `"medianote"`）走 `Lf51/b` 本地 fake 路径（读 `g`）。
     *   → 改写时**两个下标都要写**，且 arg4 仅在「与原文相同」时才改。
     * - WA 的 `else -> 7` 在 15 参形态下取到的是**接收者**（字段 `m` = `toUserName`），
     *   不是文本 —— 这是照抄下标最直接的翻车点。
     *
     * 详见 `docs/Kiora多宿主改造设计.md` §17.3 的证据链。
     */
    const val MSG_FORMAT_SEND_TEXT_CLASS = "MsgFormat.ClassSendTextComponent"

    // ---- 辅助 ----

    /**
     * 表情游戏：`Lvr/p;->a(Landroid/view/View;Landroid/content/Context;ILsr/u0;)V`。
     *
     * **查询本身没问题，WA 的 hook 体才是有问题的那一半**（8.0.78 实证）：
     *
     * 8.0.78 把「点击信息」拆成了两个类，`args(3)` 的**声明类型是基类**、**运行期是子类**：
     *
     * ```
     * Lsr/u0;  public abstract            — 字段 a:I（public final）← 判别位在这里
     * Lsr/g;   public final extends sr/u0 — 字段 b:Lcom/tencent/mm/api/IEmojiInfo;（+ c:I, d:String, e:I）
     * ```
     *
     * 于是 WA 的两步都错：
     * 1. `firstField { FINAL; Int }` 在运行期类 `sr/g` 上有 **3 个候选**（继承的 `a`、
     *    自身的 `c` 与 `e`），拿不到判别位 `a` → 闸门 `infoType == 0` 不稳定。
     *    正解：从**声明类型**（`method.parameterTypes[3]` = `sr/u0`）取那个 int 字段。
     * 2. `IEmojiInfo` 在**子类** `sr/g.b` 上，需沿运行期类的继承链按**类型**找
     *    （`type.name == "com.tencent.mm.api.IEmojiInfo"`），不能按字段名。
     *
     * 闸门值 `0` 本身是对的：`vr/p.a` @0x7ea 的 `a == 0` 分支才走
     * `Lq72/m;->m(IEmojiInfo)Z`（是否随机表情）→ 发送表情，猜拳/骰子在这里；
     * `a == 6` 是打日志 + 资料弹窗。`sr/g.<init>` 由 `IEmojiInfo.p1()` 映射出 `a`（0 或 6）。
     *
     * 详见 `docs/Kiora多宿主改造设计.md` §17.4。
     */
    const val EMOJI_GAME_CLICK = "EmojiGame.MethodEmojiPanelClick"

    /**
     * 表情游戏的「随机数」方法：`Lcom/tencent/mm/sdk/platformtools/y8;->Q(II)I`。
     *
     * 猜拳/骰子的随机结果由它产出（入参是表情类型，`2` = 猜拳、`5` = 骰子），
     * 改返回值就是「预设随机结果」。**它和 [EMOJI_GAME_CLICK] 是配套的两条锚点**：
     * 前者决定「点了什么」，后者决定「出什么」。
     *
     * 查询**只用结构约束、不带字符串**（WA 原查询另外要求方法体内 invoke
     * `currentTimeMillis` 与 `nextInt`；本表省掉这层，因为规则串越多越脆）。
     * 代价是必须自己确认唯一性 —— 已用 `recon/check_random.py` 复算：
     * 包 `com.tencent.mm.sdk.platformtools` 下 `(II)I` 方法**恰好 1 个**，
     * 且体内同时引用 `currentTimeMillis` 与 `nextInt`，与 WA 的约束完全吻合。
     */
    const val EMOJI_GAME_RANDOM = "EmojiGame.MethodRandom"

    /** 定位（国测局坐标）：`Lh51/t;->onLocationChanged(...)V`。 */
    const val LOCATION_LISTENER = "Location.MethodSLocationListener"

    /** 定位（WGS84）：`Lh51/u;->onLocationChanged(...)V`。 */
    const val LOCATION_LISTENER_WGS84 = "Location.MethodSLocationListenerWgs84"

    /** 定位 SDK 默认管理器：`Lmf/c;->onLocationChanged(...)V`。 */
    const val LOCATION_DEFAULT_MANAGER = "Location.MethodDefaultTencentLocationManager"

    /** 选点地图点击：`com.tencent.mm.plugin.location.ui.impl.l1;->onClick(Landroid/view/View;)V`。 */
    const val LOCATION_SELECT_POI_MAP = "Location.MethodSelectPoiMapOnClick"

    /** 语音时长：`Lv61/m1;->K1(Ljava/lang/String;Lv61/c1;)Z`。 */
    const val VOICE_LENGTH = "VoiceLength.MethodVoiceStorage"

    // ---- 高危 ----

    /** 微信运动步数：`com.tencent.mm.plugin.sport.model.d;->a()J`。 */
    const val SPORT_STEP = "SportStep.MethodDeviceStep"

    // ---- 杂项 ----

    /**
     * 模拟扫码：`Lv74/v;->g(...)V`（16 个参数的扫码入口）。
     *
     * 注意第二个串 `key_offline_scan_show_tips` **同时**出现在
     * `com.tencent.mm.plugin.scanner.ui.BaseScanUI;->L7(ILandroid/os/Bundle;)V` 里。
     * 两串的**交集**落在 `v74/v.g`，所以本表能唯一命中；若哪天把
     * `key_offline_scan_show_tips` 单独拎出来用，就会一下命中两个方法而拿不到结果。
     */
    const val MOCK_SCAN = "MockScan.MethodQBarHandler"

    /** 多开 WebView：`Lhc5/l;->j(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;Landroid/content/Intent;Landroid/os/Bundle;)V`。 */
    const val MULTI_WEBVIEW = "MultiWebView.MethodPluginHelper"

    /** 跳过分享校验：`com.tencent.mm.pluginsdk.model.app.i1;->b(...)Z`。 */
    const val SHARE_CHECK = "ShareCheck.MethodCheckAppSignature"

    // ---- 设置入口 ----

    /**
     * 设置页「插件」行所在类，8.0.78 = `com.tencent.mm.plugin.setting.ui.setting_new.settings.other.SettingGroupPlugin`。
     *
     * 本模块**只拿它当「一条正常设置行」的样板类**：在设置页最上方**克隆**一份实例插入，
     * 把克隆体标题改成 Kiora（见 [cn.hxy.kiora.wx.hook.entry.WxSettingTopEntry]）。
     * 原「插件」行本身一个字节都不改 —— 早先「改写原行」的方案已废弃，正是它把「插件」顶掉了。
     *
     * 该类的唯一标识由 `M7()` 返回常量串 `SettingGroup_Main_Other_Plugin`，而该串在 8.0.78
     * 全包里**只在这一个方法体里出现**（`recon/anchor_scan.py` 复算：1 条），所以
     * `FindClass` 就能唯一定位到类本身。
     *
     * 拿到类之后不再依赖任何混淆名（都按结构取，见类里 KDoc）：
     * - 标题走基类 `O7()`，见 [SETTING_ITEM_TITLE]；
     * - 摘要 = 该类唯一的非 final `String` 字段（克隆后清空，免得带上推广文案）；
     * - 点击 = 该类唯一的 `(Context, View, int) -> void` 方法。
     *
     * 类名本身其实没混淆（`settings/other/SettingGroupPlugin` 是可读命名），但仍走字符串
     * 反查：设置模块跨版本重构过，包路径不如 item id 常量稳。
     *
     * 位置校准（`recon/settings_items.txt` 前驱链还原，8.0.78 主页 20 项）：
     * `SettingGroupMain` 的直接子条目里本类排第 14（0-based 13），是主页上一条正常的
     * 「标题 + 右箭头」行，适配器对它的 view type 一定有 holder —— 所以拿它当克隆源最稳。
     */
    const val SETTING_PLUGIN_ENTRY = "SettingEntry.PluginGroupClass"

    /**
     * 设置条目的「唯一 id」getter：`Lr94/i;->M7()`，**每个条目类各自覆写**（基类 `r94/i` 里没有
     * `M7()`，见 `recon/settings_items.txt`）。所以只能按「克隆源类」那一个实现来定位，不能像
     * [SETTING_ITEM_TITLE] 那样从基类拦。
     *
     * ## 为什么必须知道它 —— stableId 冲突（8.0.78 实测根因）
     *
     * 设置页的列表是 `MvvmList` + `WxRecyclerAdapter`，而 `WxRecyclerAdapter` 建的时候就
     * `setHasStableIds(true)`（`Ltf0/r0;->V0(...)`，`recon/find_stable.py` 复算）。条目的
     * stableId 一路是：
     *
     * ```
     * RecyclerView 取 id
     *   ← Lzs3/a;->getItemId()J            // 条目包装基类：v().hashCode()
     *   ← Lt94/f;->v()Ljava/lang/String;   // = this.e.M7()
     *   ← Lr94/i;->M7()Ljava/lang/String;  // 条目自己的 id 常量
     * ```
     *
     * 也就是说 **`M7()` 的 hashCode 就是 RecyclerView 的 stableId**。往列表里塞一条与本列表
     * 某条 `M7()` 相同的克隆体 → 两条同 stableId → RecyclerView 只会排一条，另一条（本例是
     * **原「插件」行**）从界面上消失。这就是「插件行又没了」的真正原因，与「改写原行」无关。
     *
     * 所以 [cn.hxy.kiora.wx.hook.entry.WxSettingTopEntry] 必须把**克隆体**的 `M7()` 改成唯一值。
     *
     * ## 查询谓词
     *
     * id 常量串 `SettingGroup_Main_Other_Plugin` 在 8.0.78 全包里**只出现在这个方法体内**
     * （见 [SETTING_PLUGIN_ENTRY] 的复算），故 `usingEqStrings(该串)` 单独就唯一；
     * 再叠 `paramCount(0)` + `returnType(String)` 作结构兜底。
     */
    const val SETTING_PLUGIN_ID = "SettingEntry.PluginItemId"

    /**
     * 设置条目标题 getter：`Lr94/i;->O7()`，基类实现 = `getContext().getString(P7())`。
     *
     * 8.0.78 全量条目类（`recon/settings_items.txt`，152 个）**没有任何一个覆写 `O7()`**，
     * 标题一律来自这条基类方法，所以从基类拦一次即可；调用方再用实例/类过滤出目标行 ——
     * 无条件替换会把整页条目标题全改掉。
     *
     * 谓词三段缺一不可（已用 `recon/anchor_scan.py` 复算）：
     * - `usingEqStrings("getString(...)")` 是 `O7()` 体内 Kotlin `checkNotNullExpressionValue`
     *   留下的串，**单独用会命中 125 个方法**；
     * - `declaredClass(ClassMatcher().usingEqStrings("switch_button_type"))` 把范围收窄到
     *   `r94/i`（该串唯一命中它的 `S7(Intent)Map`）；收窄后「0 参 + 返回 String + 含该串」
     *   恰好 1 条；
     * - `paramCount(0)` + `returnType(String)` 再排除同类里签名不符的方法。
     */
    const val SETTING_ITEM_TITLE = "SettingEntry.ItemTitle"

    /**
     * 微信主页右上角「+」菜单的装配方法：`Lcom/tencent/mm/ui/HomeUI;->o()V`（8.0.78）。
     *
     * 本表只登记**这一个**方法锚点，菜单管理器/菜单项/适配器/点击监听全部在运行期
     * 由它**结构化反查**（见 [cn.hxy.kiora.wx.hook.entry.WxPlusEntry]），不写死任何混淆名。
     *
     * ## 为什么单串就够
     *
     * `usingEqStrings("dyna plus config is null, we use default one")` 用
     * `recon/anchor_scan.py` 复算：全包**恰好 1 个方法体**引用它
     * （`classes11.dex` `Lcom/tencent/mm/ui/HomeUI;->o()V`），字符串池里也只有 1 条。
     * 所以不需要 `paramCount`/`returnType` 之类的附加约束就已经唯一。
     *
     * ## 这个方法的语义（8.0.78 反汇编，`recon/homeui_o.txt`）
     *
     * `o()` 是「+」按钮的点击处理，**同时负责开和关**：
     *
     * - `HomeUI.k`（字段 `k`，类型 `com.tencent.mm.ui.wg`）非空且已登录时进入；
     * - 若 `wg.c()`（弹窗已显示）→ `wg.a()`（关闭）后直接返回；
     * - 否则把菜单数据填进 `wg.s`（`SparseArray<tg>`，**下标即列表位置**）：
     *   按登录态选 `wg.z`/`y`/`x`（`[2,1,10,20]` 之类），循环
     *   `new tg(wg.f(id))` → `wg.s.put(idx++, tg)`；末尾可能再补「离线付款」(id=20)
     *   与 id=2147483646；再对 `ug.c==10`（扫一扫）调 `m74/i0.hj(mode, tg.a)`；
     * - 最后 `wg.r.notifyDataSetChanged()` + `hd.d(res)` 显示弹窗。
     *
     * 因此「在弹窗显示前插一行」只需在 `hd.d(I)` 之前改 `wg.s`：`rg.getView(i)` /
     * `rg.getCount()` 都是**惰性**读 `wg.s`（`recon/wg_f_dump.txt` 同族复算），
     * 插进去就会被渲染，弹窗也按新项数测量高度。
     */
    const val PLUS_MENU_ASSEMBLE = "PlusMenu.MethodAssemble"

    /**
     * 设置页条目「显示顺序」计算方法：`Lla4/g;->d(Lr94/i;Ljava/util/List;)Lc96/l;`（8.0.78）。
     *
     * ## 为什么是这一个方法
     *
     * 设置页不是「provider + 列表」，而是「条目类注册表 + 前驱链」：每个条目
     * `r94/i` 用 `N7()` 返回 `r94/u(前驱→后继? 见下)`，由本方法把散装的类集合
     * 排序成最终展示用的 `List`。要往页面里塞一行，最稳妥的落点就是它算完顺序、
     * 还没交给 MVVM 状态中心（`BaseSettingPrefUI.onCreate` 里紧接着的
     * `getStateCenter().x4(ia4/e(l))`）的那一刻 —— 即本方法的返回值。
     *
     * ## 反汇编语义（8.0.78，`recon/order2.py` 复算）
     *
     * `recon/la4_g_d.txt`。核心三步：
     *
     * 1. 建 `map`：`map[item.N7().b] = item`，跳过 `N7().a == root.getClass()`
     *    或 `item === root` 的条目（即跳过最外层容器自身）；
     * 2. 从 `map.get(null)`（**没有前驱**的那条 = 列表第一条）起，用
     *    `map[cur.getClass()]`（前驱是 cur 的那条 = cur 的下一条）逐级展开 →
     *    **得到的 `ArrayList` 就是自上而下的展示顺序**；
     * 3. `map.size()` 与展开长度不等时打 `frontSize/childrenSize` 日志（不抛），
     *    相等时返回 `c96/l(root, 顺序List)`。
     *
     * 因此 `N7().b` 是**前驱类**：`SettingAdditionHeaderSearch`（b 为 null）排第一，
     * `SettingGroupMain` 的直接子项顺序为
     * `HeaderSearch → PersonalInfo → AccountInfo → PrivacyPermission → Notify → …`
     * （详见 `recon/order.py` 的链输出，与真机 `recon/set_live.txt` 的
     * `WxRecyclerView #7f0a58af` 子节点对得上）。
     *
     * ## 查询谓词
     *
     * 方法体内同时出现 Kotlin 的 `checkNotNull` 参数名串 `rootItem` 与日志串
     * `frontSize:`。`recon/anchor_scan.py` 复算：`rootItem` 全包**仅 1 个方法体**
     * 引用（字符串池里另有一条 `[ImageEditXWFlow]…rootItems=` 是别的模块的，方法体不命中），
     * 再加 `frontSize:` 交叉锁死到 `la4/g.d`，`singleOrNull()` 必然拿到唯一解。
     */
    const val SETTING_LIST_ORDER = "SettingEntry.ListOrderMethod"

    /**
     * 当前登录账号（wxid）getter：`com.tencent.mm.app.q3;->i()Ljava/lang/String;`。
     *
     * 反汇编依据（8.0.78 `classes11.dex`）：该方法依次读取
     * `login_weixin_username`（默认 `""`）、`login_user_name`，并带 `never_login_crash`，
     * 返回登录用户名 —— 即微信侧的 wxid。
     *
     * `login_weixin_username` ∩ `never_login_crash`（乃至再加 `login_user_name`）
     * 都**仍有两个**命中，另一个是 `com.tencent.mm.plugin.voip.widget.k;->e(Landroid/content/Intent;)V`。
     * 两者靠 `returnType(String)` 区分：voip 那个返回 void。
     * 用返回类型而不是 `name` 或包名做约束，是因为返回类型是结构信息，
     * 不像方法名/类名那样每版都换。
     *
     * 目前仅登记锚点、**尚未接入** `IHostAdapter.currentAccount`：
     * 接进去会牵动启动顺序（DexKit 首轮要弹窗 + 重启才写缓存），
     * 属阶段 3 的事，届时 [cn.hxy.kiora.wx.host.WeChatHostAdapter.currentAccount]
     * 在这个键上做一次 `requireMethod` 调用即可，取不到就退回 `"global"`。
     */
    const val ACCOUNT_CURRENT = "Account.CurrentUsername"

    private const val PKG_CHATTING = "com.tencent.mm.ui.chatting.component"
    private const val PKG_LOCATION = "com.tencent.mm.plugin.location.ui.impl"
    private const val PKG_SPORT = "com.tencent.mm.plugin.sport.model"
    private const val PKG_PLATFORMTOOLS = "com.tencent.mm.sdk.platformtools"

    override fun getQueryMap(): Map<String, BaseMatcher> = mapOf(

        ANTI_REVOKE_1 to FindMethod().apply {
            matcher {
                usingEqStrings("doRevokeMsg xmlSrvMsgId=%d talker=%s isGet=%s")
            }
        },

        AUTO_VIEW_ORIGINAL_HD to FindMethod().apply {
            matcher {
                usingEqStrings("setHdImageActionDownloadable")
            }
        },

        AUTO_VIEW_ORIGINAL_VIDEO to FindMethod().apply {
            matcher {
                usingEqStrings("checkNeedShowOriginVideoBtn")
            }
        },

        DISABLE_RINGTONE to FindMethod().apply {
            matcher {
                // 「MicroMsg.BaseSceneSetting」单独用会命中别的类，必须配日志串收窄。
                usingEqStrings("MicroMsg.BaseSceneSetting", "playSound Failed Throwable t = ")
            }
        },

        DISABLE_SEND_STATUS to FindMethod().apply {
            searchPackages(PKG_CHATTING)
            matcher {
                usingEqStrings(
                    "MicroMsg.SignallingComponent",
                    "[doDirectSend] mChattingContext is null!"
                )
            }
        },

        MSG_FORMAT_SEND_TEXT_CLASS to FindClass().apply {
            searchPackages(PKG_CHATTING)
            matcher {
                usingEqStrings("MicroMsg.ChattingUI.SendTextComponent", "doSendMessage begin send txt msg")
            }
        },

        EMOJI_GAME_CLICK to FindMethod().apply {
            matcher {
                usingEqStrings(
                    "MicroMsg.EmojiPanelClickListener",
                    "penn send capture emoji click emoji: %s status: %d."
                )
            }
        },

        EMOJI_GAME_RANDOM to FindMethod().apply {
            searchPackages(PKG_PLATFORMTOOLS)
            matcher {
                returnType(Int::class.java)
                paramTypes(Int::class.java, Int::class.java)
            }
        },

        LOCATION_LISTENER to FindMethod().apply {
            matcher {
                // 两个 Listener 的 `onLocationChanged` 签名一模一样，靠 tag 串区分。
                name = "onLocationChanged"
                usingEqStrings("MicroMsg.SLocationListener")
            }
        },

        LOCATION_LISTENER_WGS84 to FindMethod().apply {
            matcher {
                name = "onLocationChanged"
                usingEqStrings("MicroMsg.SLocationListenerWgs84")
            }
        },

        LOCATION_DEFAULT_MANAGER to FindMethod().apply {
            matcher {
                name = "onLocationChanged"
                usingEqStrings("MicroMsg.DefaultTencentLocationManager", "[mlocationListener]error:%d, reason:%s")
            }
        },

        LOCATION_SELECT_POI_MAP to FindMethod().apply {
            searchPackages(PKG_LOCATION)
            matcher {
                usingEqStrings("MicroMsg.MMPoiMapUI", "invalid lat lng")
            }
        },

        VOICE_LENGTH to FindMethod().apply {
            matcher {
                usingEqStrings("MicroMsg.VoiceStorage", "update failed, no values set")
            }
        },

        SPORT_STEP to FindMethod().apply {
            searchPackages(PKG_SPORT)
            matcher {
                usingEqStrings("MicroMsg.Sport.DeviceStepManager", "get today step from %s todayStep %d")
            }
        },

        MOCK_SCAN to FindMethod().apply {
            matcher {
                usingEqStrings("MicroMsg.QBarStringHandler", "key_offline_scan_show_tips")
            }
        },

        MULTI_WEBVIEW to FindMethod().apply {
            matcher {
                usingEqStrings("MicroMsg.PluginHelper", "start multi webview!!!!!!!!!")
            }
        },

        SHARE_CHECK to FindMethod().apply {
            matcher {
                usingEqStrings("checkAppSignature get local signature failed")
            }
        },

        SETTING_PLUGIN_ENTRY to FindClass().apply {
            matcher {
                usingEqStrings("SettingGroup_Main_Other_Plugin")
            }
        },

        SETTING_PLUGIN_ID to FindMethod().apply {
            matcher {
                paramCount(0)
                returnType(String::class.java)
                usingEqStrings("SettingGroup_Main_Other_Plugin")
            }
        },

        SETTING_ITEM_TITLE to FindMethod().apply {
            matcher {
                // declaredClass 没有 lambda 重载，只能显式 new 一个 ClassMatcher 塞进去
                declaredClass(ClassMatcher().usingEqStrings("switch_button_type"))
                returnType(String::class.java)
                paramCount(0)
                usingEqStrings("getString(...)")
            }
        },

        PLUS_MENU_ASSEMBLE to FindMethod().apply {
            matcher {
                usingEqStrings("dyna plus config is null, we use default one")
            }
        },

        SETTING_LIST_ORDER to FindMethod().apply {
            matcher {
                usingEqStrings("rootItem", "frontSize:")
            }
        },

        ACCOUNT_CURRENT to FindMethod().apply {
            matcher {
                returnType(String::class.java)
                usingEqStrings("login_weixin_username", "login_user_name", "never_login_crash")
            }
        }
    )
}
