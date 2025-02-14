package com.tencent.bkrepo.common.security.http.devops

import com.tencent.bkrepo.common.devops.DEPLOY_CANWAY
import com.tencent.bkrepo.common.security.http.core.HttpAuthSecurity
import com.tencent.bkrepo.common.security.http.core.HttpAuthSecurityCustomizer
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnExpression("\${security.auth.devopsToken.enabled:false} && '\${auth.realm}' == '$DEPLOY_CANWAY'")
class AccessTokenAuthConfiguration {

    @Bean
    fun accessTokenAuthSecurityCustomizer(): HttpAuthSecurityCustomizer {
        return object : HttpAuthSecurityCustomizer {
            override fun customize(httpAuthSecurity: HttpAuthSecurity) {
                httpAuthSecurity.enableDevopsAccessTokenAuth()
            }
        }
    }
}
