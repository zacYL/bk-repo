package com.tencent.bkrepo.repository.job.clean

import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object CleanThreadPoolExecutor {
    /**
     * 线程池实例
     */
    val instance: ThreadPoolExecutor = buildThreadPoolExecutor()

    /**
     * 创建线程池
     */
    private fun buildThreadPoolExecutor(): ThreadPoolExecutor {
        val namedThreadFactory = ThreadFactoryBuilder().setNameFormat("clean-worker-%d").build()
        return ThreadPoolExecutor(
            20, 100, 30, TimeUnit.SECONDS,
            ArrayBlockingQueue(10), namedThreadFactory, ThreadPoolExecutor.AbortPolicy()
        )
    }
}