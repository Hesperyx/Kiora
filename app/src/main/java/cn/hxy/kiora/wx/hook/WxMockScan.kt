package cn.hxy.kiora.wx.hook

import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.wx.util.WeChatDexKit

/**
 * 模拟相机扫码（移植自 WA `MockScanHook`）。
 *
 * 有些二维码接口按「识别来源」给不同结果（微信扫一扫 / 相册识别 / 长按识别）。
 * 这里把「相册扫码」「长按图片识别」伪造成「微信扫一扫」再交给下游。
 *
 * ## 下标按参数个数分支，不能写死
 *
 * `Lv74/v;->g(...)` 在 8.0.78 上是 **16 个参数**，WA 走 `(3, 4)` 分支
 * （§17.2 已核验 `args(3)/args(4)` 均为 `I`）。但 WA 同时保留了 15 参时的
 * `(2, 3)` —— 参数表一变，下标整体挪一位。所以这里保留分支，
 * 并且**只在参数个数恰好是 16 或 15 时动手**，其他形态一律放行：
 * 与其猜下标，不如不动。
 */
@HookItemAnnotation(
    tag = "模拟相机扫码",
    desc = "把相册扫码、长按识别的结果按微信扫一扫的口径处理",
    category = HookCategory.MISC,
    hosts = ["wechat"]
)
object WxMockScan : BaseSwitchHookItem() {

    /**
     * 识别场景：`source` 与 `a8KeyScene` 的组合。
     *
     * 微信自己用这一对值给下游传递「从哪来的」，所以两个都要改 ——
     * 只改一个会造出一个现实中不存在的组合。
     */
    private enum class ScanScene(val source: Int, val a8KeyScene: Int) {
        /** 微信扫一扫识别。 */
        WECHAT_SCAN(0, 4),

        /** 手机相册扫码识别。 */
        ALBUM_SCAN(1, 34),

        /** 长按图片识别。 */
        LONG_PRESS_SCAN(4, 37)
    }

    /** 期望的参数表形态 → `(source 下标, a8KeyScene 下标)`。 */
    private val indexPairs = mapOf(16 to (3 to 4), 15 to (2 to 3))

    override fun onInit(): Boolean =
        runCatching { WeChatDexKit.requireMethod(WeChatDexKit.MOCK_SCAN) }
            .onFailure { LogUtils.w("$name 未取到扫码入口方法，通常是还没跑过「查找方法」") }
            .isSuccess

    override fun onHook() {
        WeChatDexKit.requireMethod(WeChatDexKit.MOCK_SCAN).hookBefore(this) { param ->
            val (sourceIndex, sceneIndex) = indexPairs[param.args.size] ?: return@hookBefore

            val source = param.args.getOrNull(sourceIndex) as? Int ?: return@hookBefore
            val a8KeyScene = param.args.getOrNull(sceneIndex) as? Int ?: return@hookBefore

            val scene = ScanScene.entries.firstOrNull {
                it.source == source && it.a8KeyScene == a8KeyScene
            } ?: return@hookBefore

            // 只伪造「相册识别」与「长按识别」；本来就是扫一扫时不动
            if (scene == ScanScene.WECHAT_SCAN) return@hookBefore

            param.args[sourceIndex] = ScanScene.WECHAT_SCAN.source
            param.args[sceneIndex] = ScanScene.WECHAT_SCAN.a8KeyScene
        }
    }
}
