package com.tencent.bkrepo.common.service.util

import com.tencent.bkrepo.common.service.exception.HttpRequestException
import com.tencent.bkrepo.common.service.pojo.ApiResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory

object HttpUtils {
    @Suppress("TooGenericExceptionCaught")
    fun doRequest(
        okHttpClient: OkHttpClient,
        request: Request,
        retry: Int = 0,
        acceptCode: Set<Int> = setOf()
    ): ApiResponse {
        try {
            val response = okHttpClient.newBuilder().build().newCall(request).execute()
            val responseCode = response.code
            val responseContent = response.body!!.string()
            if (response.isSuccessful || acceptCode.contains(responseCode)) {
                return ApiResponse(responseCode, responseContent)
            }
            logger.warn("http request failed, request: [$request], response: [$response]")
        } catch (e: Exception) {
            if (retry > 0) {
                logger.warn("http request error, request: [$request], cause: ${e.message}")
            } else {
                logger.error("http request error, request: [$request]", e)
            }
        }
        if (retry > 0) {
            return doRequest(okHttpClient, request, retry - 1, acceptCode)
        } else {
            throw HttpRequestException(request)
        }
    }

    private val logger = LoggerFactory.getLogger(HttpUtils::class.java)
}
