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

package com.tencent.bkrepo.maven.artifact.repository

import com.tencent.bkrepo.common.artifact.exception.ArtifactNotInWhitelistException
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.maven.artifact.MavenArtifactInfo
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class MavenRemoteRepository : RemoteRepository() {

    /**
     * 针对索引文件`maven-metadata.xml` 每次都尝试从远程拉取最新的索引文件，
     * 如果远程下载失败则改为使用缓存
     */
    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        return if ((context.artifactInfo as MavenArtifactInfo).isMetadata()) {
            val remoteConfiguration = context.getRemoteConfiguration()
            logger.info(
                "Remote download: not found artifact in cache, " +
                    "download from remote repository: $remoteConfiguration"
            )
            val httpClient = createHttpClient(remoteConfiguration)
            val downloadUrl = createRemoteDownloadUrl(context)
            val request = Request.Builder().url(downloadUrl).build()
            logger.info("Remote download: download url: $downloadUrl")
            val response = downloadRetry(request, httpClient)
            if (response != null && checkResponse(response)) {
                onDownloadResponse(context, response)
            } else getCacheArtifactResource(context)
        } else super.onDownload(context)
    }

    override fun whitelistInterceptor(context: ArtifactDownloadContext) {
        (context.artifactInfo as MavenArtifactInfo).let {
            if (it.isArtifact() &&
                whitelistSwitchClient.get(RepositoryType.MAVEN).data == true &&
                remotePackageClient.search(
                        RepositoryType.MAVEN, "${it.groupId}:${it.artifactId}", it.versionId
                    ).data != true
            ) {
                throw ArtifactNotInWhitelistException()
            }
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MavenRemoteRepository::class.java)
    }
}
