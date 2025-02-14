package com.tencent.bkrepo.common.redis

import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Sentinel

class RedisPropertiesPostProcessor : BeanPostProcessor {

    @Throws(BeansException::class)
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is RedisProperties && bean.sentinel != null && disableSentinel(bean.sentinel)) {
            bean.sentinel = null
        }
        return bean
    }

    private fun disableSentinel(sentinel: Sentinel): Boolean {
        return sentinel.master.isNullOrBlank() || sentinel.password.isNullOrBlank()
    }
}
