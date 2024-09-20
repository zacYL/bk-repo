/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2024 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.common.service.thread

import com.tencent.bkrepo.common.service.condition.ConditionalOnTongWeb
import com.tongweb.springboot.starter.TongWebConnectorCustomizer
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
import org.springframework.boot.task.TaskExecutorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.core.task.TaskDecorator
import org.springframework.data.util.CastUtils
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.Enumeration
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper

@Configuration(proxyBeanMethods = false)
@ConditionalOnTongWeb
class ThreadPoolTaskExecutorConfiguration {

    @Lazy
    @Bean(
        name = [
            TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME,
            AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME
        ]
    )
    @Primary
    fun applicationTaskExecutor(builder: TaskExecutorBuilder): ThreadPoolTaskExecutor {
        return builder.build().apply {
            setTaskDecorator(TaskDecorator { TaskWrapper(it) })
            afterPropertiesSet()
        }
    }

    @Bean
    fun disabledDiscardFacade(): TongWebConnectorCustomizer = TongWebConnectorCustomizer { connector ->
        connector.discardFacades = false
    }
}

private class TaskWrapper(private val delegate: Runnable) : Runnable {

    private val attributes =
        ServletRequestAttributeWrapper(CastUtils.cast(RequestContextHolder.getRequestAttributes()!!))

    override fun run() {
        RequestContextHolder.setRequestAttributes(attributes)
        try {
            delegate.run()
        } finally {
            RequestContextHolder.resetRequestAttributes()
        }
    }
}

private class ServletRequestAttributeWrapper(private val delegate: ServletRequestAttributes) :
    ServletRequestAttributes(RequestWrapper(delegate.request), delegate.response) {

    private val attributes = mapOf(
        SCOPE_REQUEST to
                delegate.getAttributeNames(SCOPE_REQUEST).associateWith { delegate.getAttribute(it, SCOPE_REQUEST) },
        SCOPE_SESSION to
                delegate.getAttributeNames(SCOPE_SESSION).associateWith { delegate.getAttribute(it, SCOPE_SESSION) }
    )

    override fun getAttribute(name: String, scope: Int) = attributes[scope]?.get(name)

    override fun setAttribute(name: String, value: Any, scope: Int) { throw UnsupportedOperationException() }

    override fun removeAttribute(name: String, scope: Int) { throw UnsupportedOperationException() }

    override fun getAttributeNames(scope: Int): Array<out String> = attributes[scope]?.keys?.toTypedArray().orEmpty()

    override fun registerDestructionCallback(name: String, callback: Runnable, scope: Int) {
        throw UnsupportedOperationException()
    }

    override fun resolveReference(key: String) = null
}

private class RequestWrapper(delegate: HttpServletRequest) : HttpServletRequestWrapper(delegate) {

    private val headers =
        super.getHeaderNames()?.toList()?.associateWith { super.getHeaders(it)?.toList().orEmpty() }.orEmpty()

    private val attributes =
        super.getAttributeNames()?.toList()?.associateWith { super.getAttribute(it) }.orEmpty().toMutableMap()

    private val cookies = super.getCookies()?.copyOf().orEmpty()

    private val remoteAddr = super.getRemoteAddr()

    private val queryString: String? = super.getQueryString()

    override fun getAttributeNames(): Enumeration<String> {
        return attributes.keys.iterator().toEnumeration()
    }

    override fun getAttribute(name: String?) = attributes[name]

    override fun getHeader(name: String): String? {
        return headers[name]?.firstOrNull()
    }

    override fun getHeaderNames(): Enumeration<String> {
        return headers.keys.iterator().toEnumeration()
    }

    override fun getDateHeader(name: String) = getHeader(name)!!.toLong()

    override fun getIntHeader(name: String) = getHeader(name)!!.toInt()

    override fun getHeaders(name: String): Enumeration<String> {
        return headers[name].orEmpty().iterator().toEnumeration()
    }

    override fun getCookies() = cookies

    override fun getRemoteAddr() = remoteAddr

    override fun getQueryString() = queryString

    override fun setAttribute(name: String, o: Any?) {
        if (o == null) removeAttribute(name) else attributes[name] = o
    }

    override fun removeAttribute(name: String) {
        attributes.remove(name)
    }
}

private fun <T> Iterator<T>.toEnumeration(): Enumeration<T> {
    return object : Enumeration<T> {
        override fun hasMoreElements(): Boolean {
            return this@toEnumeration.hasNext()
        }

        override fun nextElement(): T {
            return this@toEnumeration.next()
        }
    }
}
