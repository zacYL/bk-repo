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

package com.tencent.bkrepo.proxy.security

import com.tencent.bkrepo.common.artifact.constant.PROJECT_ID
import com.tencent.bkrepo.common.artifact.constant.REPO_NAME
import com.tencent.bkrepo.common.artifact.exception.RepoNotFoundException
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.service.proxy.ProxyEnv
import com.tencent.bkrepo.common.service.proxy.ProxyFeignClientFactory
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.storage.innercos.http.HttpMethod
import com.tencent.bkrepo.repository.api.proxy.ProxyRepositoryClient
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.web.servlet.HandlerMapping

/**
 * 校验项目是否和Proxy所属项目一致
 */
@Aspect
class ProxyProjectPermissionAspect {

    private val proxyRepositoryClient: ProxyRepositoryClient by lazy {
        ProxyFeignClientFactory.create("repository")
    }

    @Around(
        "@within(org.springframework.web.bind.annotation.RestController) " +
            "|| @annotation(org.springframework.web.bind.annotation.RestController)"
    )
    fun checkProject(point: ProceedingJoinPoint): Any? {
        val request = HttpContextHolder.getRequest()
        val uriAttribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)
        require(uriAttribute is Map<*, *>)
        val projectId = uriAttribute[PROJECT_ID]?.toString()
        val repoName = uriAttribute[REPO_NAME]?.toString()
        val proxyProjectId = ProxyEnv.getProjectId()
        if (request.method.equals(HttpMethod.PUT.name, true)) {
            if (!projectId.isNullOrBlank() && projectId != proxyProjectId) {
                throw PermissionException()
            }
        }
        if (request.method.equals(HttpMethod.GET.name, true)) {
            if (projectId.isNullOrBlank() || repoName.isNullOrBlank()) {
                throw IllegalArgumentException("$projectId/$repoName")
            }
            val repoDetail = proxyRepositoryClient.getRepoDetail(projectId, repoName).data
                ?: throw RepoNotFoundException("$projectId/$repoName")
            if (!repoDetail.public && projectId != proxyProjectId) {
                throw PermissionException()
            }
        }
        return point.proceed()
    }
}
