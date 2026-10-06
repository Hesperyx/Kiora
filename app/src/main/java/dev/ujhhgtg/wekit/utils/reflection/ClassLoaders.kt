package dev.ujhhgtg.wekit.utils.reflection

import android.content.Context
import cn.hxy.kiora.utils.reflect.ClassUtils
import dev.ujhhgtg.wekit.loader.utils.HybridClassLoader

/**
 * WeKit 的 ClassLoader 入口，四个 loader 与上游一一对应。
 */
object ClassLoaders {

    val HOST: ClassLoader get() = ClassUtils.hostClassLoader

    /** 模块自身（WeKit 血统代码）的 ClassLoader，与上游 `ClassLoaders.javaClass.classLoader` 同义。 */
    val MODULE: ClassLoader get() = ClassLoaders::class.java.classLoader!!

    /**
     * 引导 ClassLoader，与上游 `Context::class.java.classLoader` 同义。
     *
     * [HybridClassLoader] 以它为父，`BOOT.` 前缀路由的目标。
     */
    val BOOT: ClassLoader get() = Context::class.java.classLoader!!

    /** 前缀路由 ClassLoader；在 Kiora 中只有 [HybridClassLoader.additionalLoaders] 路径可用。 */
    val HYBRID: ClassLoader get() = HybridClassLoader
}
