package dev.ujhhgtg.wekit.utils.reflection

import cn.hxy.kiora.utils.reflect.ClassUtils

/**
 * WeKit 的 ClassLoader 入口。
 *
 * 原版区分 `BOOT` / `MODULE` / `HOST` / `PARASITIC` 等多个 loader；切片只需要宿主
 * 这一个（DexKit 描述符最终都落在宿主类上），其余按需再补。
 */
object ClassLoaders {

    val HOST: ClassLoader get() = ClassUtils.hostClassLoader
}
