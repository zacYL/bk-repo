/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.auth.service

import com.tencent.bkrepo.auth.pojo.oauth.AuthorizeRequest
import com.tencent.bkrepo.auth.pojo.oauth.AuthorizedResult
import com.tencent.bkrepo.auth.pojo.oauth.GenerateTokenRequest
import com.tencent.bkrepo.auth.pojo.oauth.JsonWebKeySet
import com.tencent.bkrepo.auth.pojo.oauth.OauthToken
import com.tencent.bkrepo.auth.pojo.oauth.OidcConfiguration
import com.tencent.bkrepo.auth.pojo.oauth.UserInfo

/**
 * Oauth授权服务
 */
interface OauthAuthorizationService {

    /**
     * 确认授权
     */
    fun authorized(authorizeRequest: AuthorizeRequest): AuthorizedResult

    /**
     * 创建token
     */
    fun createToken(generateTokenRequest: GenerateTokenRequest)

    fun refreshToken(generateTokenRequest: GenerateTokenRequest)

    /**
     * 获取token信息
     */
    fun getToken(accessToken: String): OauthToken?

    /**
     * 验证token，验证通过返回userId
     */
    fun validateToken(accessToken: String): String?

    /**
     * 删除token
     */
    fun deleteToken(clientId: String, clientSecret: String, accessToken: String)

    /**
     * 获取用户信息
     */
    fun getUserInfo(): UserInfo

    /**
     * 获取Oidc配置
     */
    fun getOidcConfiguration(projectId: String): OidcConfiguration

    /**
     * 获取Json web key set
     */
    fun getJwks(): JsonWebKeySet
}
