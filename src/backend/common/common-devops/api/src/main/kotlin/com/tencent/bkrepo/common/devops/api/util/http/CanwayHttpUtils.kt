package com.tencent.bkrepo.common.devops.api.util.http

import com.tencent.bkrepo.common.service.pojo.ApiResponse
import com.tencent.bkrepo.common.service.util.HttpUtils
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.concurrent.TimeUnit

object CanwayHttpUtils {
    private val mediaType = MediaType.parse("application/json; charset=utf-8")

    private val okHttpClient = OkHttpClient.Builder()
        .sslSocketFactory(
            CertTrustManager.disableValidationSSLSocketFactory,
            CertTrustManager.disableValidationTrustManager
        )
        .hostnameVerifier(CertTrustManager.trustAllHostname)
        .connectTimeout(3L, TimeUnit.SECONDS)
        .readTimeout(5L, TimeUnit.SECONDS)
        .writeTimeout(5L, TimeUnit.SECONDS)
        .build()

    fun doGet(url: String, retry: Int = 3, acceptCode: Set<Int> = setOf(200)): ApiResponse {
        val request = Request.Builder().url(url).build()
        return HttpUtils.doRequest(okHttpClient, request, retry, acceptCode)
    }

    fun doPost(url: String, body: String, retry: Int = 3, acceptCode: Set<Int> = setOf(200)): ApiResponse {
        val requestBody = RequestBody.create(mediaType, body)
        val request = Request.Builder().url(url).post(requestBody).build()
        return HttpUtils.doRequest(okHttpClient, request, retry, acceptCode)
    }
}
