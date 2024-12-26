package com.tencent.bkrepo.common.service.condition

import com.tencent.bkrepo.common.api.pojo.AuthRealm
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

class OnDevopsCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata) =
        context.environment.getProperty("auth.realm", AuthRealm.DEVOPS.value) == AuthRealm.DEVOPS.value
}
