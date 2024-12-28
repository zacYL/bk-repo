
package com.tencent.bkrepo.cocoapods.pool

import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 事件处理线程池
 */
object EventHandlerThreadPoolExecutor {
    /**
     * 线程池实例
     */
    val instance: ThreadPoolExecutor = buildThreadPoolExecutor()

    /**
     * 创建线程池
     */
    private fun buildThreadPoolExecutor(): ThreadPoolExecutor {
        val namedThreadFactory = ThreadFactoryBuilder().setNameFormat("cocoapods-event-worker-%d").build()
        return ThreadPoolExecutor(
            20, 100, 30, TimeUnit.SECONDS,
            LinkedBlockingQueue(1024), namedThreadFactory, ThreadPoolExecutor.CallerRunsPolicy()
        )
    }
}
