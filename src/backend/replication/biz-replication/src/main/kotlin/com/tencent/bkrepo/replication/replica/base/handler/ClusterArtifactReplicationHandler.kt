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

package com.tencent.bkrepo.replication.replica.base.handler

import com.tencent.bkrepo.common.artifact.stream.rateLimit
import com.tencent.bkrepo.replication.config.ReplicationProperties
import com.tencent.bkrepo.replication.constant.BLOB_PUSH_URI
import com.tencent.bkrepo.replication.constant.BOLBS_UPLOAD_FIRST_STEP_URL_STRING
import com.tencent.bkrepo.replication.constant.FILE
import com.tencent.bkrepo.replication.constant.PUSH_WITH_CHUNKED
import com.tencent.bkrepo.replication.constant.SHA256
import com.tencent.bkrepo.replication.constant.SIZE
import com.tencent.bkrepo.replication.constant.STORAGE_KEY
import com.tencent.bkrepo.replication.manager.LocalDataManager
import com.tencent.bkrepo.replication.replica.base.context.FilePushContext
import com.tencent.bkrepo.replication.util.StreamRequestBody
import okhttp3.MultipartBody
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ClusterArtifactReplicationHandler(
    localDataManager: LocalDataManager,
    replicationProperties: ReplicationProperties
) : ArtifactReplicationHandler(localDataManager, replicationProperties) {


    override fun blobPush(
        filePushContext: FilePushContext,
        pushType: String
    ) : Boolean {
        return when (pushType) {
            PUSH_WITH_CHUNKED -> {
                super.blobPush(filePushContext, pushType)
            }
            else -> {
                pushBlob(filePushContext)
                true
            }
        }
    }

    override fun getBlobSha256AndSize(
        filePushContext: FilePushContext
    ): Pair<String, Long> {
        return Pair(filePushContext.sha256!!, filePushContext.size!!)
    }

    override fun buildSessionRequestInfo(filePushContext: FilePushContext) : Pair<String, String?> {
        with(filePushContext) {
            val url = BOLBS_UPLOAD_FIRST_STEP_URL_STRING.format(context.remoteProjectId, context.remoteRepoName)
            val postUrl = buildUrl(context.cluster.url, url, context)
            return Pair(postUrl, buildParams(sha256!!, filePushContext))
        }
    }

    override fun buildChunkUploadRequestInfo(
        sha256: String,
        filePushContext: FilePushContext
    ) : Pair<String?, List<Int>>{
        return Pair(buildParams(sha256, filePushContext), emptyList())
    }

    override fun buildSessionCloseRequestParam(
        sha256: String,
        filePushContext: FilePushContext
    ) : String {
        return buildParams(sha256, filePushContext)
    }


    override fun buildBlobUploadWithSingleChunkRequestParam(
        sha256: String,
        filePushContext: FilePushContext
    ) : String? {
        return buildParams(sha256, filePushContext)
    }

    /**
     * 推送blob文件数据到远程集群
     */
    private fun pushBlob(
        filePushContext: FilePushContext
    ) {
        with(filePushContext) {
            logger.info("File $sha256 will be pushed using the default way.")
            val artifactInputStream = localDataManager.getBlobData(sha256!!, size!!, context.localRepo)
            val rateLimitInputStream = artifactInputStream.rateLimit(
                replicationProperties.rateLimit.toBytes()
            )
            val storageKey = context.remoteRepo?.storageCredentials?.key
            val pushUrl =  buildUrl(context.cluster.url, BLOB_PUSH_URI, context)
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(FILE, sha256, StreamRequestBody(rateLimitInputStream, size))
                .addFormDataPart(SIZE, size.toString())
                .addFormDataPart(SHA256, sha256).apply {
                    storageKey?.let { addFormDataPart(STORAGE_KEY, it) }
                }.build()
            logger.info("The request will be sent for file sha256 [$sha256].")
            val httpRequest = Request.Builder()
                .url(pushUrl)
                .post(requestBody)
                .build()
            httpClient.newCall(httpRequest).execute().use {
                check(it.isSuccessful) { "Failed to replica file: ${it.body()?.string()}" }
            }
        }
    }

    private fun buildParams(
        sha256: String,
        filePushContext: FilePushContext
    ): String {
        val params = "$SHA256=$sha256"
        filePushContext.context.remoteRepo?.storageCredentials?.key?.let {
            "$params&$STORAGE_KEY=$it"
        }
        return params
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClusterArtifactReplicationHandler::class.java)
    }
}