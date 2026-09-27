package cn.hxy.kiora.hook.social

import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.reflect.ClassUtils
import java.lang.reflect.Method
import java.lang.reflect.Modifier

@HookItemAnnotation(
    "移除媒体选择数量限制",
    "移除聊天页相册最多选20张、空间上传最多选50张图片/视频的限制",
    HookCategory.SOCIAL
)
object RemoveMediaLimit : BaseSwitchHookItem() {

    /** 聊天页相册与空间相册的可选性查询类；任一缺失即认为本功能无落点。 */
    private const val CHAT_ALBUM_VM = "com.tencent.qqnt.qbasealbum.select.viewmodel.SelectedMediaViewModel"
    private const val WINK_ALBUM_VM = "com.tencent.mobileqq.wink.picker.core.viewmodel.WinkSelectedMediaViewModel"

    /** 空间侧的两个附加限制点，缺失时只跳过自身，不影响整体可用性。 */
    private const val QZONE_ALBUM_VM = "com.tencent.mobileqq.wink.picker.qzone.viewmodel.QZoneSelectedMediaViewModel"
    private const val QZONE_CONFIG = "common.config.service.QzoneConfig"

    private const val QZONE_CONFIG_GROUP = "PublishMood"
    private const val QZONE_CONFIG_KEY = "MoodPhotoMaxNum"
    private const val MEDIA_MAX_NUM = 114514

    private const val QZONE_SELECTED_SIZE_METHOD = "getCurSelectedSize"

    private lateinit var chatAlbumVm: Class<*>
    private lateinit var winkAlbumVm: Class<*>

    override fun onInit(): Boolean {
        // 两个主目标缺一即不可用：宿主版本对不上时设置页会直接显示为不可用，
        // 避免“开关打得开、钩子其实没装上”的静默失效。
        chatAlbumVm = ClassUtils.loadClassOrNull(CHAT_ALBUM_VM) ?: return false
        winkAlbumVm = ClassUtils.loadClassOrNull(WINK_ALBUM_VM) ?: return false
        return super.onInit()
    }

    override fun onHook() {
        unlockSelectable(chatAlbumVm)
        unlockSelectable(winkAlbumVm)
        unlockQzoneNextStep()
        unlockQzoneUploadCount()
    }

    /**
     * 让「还能不能再选」恒为真。
     *
     * 沿继承链查找：宿主把该判断收归父类时，只扫 `declaredMethods` 会一无所获。
     */
    private fun unlockSelectable(cls: Class<*>) {
        val targets = cls.lookupChain()
            .flatMap { it.declaredMethods.toList() }
            .filter { it.isSelectableQuery() }
            .distinctBy { it.name }
            .toList()

        if (targets.isEmpty()) {
            LogUtils.e(this, IllegalStateException("${cls.name} 未找到可选性查询方法，限制未解除"))
            return
        }

        targets.forEach { method ->
            method.hookBefore(this) { param -> param.result = true }
        }
        LogUtils.i("${name}: ${cls.simpleName} 已解锁 ${targets.joinToString { it.name }}")
    }

    /** 空间「下一步」的点击限制：把已选数量报成 1，绕过前端数量校验。 */
    private fun unlockQzoneNextStep() {
        val cls = ClassUtils.loadClassOrNull(QZONE_ALBUM_VM) ?: return
        runCatching {
            cls.lookupChain()
                .flatMap { it.declaredMethods.toList() }
                .firstOrNull { it.name == QZONE_SELECTED_SIZE_METHOD && it.parameterCount == 0 }
                ?.hookBefore(this) { param -> param.result = 1 }
        }.onFailure { LogUtils.e(this, it) }
    }

    /** 空间上传的数量上限配置项。 */
    private fun unlockQzoneUploadCount() {
        val cls = ClassUtils.loadClassOrNull(QZONE_CONFIG) ?: return
        runCatching {
            cls.lookupChain()
                .flatMap { it.declaredMethods.toList() }
                .firstOrNull { it.matchesQzoneGetConfig() }
                ?.hookBefore(this) { param ->
                    val group = param.args.getOrNull(0) as? String
                    val key = param.args.getOrNull(1) as? String
                    if (group == QZONE_CONFIG_GROUP && key == QZONE_CONFIG_KEY) {
                        param.result = MEDIA_MAX_NUM
                    }
                }
        }.onFailure { LogUtils.e(this, it) }
    }

    private fun Method.isSelectableQuery(): Boolean =
        Modifier.isPublic(modifiers) &&
                !Modifier.isStatic(modifiers) &&
                parameterCount == 0 &&
                (returnType == Boolean::class.javaPrimitiveType || returnType == Boolean::class.javaObjectType)

    private fun Method.matchesQzoneGetConfig(): Boolean =
        name == "getConfig" &&
                parameterTypes.contentEquals(
                    arrayOf(String::class.java, String::class.java, Int::class.javaPrimitiveType)
                )

    private fun Class<*>.lookupChain(): Sequence<Class<*>> =
        generateSequence(this) { it.superclass }
            .takeWhile { it != Any::class.java }
}
