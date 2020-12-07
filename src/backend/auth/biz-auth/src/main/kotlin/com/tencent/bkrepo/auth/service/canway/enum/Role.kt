package com.tencent.bkrepo.auth.service.canway.enum

import com.tencent.bkrepo.auth.service.canway.REPO_ADMIN
import com.tencent.bkrepo.auth.service.canway.REPO_USER
import com.tencent.bkrepo.auth.service.canway.REPO_VIEWER
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode

enum class Role(val value: String) {
    ADMIN("repo_admin"),
    USER("repo_user"),
    VIEWER("repo_viewer");

    fun nickName(): String {
        return when (value) {
            "repo_admin" -> REPO_ADMIN
            "repo_user" -> REPO_USER
            "repo_viewer" -> REPO_VIEWER
            else -> throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID,"Can not found Role name:$name")
        }
    }
}
