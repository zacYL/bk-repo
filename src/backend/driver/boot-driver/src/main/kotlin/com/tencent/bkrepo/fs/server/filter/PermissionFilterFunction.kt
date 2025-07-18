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

package com.tencent.bkrepo.fs.server.filter

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.common.api.constant.PLATFORM_KEY
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.artifact.constant.PROJECT_ID
import com.tencent.bkrepo.common.artifact.constant.REPO_NAME
import com.tencent.bkrepo.fs.server.constant.JWT_CLAIMS_PERMIT
import com.tencent.bkrepo.fs.server.constant.JWT_CLAIMS_REPOSITORY
import com.tencent.bkrepo.fs.server.service.PermissionService
import com.tencent.bkrepo.fs.server.utils.ReactiveSecurityUtils.bearerToken
import com.tencent.bkrepo.fs.server.utils.SecurityManager
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.util.AntPathMatcher
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.buildAndAwait

class PermissionFilterFunction(
    private val securityManager: SecurityManager,
    private val permissionService: PermissionService
) : CoHandlerFilterFunction {
    private val matcher = AntPathMatcher()
    override suspend fun filter(
        request: ServerRequest,
        next: suspend (ServerRequest) -> ServerResponse,
    ): ServerResponse {
        if (uncheckedUrlPrefixList.any { request.path().startsWith(it) }) {
            return next(request)
        }
        val projectId = request.pathVariable(PROJECT_ID)
        val repoName = request.pathVariable(REPO_NAME)
        val action = request.getAction()
        val platformKey = request.exchange().attributes[PLATFORM_KEY]
        if (platformKey != null) {
            val userId = request.exchange().attributes[USER_KEY].toString()
            return if (permissionService.checkPermission(projectId, repoName, action, userId)) {
                next(request)
            } else {
                logger.info("user[$userId] no $action permission in [$projectId/$repoName]")
                ServerResponse.status(HttpStatus.FORBIDDEN).buildAndAwait()
            }
        }

        val token = request.bearerToken()
        return if (checkToken(token, request, action)) {
            next(request)
        } else {
            ServerResponse.status(HttpStatus.FORBIDDEN).buildAndAwait()
        }
    }

    private fun checkToken(
        token: String?,
        request: ServerRequest,
        action: PermissionAction,
    ): Boolean {
        return if (token == null) {
            false
        } else {
            val jws = securityManager.validateToken(token)
            val repo = jws.body[JWT_CLAIMS_REPOSITORY]
            val permit = jws.body[JWT_CLAIMS_PERMIT].toString()
            val projectId = request.pathVariable(PROJECT_ID)
            val repoName = request.pathVariable(REPO_NAME)
            val requestRepo = "$projectId/$repoName"
            return requestRepo == repo && checkAction(permit, action)
        }
    }

    private fun checkAction(permit: String, action: PermissionAction): Boolean {
        if (action == PermissionAction.READ) {
            return permit == PermissionAction.READ.name || permit == PermissionAction.WRITE.name
        }
        return permit == action.name
    }

    private fun ServerRequest.getAction(): PermissionAction {
        WRITE_REQUEST_URL_PATTERN_SET.forEach {
            if (matcher.match(it, this.path())) {
                return PermissionAction.WRITE
            }
        }
        return PermissionAction.READ
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PermissionFilterFunction::class.java)
        private val WRITE_REQUEST_URL_PATTERN_SET = arrayOf(
            "/node/change/**",
            "/node/move/**",
            "/node/create/**",
            "/node/delete/**",
            "/node/mkdir/**",
            "/node/set-length/**",
            "/block/**",
            "/stream/**"
        )
        private val uncheckedUrlPrefixList = listOf(
            "/login", "/devx/login", "/service", "/token", "/ioa", "/client/metrics/push"
        )
    }
}
