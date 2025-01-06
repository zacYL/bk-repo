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

package com.tencent.bkrepo.common.artifact.repository.local

import com.tencent.bkrepo.common.artifact.pojo.RepositoryIdentify
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.core.AbstractArtifactRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.springframework.beans.factory.annotation.Autowired

/**
 * 本地仓库抽象逻辑
 */
abstract class LocalRepository : AbstractArtifactRepository() {

    @Autowired
    lateinit var permissionManager: PermissionManager

    override fun onUpload(context: ArtifactUploadContext) {
        with(context) {
            val nodeCreateRequest = buildNodeCreateRequest(this)
            storageManager.storeArtifactFile(nodeCreateRequest, getArtifactFile(), storageCredentials)
        }
    }

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        with(context) {
            getFullPathInterceptors().forEach { it.intercept(projectId, artifactInfo.getArtifactFullPath()) }
            val node = ArtifactContextHolder.getNodeDetail(artifactInfo)
            node?.let { downloadIntercept(context, it) }
            val inputStream = storageManager.loadArtifactInputStream(node, storageCredentials) ?: return null
            val responseName = artifactInfo.getResponseName()
            val srcRepo = RepositoryIdentify(projectId, repoName)
            return ArtifactResource(inputStream, responseName, srcRepo, node, ArtifactChannel.LOCAL, useDisposition)
        }
    }

    /**
     * 拦截node上传
     */
    fun uploadIntercept(context: ArtifactUploadContext, nodeDetail: NodeDetail) {
        with(context) {
            getInterceptors().forEach { it.intercept(projectId, nodeDetail) }
            // 拦截package下载
            val packageKey = nodeDetail.packageName()?.let { PackageKeys.ofName(repositoryDetail.type, it) }
            val version = nodeDetail.packageVersion()
            if (packageKey != null && version != null) {
                packageClient.findVersionByName(projectId, repoName, packageKey, version).data?.let { packageVersion ->
                    uploadIntercept(context, packageVersion)
                }
            }
        }
    }

    /**
     * 拦截package上传
     * TODO NODE中统一存储packageKey与version元数据后可设置为private方法
     */
    fun uploadIntercept(context: ArtifactUploadContext, packageVersion: PackageVersion) {
        context.getPackageInterceptors().forEach { it.intercept(context.projectId, packageVersion) }
    }


    override fun onDownloadBefore(context: ArtifactDownloadContext) {
        super.onDownloadBefore(context)
    }

    /**
     * 构造节点创建请求
     */
    open fun buildNodeCreateRequest(context: ArtifactUploadContext): NodeCreateRequest {
        return NodeCreateRequest(
            projectId = context.repositoryDetail.projectId,
            repoName = context.repositoryDetail.name,
            folder = false,
            fullPath = context.artifactInfo.getArtifactFullPath(),
            size = context.getArtifactFile().getSize(),
            sha256 = context.getArtifactSha256(),
            md5 = context.getArtifactMd5(),
            operator = context.userId
        )
    }

    /**
     * 包版本文件路径集合
     */
    @Suppress("LongParameterList")
    open fun getArtifactFullPaths(
        projectId: String,
        repoName: String,
        key: String,
        version: String,
        manifestPath: String?,
        artifactPath: String?
    ): List<String> {
        return artifactPath?.let { listOf(it) } ?: emptyList()
    }

    open fun updateIndex(projectId: String, repoName: String, key: String, version: String) = Unit
}
