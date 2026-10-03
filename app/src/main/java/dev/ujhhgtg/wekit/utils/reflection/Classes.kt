package dev.ujhhgtg.wekit.utils.reflection

/**
 * 常用原始类型的 `Class` 常量，供 DexKit matcher 的 `paramTypes` / `returnType`
 * 使用。与 WeKit 原版一致（原版还有 long/float/double/char 等，按需补充）。
 */
inline val int: Class<Int> get() = Int::class.javaPrimitiveType!!

inline val bool: Class<Boolean> get() = Boolean::class.javaPrimitiveType!!
