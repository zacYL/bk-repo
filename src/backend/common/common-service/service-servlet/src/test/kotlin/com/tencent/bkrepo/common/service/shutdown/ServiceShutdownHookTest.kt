package com.tencent.bkrepo.common.service.shutdown

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ServiceShutdownHookTest {

    @AfterEach
    fun cleanupHooks() {
        val field = ServiceShutdownHook::class.java.getDeclaredField("hookList")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(null) as MutableList<Callable<Any>>).clear()
    }

    @Test
    fun `stop waits for registered hooks to finish`() {
        val executed = CountDownLatch(1)
        ServiceShutdownHook.add(
            Callable {
                executed.countDown()
                "done"
            },
        )

        ServiceShutdownHook(ShutdownProperties(maxWaitMinute = 1)).stop()

        assertTrue(executed.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun `stop runs all hooks before returning`() {
        val counter = AtomicInteger()
        ServiceShutdownHook.add(Callable { counter.incrementAndGet(); null })
        ServiceShutdownHook.add(Callable { counter.incrementAndGet(); null })

        ServiceShutdownHook(ShutdownProperties(maxWaitMinute = 1)).stop()

        assertEquals(2, counter.get())
    }
}
