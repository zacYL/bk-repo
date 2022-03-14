package com.tencent.bkrepo.scanner.util

/**
 * 生成扫描任务运行时
 */
object TaskIdUtil {

    fun build(): String {
        return System.currentTimeMillis().toString()
    }
}
