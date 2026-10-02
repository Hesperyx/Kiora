package cn.hxy.kiora.hook.wx

import cn.hxy.kiora.host.HostEnv
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Parcelable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.conf.WxLocationConfig
import cn.hxy.kiora.hook.base.BaseClickableHookItem
import cn.hxy.kiora.ui.pages.configs.wx.WxLocationPage
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.hook.hookReplace
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.reflect.findMethodOrNull
import cn.hxy.kiora.utils.reflect.toClass
import cn.hxy.kiora.utils.wx.WeChatDexKit
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * 虚拟定位（移植自 WA `LocationHook`）。
 *
 * 腾讯定位 SDK 回调时把经纬度换成配置值，从而让「发送位置」「附近的人」等
 * 都定位到指定坐标。
 *
 * ## 三步
 *
 * 1. **换坐标**：挂三个回调（国测局 / WGS84 / 默认管理器），拿到回调参数里的
 *    location 对象后，给它的 `getLatitude()` / `getLongitude()` 挂钩子返回配置值。
 *    这里**不按字段名改**（经纬度可能存在 `double` 字段里，也可能现算），
 *    而是直接替换 getter —— 对调用方完全等价，且不依赖字段布局。
 *    每个 location 类只挂一次（`hookedClasses` 去重），否则每次定位回调都会叠一层钩子。
 * 2. **选点页改坐标**：点选点页时弹输入框，当场改。
 * 3. **地图选点回填**：`RedirectUI` 选完位置后，从结果里解出 `lat x;lng y;`，
 *    写进 [pickedLocation] —— 它是 Compose 状态，配置页读到变化会自动回填输入框，
 *    配置页**不需要自己接 `onActivityResult`**。
 *
 * ## 与 WA 的差异
 *
 * - WA 用 `removeSelf()` 在挂完 getter 后把自己摘掉；这里用「类级去重集合」，
 *   语义相同但不会因为异常路径漏摘。
 * - WA 的 `targetProcess` 是「主进程 + `:appbrand0`」两个；本模块的
 *   宿主注解只能写一个进程，这里保持**主进程**（定位回调在主进程）。
 *    若真机上发现小程序内的定位没被替换，再单独为 `:appbrand0` 加一项。
 */
@HookItemAnnotation(
    tag = "虚拟定位",
    desc = "把腾讯定位 SDK 的结果替换为指定经纬度",
    category = HookCategory.MISC,
    hosts = ["wechat"]
)
object WxLocation : BaseClickableHookItem<WxLocationConfig>(WxLocationConfig.serializer()) {

    override val defaultConfig: WxLocationConfig = WxLocationConfig()

    /** 改坐标要重启宿主才彻底生效（已有定位结果会被缓存）。 */
    override val isNeedRestart: Boolean = true

    private const val REDIRECT_UI_CLASS = "com.tencent.mm.plugin.location.ui.RedirectUI"
    private const val REQUEST_CODE = 6
    private const val EXTRA_TARGET = "KLocationIntent"
    private const val EXTRA_MAP_VIEW_TYPE = "map_view_type"

    /** 微信的「选点」地图视图类型，WA 传的就是这个值。 */
    private const val MAP_VIEW_TYPE_PICK = 8

    /** 选点结果里的坐标串格式：`lat 31.1;lng 121.6;`。 */
    private val latLngRegex = Regex("lat ([-+]?[0-9]*\\.?[0-9]+);lng ([-+]?[0-9]*\\.?[0-9]+);")

    /**
     * 地图选点回填的坐标。
     *
     * 对外暴露成 Compose 状态（而不是普通字段）是**刻意的**：
     * 配置页在构图里读它，值一变就自动重组，输入框跟着刷新 ——
     * 省掉「配置页自己注册结果回调」那套样板。
     */
    var pickedLocation by mutableStateOf<Pair<Float, Float>?>(null)
        private set

    /**
     * 已经挂过经纬度 getter 的 location 类，避免重复叠钩子。
     *
     * 用并发集合而不是普通 `mutableSetOf`：三个定位回调来自不同的 SDK 监听器，
     * 不保证都在主线程；`add` 的返回值正好当「谁是第一个」的判定，
     * 换成先查再写的写法就必须加锁了。
     */
    private val hookedClasses: MutableSet<Class<*>> = ConcurrentHashMap.newKeySet()

    private var listenerMethods: List<Method> = emptyList()
    private var selectPoiMapMethod: Method? = null

    override fun onInit(): Boolean {
        listenerMethods = listOf(
            WeChatDexKit.LOCATION_LISTENER,
            WeChatDexKit.LOCATION_LISTENER_WGS84,
            WeChatDexKit.LOCATION_DEFAULT_MANAGER
        ).mapNotNull { key -> runCatching { WeChatDexKit.requireMethod(key) }.getOrNull() }

        selectPoiMapMethod = runCatching {
            WeChatDexKit.requireMethod(WeChatDexKit.LOCATION_SELECT_POI_MAP)
        }.getOrNull()

        if (listenerMethods.isEmpty()) {
            LogUtils.w("$name 未取到定位回调方法，通常是还没跑过「查找方法」")
            return false
        }
        return super.onInit()
    }

