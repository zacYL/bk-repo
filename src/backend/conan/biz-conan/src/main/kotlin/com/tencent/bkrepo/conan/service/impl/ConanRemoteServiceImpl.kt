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

package com.tencent.bkrepo.conan.service.impl

import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.common.artifact.util.http.UrlFormatter
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.conan.artifact.repository.ConanRemoteRepository
import com.tencent.bkrepo.conan.constant.CONAN_URL_V2
import com.tencent.bkrepo.conan.service.ConanRemoteService
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Service
class ConanRemoteServiceImpl : ConanRemoteService {
    /**
     * @return 代理是否成功
     */
    override fun proxyRequestToRemote(repositoryDetail: RepositoryDetail, response: HttpServletResponse): Boolean {
        val remoteConfiguration = repositoryDetail.configuration as RemoteConfiguration
        val repository = SpringContextUtils.getBean<ConanRemoteRepository>()
        val httpClient = repository.getHttpClient(remoteConfiguration)
        val url = getUrl(remoteConfiguration)
        val request = Request.Builder().url(url).build()
        logger.info("Remote download url: $url, network config: ${remoteConfiguration.network}")
        return try {
            val proxyResponse = httpClient.newCall(request).execute()
            val headers = HttpHeaders()
            proxyResponse.headers().names().forEach { name ->
                headers[name] = proxyResponse.headers().values(name)
            }
            if (proxyResponse.isSuccessful) {
                // 设置响应状态码
                response.status = proxyResponse.code()
                headers.forEach { (key, value) ->
                    response.addHeader(key, value.joinToString(","))
                }
                // 设置响应内容
                proxyResponse.body()?.byteStream()?.use { inputStream ->
                    val outputStream = response.outputStream
                    inputStream.copyTo(outputStream)
                    outputStream.flush()
                }
                true
            } else {
                logger.info("remote proxy is false, status code: ${proxyResponse.code()},should use cache")
                false
            }
        } catch (e: Exception) {
            logger.error("Error occurred while proxying request to remote URL: ${e.message}", e)
            false
        }
    }

    private fun getUrl(remoteConfiguration: RemoteConfiguration): String {
        val request: HttpServletRequest = HttpContextHolder.getRequest()
        val requestPath = request.requestURL.toString()
        val startIndex = requestPath.indexOf(CONAN_URL_V2)
        val trimmedPath = requestPath.substring(startIndex)
        val queryString = request.queryString
        return UrlFormatter.format(remoteConfiguration.url, trimmedPath, queryString)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
