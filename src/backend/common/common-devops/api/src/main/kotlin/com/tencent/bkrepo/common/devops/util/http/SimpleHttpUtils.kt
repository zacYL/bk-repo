package com.tencent.bkrepo.common.devops.util.http

import com.tencent.bkrepo.common.service.pojo.ApiResponse
import com.tencent.bkrepo.common.service.util.HttpUtils
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object SimpleHttpUtils {
    private val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
    private const val connectTimeout = 3L
    private const val readTimeout = 5L
    private const val writeTimeout = 5L
    private const val defaultRetry = 3

    private val okHttpClient = OkHttpClient.Builder()
        .sslSocketFactory(
            CertTrustManager.disableValidationSSLSocketFactory,
            CertTrustManager.disableValidationTrustManager
        )
        .hostnameVerifier(CertTrustManager.trustAllHostname)
        .connectTimeout(connectTimeout, TimeUnit.SECONDS)
        .readTimeout(readTimeout, TimeUnit.SECONDS)
        .writeTimeout(writeTimeout, TimeUnit.SECONDS)
        .build()

    fun doGet(url: String, retry: Int = defaultRetry, acceptCode: Set<Int> = setOf()): ApiResponse {
        val request = Request.Builder().url(url).build()
        return HttpUtils.doRequest(okHttpClient, request, retry, acceptCode)
    }

    fun doPost(url: String, body: String, retry: Int = defaultRetry, acceptCode: Set<Int> = setOf()): ApiResponse {
        val requestBody = body.toRequestBody(mediaType)
        val request = Request.Builder().url(url).post(requestBody).build()
        return HttpUtils.doRequest(okHttpClient, request, retry, acceptCode)
    }
}
