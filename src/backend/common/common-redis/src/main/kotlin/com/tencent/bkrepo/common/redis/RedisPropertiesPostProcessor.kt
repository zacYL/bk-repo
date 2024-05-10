package com.tencent.bkrepo.common.redis

import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.autoconfigure.data.redis.RedisProperties

class RedisPropertiesPostProcessor : BeanPostProcessor {

    @Throws(BeansException::class)
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is RedisProperties && bean.sentinel != null && bean.sentinel.master.isNullOrBlank()) {
            bean.sentinel = null
        }
        return bean
    }
}
