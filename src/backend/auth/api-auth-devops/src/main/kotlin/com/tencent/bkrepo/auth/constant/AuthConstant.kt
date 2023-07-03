package com.tencent.bkrepo.auth.constant

object AuthConstant {
    const val ANY_RESOURCE_CODE = "*"

    const val TOKEN_EXPIRE_TIME_IN_SECOND = 2 * 60 * 60L

    const val TOKEN_EXPIRE_TIME_IN_MS = TOKEN_EXPIRE_TIME_IN_SECOND * 1000L

    const val DEVOPS_AUTH_NAME = "\${service.devops-prefix:}auth\${service.suffix:}"

    const val DEVOPS_USER_NAME = "\${service.devops-prefix:}usermanager\${service.suffix:}"

    const val AUTH_HEADER_USER_ID = "X-DEVOPS-UID"

    const val AUTH_HEADER_PROJECT_ID = "X-DEVOPS-PROJECT-ID"

    const val CANWAY_AUTH_SERVICE = "/api/extAuth"
}
