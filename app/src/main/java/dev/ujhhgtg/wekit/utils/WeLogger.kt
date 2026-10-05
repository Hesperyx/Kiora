package dev.ujhhgtg.wekit.utils

import cn.hxy.kiora.utils.log.LogUtils

/**
 * WeKit 血统代码的日志入口。
 *
 * WeKit 原版是一个带异步队列 + 按天滚动落盘 + 丢弃计数的完整实现；
 * 迁进 Kiora 后直接转接到 Kiora 的 [LogUtils]，不再维护第二套日志落盘体系。
 *
 * **已知简化**：WeKit 原版的日志文件查看器（`WeLogger.logsDir` / `allLogFiles` /
 * `flush`）在切片里没有对应实现。若后续要迁「日志查看」相关功能，需要补回。
 */
object WeLogger {

    fun v(tag: String?, msg: String) = LogUtils.d("$tag: $msg")

    fun d(tag: String?, msg: String) = LogUtils.d("$tag: $msg")

    fun i(tag: String?, msg: String) = LogUtils.i("$tag: $msg")

    fun w(tag: String?, msg: String) = LogUtils.w("$tag: $msg")

    fun e(tag: String?, msg: String) = LogUtils.w("$tag: $msg")

    fun v(tag: String?, msg: String, t: Throwable) = LogUtils.w("$tag: $msg", t)

    fun d(tag: String?, msg: String, t: Throwable) = LogUtils.w("$tag: $msg", t)

    fun i(tag: String?, msg: String, t: Throwable) = LogUtils.w("$tag: $msg", t)

    fun w(tag: String?, msg: String, t: Throwable) = LogUtils.w("$tag: $msg", t)

    fun e(tag: String?, msg: String, t: Throwable) = LogUtils.e(tag.orEmpty(), msg, t)

    /** WeKit 的「超长日志分块」能力，这里直接透传。 */
    fun logChunkedI(tag: String, msg: String) = LogUtils.i("$tag: $msg")

    fun logChunkedD(tag: String, msg: String) = LogUtils.d("$tag: $msg")

    /** 原版 WeLogger 的 flush（异步文件 logger 落盘）。Kiora 版直接转发 LogUtils 是同步的，无需 flush。 */
    fun flush() {}

    /** 当前线程调用栈（去掉 getter 自身两帧）。原版 WeLogger 的属性，供崩溃上报用。 */
    val currentStackTrace: String
        get() = Thread.currentThread().stackTrace
            .drop(2)
            .joinToString(separator = "\n") { element ->
                "at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})"
            }
}