    override fun onHook() {
        listenerMethods.forEach { method ->
            method.hookBefore(this) { param ->
                val location = param.args.getOrNull(0) ?: return@hookBefore
                hookCoordinates(location)
            }
        }

        selectPoiMapMethod?.hookBefore(this) { param ->
            val view = param.args.getOrNull(0) as? View ?: return@hookBefore
            showCoordinateDialog(view)
        }

        hookRedirectUi()
    }

    /**
     * 给 location 对象的经纬度 getter 挂钩子。
     *
     * 按「类」去重：三个回调可能传同一个类的对象，重复挂会让钩子层层叠加
     * （虽然结果一样，但每次定位都多一次调用开销）。
     */
    private fun hookCoordinates(location: Any) {
        val clazz = location.javaClass
        if (!hookedClasses.add(clazz)) return

        runCatching {
            clazz.findMethodOrNull {
                name = "getLatitude"
                paramCount = 0
            }?.hookReplace(this) { config.latitude.toDouble() }

            clazz.findMethodOrNull {
                name = "getLongitude"
                paramCount = 0
            }?.hookReplace(this) { config.longitude.toDouble() }
        }.onFailure { LogUtils.e(this, it) }
    }

    /** 选点页上当场改坐标：两个输入框 + 确定/取消。 */
    private fun showCoordinateDialog(view: View) {
        val context = view.context
        val density = context.resources.displayMetrics.density
        val padding = (12 * density).toInt()

        val latitudeInput = numberInput(context, config.latitude.toString())
        val longitudeInput = numberInput(context, config.longitude.toString())

        val container = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            setPadding(padding, padding, padding, padding)
            addView(latitudeInput)
            addView(longitudeInput)
        }

        AlertDialog.Builder(context)
            .setTitle("修改经纬度")
            .setView(container)
            .setPositiveButton("确定") { _, _ ->
                val latitude = latitudeInput.text?.toString()?.toFloatOrNull()
                val longitude = longitudeInput.text?.toString()?.toFloatOrNull()
                updateConfig(
                    config.copy(
                        latitude = latitude ?: config.latitude,
                        longitude = longitude ?: config.longitude
                    )
                )
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun numberInput(context: android.content.Context, value: String): EditText =
        EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL or
                InputType.TYPE_NUMBER_FLAG_SIGNED
            setText(value)
        }

    /**
     * 打开微信自己的地图选点页。
     *
     * 从配置页调用。拿不到顶层 Activity 时什么都不做 ——
     * 比崩掉好，而且用户重开一次即可。
     */
    fun openMapPicker() {
        val activity = HostEnv.activity ?: run {
            LogUtils.w("$name 拿不到顶层 Activity，无法打开地图选点")
            return
        }
        val redirectUi = runCatching { REDIRECT_UI_CLASS.toClass }.getOrNull() ?: run {
            LogUtils.w("$name 未找到 $REDIRECT_UI_CLASS")
            return
        }

        activity.startActivityForResult(
            Intent(activity, redirectUi).apply { putExtra(EXTRA_MAP_VIEW_TYPE, MAP_VIEW_TYPE_PICK) },
            REQUEST_CODE
        )
    }

    /**
     * 解析选点结果。
     *
     * `onActivityResult` 的第 3 个参数里带着一个 `KLocationIntent` 包，
     * 它的某个无参 String getter 返回 `lat 31.1;lng 121.6;` 这种串 ——
     * 用正则抠出来。取不到无参 String 方法时按 WA 的做法「取第一个返回
     * String 的方法」，保持与 WA 相同的容忍度。
     */
    private fun hookRedirectUi() {
        val clazz = runCatching { REDIRECT_UI_CLASS.toClass }.getOrNull() ?: return

        val onActivityResult = clazz.findMethodOrNull {
            name = "onActivityResult"
            paramTypes(Integer.TYPE, Integer.TYPE, Intent::class.java)
        }
        if (onActivityResult == null) {
            LogUtils.w("$name $REDIRECT_UI_CLASS 没有 onActivityResult，地图选点回填不可用")
            return
        }

        onActivityResult.hookAfter(this) { param ->
            if (param.args.getOrNull(0) != REQUEST_CODE) return@hookAfter
            if (param.args.getOrNull(1) != Activity.RESULT_OK) return@hookAfter

            val intent = param.args.getOrNull(2) as? Intent ?: return@hookAfter
            @Suppress("DEPRECATION")
            val payload = intent.getParcelableExtra(EXTRA_TARGET) as? Parcelable ?: return@hookAfter

            val text = runCatching {
                payload.javaClass.findMethodOrNull {
                    returnType = String::class.java
                    paramCount = 0
                }?.invoke(payload) as? String
            }.getOrNull() ?: return@hookAfter

            val match = latLngRegex.find(text) ?: return@hookAfter
            val latitude = match.groupValues[1].toFloatOrNull()
            val longitude = match.groupValues[2].toFloatOrNull()
            if (latitude != null && longitude != null) {
                pickedLocation = latitude to longitude
            }
        }
    }

    @Composable
    override fun ConfigContent(onDismiss: () -> Unit) {
        WxLocationPage(config, ::updateConfig, onDismiss)
    }
}
