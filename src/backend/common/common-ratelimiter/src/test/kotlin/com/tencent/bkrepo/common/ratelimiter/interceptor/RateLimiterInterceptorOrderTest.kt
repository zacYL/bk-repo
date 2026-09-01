package com.tencent.bkrepo.common.ratelimiter.interceptor

import com.tencent.bkrepo.common.ratelimiter.RateLimiterAutoConfiguration
import com.tencent.bkrepo.common.ratelimiter.service.RequestLimitCheckService
import com.tencent.bkrepo.common.ratelimiter.service.connection.ServiceInstanceConnectionLimiterService
import com.tencent.bkrepo.common.ratelimiter.service.connection.UserConcurrentConnectionLimiterService
import com.tencent.bkrepo.common.ratelimiter.service.concurrent.UrlConcurrentRequestLimiterService
import com.tencent.bkrepo.common.ratelimiter.service.concurrent.UserUrlConcurrentRequestLimiterService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ObjectProvider
import org.springframework.core.Ordered
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * 验证 RateLimiterAutoConfiguration 注册的拦截器 order 与架构文档一致：
 * - 非用户并发 / 非用户频率：HIGHEST_PRECEDENCE（鉴权前）
 * - 用户频率：1（HttpAuthInterceptor 默认 0 之后）
 * - 用户并发：2
 */
class RateLimiterInterceptorOrderTest {

    @Test
    fun `rate limiter interceptors use documented order relative to HttpAuthInterceptor`() {
        val config = RateLimiterAutoConfiguration()
        val requestLimitCheckService = mockk<RequestLimitCheckService>()
        val connectionLimiterService = mockk<ServiceInstanceConnectionLimiterService>()
        val userConnectionProvider = mockk<ObjectProvider<UserConcurrentConnectionLimiterService>>()
        val urlConcurrentProvider = mockk<ObjectProvider<UrlConcurrentRequestLimiterService>>()
        val userUrlConcurrentProvider = mockk<ObjectProvider<UserUrlConcurrentRequestLimiterService>>()

        every { userConnectionProvider.getIfAvailable() } returns mockk()
        every { urlConcurrentProvider.getIfAvailable() } returns null
        every { userUrlConcurrentProvider.getIfAvailable() } returns null

        val configurer = config.rateLimitHandlerInterceptorRegister(
            requestLimitCheckService,
            connectionLimiterService,
            userConnectionProvider,
            urlConcurrentProvider,
            userUrlConcurrentProvider,
        )

        val captured = captureInterceptors(configurer)
        assertEquals(4, captured.size)

        val nonUserConcurrency = captured.filter { it.interceptor is ConcurrencyLimitInterceptor }.first()
        assertEquals(Ordered.HIGHEST_PRECEDENCE, nonUserConcurrency.order)

        val nonUserRate = captured.single { it.interceptor is NonUserRateLimitHandlerInterceptor }
        assertEquals(Ordered.HIGHEST_PRECEDENCE, nonUserRate.order)

        val userRate = captured.single { it.interceptor is UserRateLimitHandlerInterceptor }
        assertEquals(1, userRate.order)

        val userConcurrency = captured.filter { it.interceptor is ConcurrencyLimitInterceptor }.last()
        assertEquals(2, userConcurrency.order)

        assertTrue(userRate.order > nonUserRate.order)
        assertTrue(userConcurrency.order > userRate.order)
    }

    private data class CapturedRegistration(
        val interceptor: HandlerInterceptor,
        val order: Int,
    )

    private fun captureInterceptors(configurer: WebMvcConfigurer): List<CapturedRegistration> {
        val registry = InterceptorRegistry()
        configurer.addInterceptors(registry)

        val registrationsField = InterceptorRegistry::class.java.getDeclaredField("registrations")
        registrationsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val registrations = registrationsField.get(registry) as List<Any>

        return registrations.map { registration ->
            val registrationClass = registration.javaClass
            val interceptorField = registrationClass.getDeclaredField("interceptor")
            interceptorField.isAccessible = true
            val orderField = registrationClass.getDeclaredField("order")
            orderField.isAccessible = true
            CapturedRegistration(
                interceptor = interceptorField.get(registration) as HandlerInterceptor,
                order = orderField.getInt(registration),
            )
        }
    }
}
