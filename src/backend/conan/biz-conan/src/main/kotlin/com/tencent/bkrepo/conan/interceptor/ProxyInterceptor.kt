/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.conan.interceptor

import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.storage.innercos.http.HttpMethod
import com.tencent.bkrepo.conan.service.ConanRemoteService
import com.tencent.bkrepo.conan.service.ConanVirtualService
import org.springframework.web.servlet.HandlerInterceptor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ProxyInterceptor(
    private val conanRemoteService: ConanRemoteService,
    private val conanVirtualService: ConanVirtualService
) : HandlerInterceptor {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        with(ArtifactContextHolder.getRepoDetail()!!) {
            if (category == RepositoryCategory.REMOTE && request.method == HttpMethod.GET.name) {
                return if (isDownFilePath(request)) {
                    //下载请求不拦截
                    true
                } else {
                    conanRemoteService.proxyRequestToRemote(this, response)
                    false
                }
            }
            if (category == RepositoryCategory.VIRTUAL) {
                return if (isSearchPath(request)) {
                    //搜索请求不拦截
                    true
                } else {
                    val repoName = conanVirtualService.getCacheRepo(this, request)
                    val mutableRequest = MutableHttpServletRequest(request)
                    val originalUri = request.requestURI
                    val modifiedUri = originalUri.replace(name, repoName)

                    mutableRequest.setRequestUri(modifiedUri)
                    request.getRequestDispatcher(modifiedUri).forward(mutableRequest, response)
                    false
                }
            }
        }
        return true
    }

    private fun isSearchPath(request: HttpServletRequest): Boolean {
        return request.requestURI.endsWith("/search")
    }

    private fun isDownFilePath(request: HttpServletRequest):Boolean {
        val regex = "/\\w+/files/[^/]+".toRegex() //路径中包含 /files/ 并紧跟一个非空文件名
        return regex.containsMatchIn(request.requestURI)
    }
}
