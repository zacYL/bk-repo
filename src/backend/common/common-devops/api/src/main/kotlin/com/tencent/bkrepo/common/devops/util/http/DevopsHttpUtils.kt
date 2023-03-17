package com.tencent.bkrepo.common.devops.util.http

import com.tencent.bkrepo.common.devops.pojo.CertType
import com.tencent.bkrepo.common.service.util.HttpContextHolder

object DevopsHttpUtils {
    fun getBkToken(): String? {
        val request = HttpContextHolder.getRequest()
        val cookies = request.cookies
        if (cookies != null) {
            for (cookie in cookies) {
                if (cookie.name == CertType.TOKEN.value) return cookie.value
            }
        }
        return null
    }
}
