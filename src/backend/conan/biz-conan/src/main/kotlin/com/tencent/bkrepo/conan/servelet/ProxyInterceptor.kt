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

package com.tencent.bkrepo.conan.servelet

import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.metadata.service.repo.RepositoryService
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.common.storage.innercos.http.HttpMethod
import com.tencent.bkrepo.conan.service.ConanRemoteService
import com.tencent.bkrepo.conan.service.ConanVirtualService
import com.tencent.bkrepo.conan.utils.PathUtils.isFirstQueryPath
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.springframework.web.servlet.HandlerInterceptor

/**
 * 本地仓库：无拦截
 * 远程仓库：
 * 下载请求不拦截，其余拦截全部转发到配置的远程仓库
 * 下载请求进ConanRemoteRepository正常逻辑，代理配置的远程仓库下载数据，缓存至本地，返回。
 * 虚拟仓库：
 * 第一次请求（search、latest）不拦截，在ConanArtifactInfoResolver尝试从redis获取packageKey对应的实际仓库：
 * a.若存在，则替换请求的repoName。
 * b.若不存在，则执行search方法（该方法会聚合结果，并缓存结果到redis），然后替换请求的repoName。
 * 非第一次的请求则拦截，从缓存获取实际仓库，根据仓库类型：a.远程仓库则转发配置的远程仓库 b.本地仓库则放行
 * @see com.tencent.bkrepo.conan.artifact.resolver.ConanArtifactInfoResolver
 */
class ProxyInterceptor(
    private val conanRemoteService: ConanRemoteService,
    private val conanVirtualService: ConanVirtualService
) : HandlerInterceptor {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        with(ArtifactContextHolder.getRepoDetail()!!) {
            if (category == RepositoryCategory.REMOTE && request.method == HttpMethod.GET.name) {
                return handleRemoteRepo(this, request, response)
            }
            if (category == RepositoryCategory.VIRTUAL) {
                return if (isFirstQueryPath(request.requestURI)) {
                    true
                } else {
                    val actualRepoName = conanVirtualService.getCacheRepo(this, request.requestURI) ?: return true
                    val actualRepoDetail = SpringContextUtils.getBean(RepositoryService::class.java)
                        .getRepoDetail(projectId, actualRepoName)!!
                    if (actualRepoDetail.category == RepositoryCategory.REMOTE) {
                        handleRemoteRepo(actualRepoDetail, request, response)
                    } else true
                }
            }
        }
        return true
    }

    private fun handleRemoteRepo(
        repositoryDetail: RepositoryDetail,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) =
        if (isDownFilePath(request)) {
            //下载请求不拦截
            true
        } else {
            //代理失败则不拦截
            conanRemoteService.proxyRequestToRemote(repositoryDetail, response).not()
        }

    private fun isDownFilePath(request: HttpServletRequest): Boolean {
        val regex = "/\\w+/files/[^/]+".toRegex() //路径中包含 /files/ 并紧跟一个非空文件名
        return regex.containsMatchIn(request.requestURI)
    }
}
