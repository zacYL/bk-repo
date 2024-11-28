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

package com.tencent.bkrepo.common.artifact.resolve.response

import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.exception.ArtifactResponseException
import com.tencent.bkrepo.common.artifact.metrics.RecordAbleInputStream
import com.tencent.bkrepo.common.artifact.stream.rateLimit
import com.tencent.bkrepo.common.artifact.util.http.HttpHeaderUtils.encodeDisposition
import com.tencent.bkrepo.common.artifact.util.http.IOExceptionUtils.isClientBroken
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.monitor.Throughput
import com.tencent.bkrepo.common.storage.monitor.measureThroughput
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.zip.GZIPOutputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
open class GzArtifactResourceWriter(
    private val storageProperties: StorageProperties,
) : BaseArtifactResourceWriter(storageProperties) {

    @Throws(ArtifactResponseException::class)
    override fun write(resource: ArtifactResource): Throughput {
        return if (resource.containsMultiArtifact()) {
            writeMultiArtifact(resource)
        } else {
            writeSingleArtifact(resource)
        }
    }

    /**
     * 响应单个构件数据
     */
    private fun writeSingleArtifact(resource: ArtifactResource): Throughput {
        val request = HttpContextHolder.getRequest()
        val response = HttpContextHolder.getResponse()
        val name = resource.getSingleName()
        val cacheControl = resource.node?.metadata?.get(HttpHeaders.CACHE_CONTROL)?.toString()
            ?: StringPool.NO_CACHE

        val compressedName = getBaseName(resource) + EXTENSION
        response.bufferSize = getBufferSize(resource.getSingleStream().range.length.toInt())
        response.contentType = MediaTypes.APPLICATION_GZIP
        response.characterEncoding = resource.characterEncoding
        response.status = HttpStatus.OK.value
        response.setHeader(HttpHeaders.CACHE_CONTROL, cacheControl)
        if (resource.useDisposition) {
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, encodeDisposition(compressedName))
        }

        return writeTarGzStream(resource, request, response)
    }

    /**
     * 响应多个构件数据
     * 将所有构件压缩到一个 `.gz` 文件中
     */
    private fun writeMultiArtifact(resource: ArtifactResource): Throughput {
        val request = HttpContextHolder.getRequest()
        val response = HttpContextHolder.getResponse()
        val compressedName = resolveMultiArtifactName(resource) + EXTENSION

        response.bufferSize = getBufferSize(resource.getTotalSize().toInt())
        response.contentType = MediaTypes.APPLICATION_GZIP
        response.characterEncoding = resource.characterEncoding
        response.status = HttpStatus.OK.value
        response.setHeader(HttpHeaders.CACHE_CONTROL, StringPool.NO_CACHE)
        if (resource.useDisposition) {
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, encodeDisposition(compressedName))
        }

        return writeTarGzStream(resource, request, response)
    }

    private fun writeTarGzStream(
        resource: ArtifactResource,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): Throughput {
        if (request.method == HttpMethod.HEAD.name) {
            return Throughput.EMPTY
        }

        try {
            return measureThroughput {
                val tarOutputStream = TarArchiveOutputStream(GZIPOutputStream(response.outputStream.buffered()))
                tarOutputStream.use { tarOutput ->
                    if (resource.containsMultiArtifact()) {
                        // 压缩多个文件并保留路径
                        resource.artifactMap.forEach { (name, inputStream) ->
                            val entry = TarArchiveEntry(name)
                            tarOutput.putArchiveEntry(entry)
                            val recordAbleInputStream = RecordAbleInputStream(inputStream)
                            recordAbleInputStream.rateLimit(storageProperties.response.rateLimit.toBytes()).use {
                                it.copyTo(tarOutput)
                            }
                            tarOutput.closeArchiveEntry()
                        }
                    } else {
                        // 压缩单个文件
                        val inputStream = resource.getSingleStream()
                        val entry = TarArchiveEntry(resource.getSingleName())
                        entry.size = inputStream.available().toLong()
                        tarOutput.putArchiveEntry(entry)
                        val recordAbleInputStream = RecordAbleInputStream(inputStream)
                        recordAbleInputStream.rateLimit(storageProperties.response.rateLimit.toBytes()).use {
                            it.copyTo(tarOutput)
                        }
                        tarOutput.closeArchiveEntry()
                        tarOutput.finish()
                    }
                }
                resource.getTotalSize()
            }
        } catch (exception: IOException) {
            val message = exception.message.orEmpty()
            val status = if (isClientBroken(exception)) HttpStatus.BAD_REQUEST else HttpStatus.INTERNAL_SERVER_ERROR
            throw ArtifactResponseException(message, status)
        }
    }

    /**
     * 响应多个构件时解析构件名称
     */
    private fun resolveMultiArtifactName(resource: ArtifactResource): String {
        val baseName = getBaseName(resource)
        return baseName
    }

    companion object {
        private const val EXTENSION = ".gz"
    }
}
