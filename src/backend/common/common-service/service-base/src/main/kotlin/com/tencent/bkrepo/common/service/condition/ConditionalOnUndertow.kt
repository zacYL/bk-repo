package com.tencent.bkrepo.common.service.condition

import org.springframework.context.annotation.Conditional

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Conditional(OnUndertowCondition::class)
annotation class ConditionalOnUndertow
