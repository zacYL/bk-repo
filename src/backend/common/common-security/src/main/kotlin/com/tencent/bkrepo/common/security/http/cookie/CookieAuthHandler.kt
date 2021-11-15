/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.security.http.cookie

import com.tencent.bkrepo.auth.constant.BKREPO_TICKET
import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.api.constant.CharPool.COLON
import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.PLATFORM_KEY
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.security.constant.AUTH_HEADER_UID
import com.tencent.bkrepo.common.security.constant.BEARER_AUTH_PREFIX
import com.tencent.bkrepo.common.security.constant.PLATFORM_AUTH_PREFIX
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.security.http.core.HttpAuthHandler
import com.tencent.bkrepo.common.security.http.credentials.AnonymousCredentials
import com.tencent.bkrepo.common.security.http.credentials.HttpAuthCredentials
import com.tencent.bkrepo.common.security.http.jwt.JwtAuthCredentials
import com.tencent.bkrepo.common.security.http.jwt.JwtAuthProperties
import com.tencent.bkrepo.common.security.manager.AuthenticationManager
import com.tencent.bkrepo.common.security.util.JwtUtils
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.Base64
import javax.servlet.http.HttpServletRequest

/**
 * 平台账号认证
 */
open class CookieAuthHandler(properties: JwtAuthProperties) : HttpAuthHandler {

    private val signingKey = JwtUtils.createSigningKey(properties.secretKey)

    override fun extractAuthCredentials(request: HttpServletRequest): HttpAuthCredentials {
        val cookies = request.cookies?: return AnonymousCredentials()
        for (cookie in cookies) {
            return if (cookie.name == BKREPO_TICKET) {
                CookieAuthCredentials(cookie.value)
            } else AnonymousCredentials()
        }
        return AnonymousCredentials()
    }

    override fun onAuthenticate(request: HttpServletRequest, authCredentials: HttpAuthCredentials): String {
        require(authCredentials is CookieAuthCredentials)
        try {
            return JwtUtils.validateToken(signingKey, authCredentials.token).body.subject
        } catch (exception: ExpiredJwtException) {
            throw AuthenticationException("Expired token")
        } catch (exception: JwtException) {
            throw AuthenticationException("Invalid token")
        } catch (exception: IllegalArgumentException) {
            throw AuthenticationException("Empty token")
        }
    }
}
