/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 Tencent.  All rights reserved.
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

package com.tencent.bkrepo.common.artifact.repository.context

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.constant.DownloadInterceptorType
import com.tencent.bkrepo.common.artifact.constant.REPO_KEY
import com.tencent.bkrepo.common.metadata.interceptor.DownloadInterceptor
import com.tencent.bkrepo.common.metadata.interceptor.DownloadInterceptorFactory
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import kotlin.reflect.full.primaryConstructor

/**
 * 构件下载context
 */
open class ArtifactDownloadContext(
    repo: RepositoryDetail? = null,
    artifact: ArtifactInfo? = null,
    artifacts: List<ArtifactInfo>? = null,
    userId: String = SecurityUtils.getUserId(),
    var useDisposition: Boolean = false
) : ArtifactContext(repo, artifact, userId) {

    val repo = repo ?: request.getAttribute(REPO_KEY) as RepositoryDetail
    val artifacts = artifacts
    var shareUserId: String = StringPool.EMPTY

    override fun copy(
        repositoryDetail: RepositoryDetail,
        instantiation: ((ArtifactInfo) -> ArtifactContext)?
    ): ArtifactContext {
        return super.copy(repositoryDetail) { artifactInfo ->
            this::class.primaryConstructor!!.call(
                repositoryDetail, artifactInfo, artifacts, this.userId, useDisposition
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getInterceptors(): List<DownloadInterceptor<*, NodeDetail>> {
        return DownloadInterceptorFactory.buildInterceptors(repo.configuration.settings)
    }

    fun getPackageInterceptors(): List<DownloadInterceptor<*, PackageVersion>> {
        return listOf(DownloadInterceptorFactory.buildPackageInterceptor(DownloadInterceptorType.PACKAGE_FORBID)!!)
    }

}
