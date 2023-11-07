package com.tencent.bkrepo.auth.constant

object AuthConstant {
    const val ANY_RESOURCE_CODE = "*"

    const val DEVOPS_AUTH_NAME = "\${service.devops-prefix:}auth\${service.suffix:}"

    const val DEVOPS_USER_NAME = "\${service.devops-prefix:}usermanager\${service.suffix:}"

    const val DEVOPS_PROJECT_NAME = "\${service.devops-prefix:}projectmanager\${service.suffix:}"

    const val AUTH_HEADER_USER_ID = "X-DEVOPS-UID"

    const val AUTH_HEADER_PROJECT_ID = "X-DEVOPS-PROJECT-ID"

    const val CANWAY_AUTH_SERVICE = "/api/extAuth"

    const val CPACK_VIEWERS = "制品库查看者"

    const val CPACK_USER = "制品库使用者"

    const val CPACK_MANAGER = "制品库管理者"

    const val SCOPECODE = "project"

    const val SUBJECTCODE = "ROLE"
}
