package com.tencent.bkrepo.auth.constant

object AuthConstant {
    const val ANY_RESOURCE_CODE = "*"

    const val X_DEVOPS_TOKEN = "X-DEVOPS-TOKEN"

    const val USER_TOKEN_REDIS_PREFIX = "devops:auth:user:"

    const val TOKEN_USER_REDIS_PREFIX = "devops:auth:token:"

    const val LOGIN_TOKEN_USER_REDIS_PREFIX = "devops:auth:login:token:"

    const val DEVOPS_TOKEN_LOCK = "devops_token_lock_"

    const val DEVOPS_TENANT = "devops_tenant_"

    const val DEVOPS_TOKEN = "devops_token_"

    const val TOKEN_EXPIRE_TIME_IN_SECOND = 2 * 60 * 60L

    const val TOKEN_EXPIRE_TIME_IN_MS = TOKEN_EXPIRE_TIME_IN_SECOND * 1000L

    const val DEVOPS_AUTH_NAME = "auth\${service-suffix:}"

    const val AUTH_HEADER_USER_ID = "X-DEVOPS-UID"

    const val AUTH_HEADER_PROJECT_ID = "X-DEVOPS-PROJECT-ID"

    const val CANWAY_AUTH_SERVICE = "/api/extAuth"
}
