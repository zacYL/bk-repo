package com.tencent.bkrepo.common.security.http.devops

import com.tencent.bkrepo.common.api.constant.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.bkrepo.common.api.constant.AUTH_HEADER_DEVOPS_UID
import com.tencent.bkrepo.common.api.constant.CPACK_PRODUCT_CODE
import com.tencent.bkrepo.common.security.constant.ACCESS_FROM_WEB
import com.tencent.bkrepo.common.security.constant.HEADER_API_TYPE
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.security.http.core.HttpAuthHandler
import com.tencent.bkrepo.common.security.http.credentials.AnonymousCredentials
import com.tencent.bkrepo.common.security.http.credentials.HttpAuthCredentials
import com.tencent.bkrepo.common.security.manager.AuthenticationManager
import javax.servlet.http.HttpServletRequest

class AccessTokenAuthHandler(
    private val authenticationManager: AuthenticationManager,
) : HttpAuthHandler {

    override fun extractAuthCredentials(request: HttpServletRequest): HttpAuthCredentials {
        val accessFromWeb = request.getHeader(HEADER_API_TYPE) == ACCESS_FROM_WEB
        val authorizationHeader = request.getHeader(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)?.ifBlank { null }
        return if (accessFromWeb || authorizationHeader == null) {
            AnonymousCredentials()
        } else {
            val userId = request.getHeader(AUTH_HEADER_DEVOPS_UID)?.ifBlank { null }
            AccessTokenAuthCredentials(userId, authorizationHeader)
        }
    }

    override fun onAuthenticate(request: HttpServletRequest, authCredentials: HttpAuthCredentials): String {
        require(authCredentials is AccessTokenAuthCredentials)
        val (userId, token) = authCredentials
        val tokenDetail = authenticationManager.getDevopsAccessTokenDetail(token)
        if (tokenDetail == null || userId != null && userId != tokenDetail.userId) {
            throw AuthenticationException("Personal access token check failed")
        }
        if (!tokenDetail.tokenScope.contains(CPACK_PRODUCT_CODE)) {
            throw PermissionException()
        }
        return tokenDetail.userId
    }
}
