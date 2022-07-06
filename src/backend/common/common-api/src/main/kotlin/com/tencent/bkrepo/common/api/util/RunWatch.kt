package com.tencent.bkrepo.common.api.util

import org.slf4j.Logger

/**
 * @param [f] 可以是一个函数，也可以是一个lambda表达式
 * @param [desc] 指定任务描述信息
 * @param [logger] 记录日志
 * @return [T] 是函数的返回
 */
fun <T> runWatch(logger: Logger, desc: String? = null, f: () -> T): T {
    val start = System.currentTimeMillis()
    val result = f()
    logger.info("$desc cost: ${System.currentTimeMillis() - start}ms")
    return result
}

/**
 * @param [f] 可以是一个函数，也可以是一个lambda表达式
 * @return [T] 是函数的返回
 * @return [Long] 函数执行耗时的返回值
 */
fun <T> runWatch(f: () -> T): Pair<T, Long> {
    val start = System.currentTimeMillis()
    return Pair(f(), System.currentTimeMillis() - start)
}
