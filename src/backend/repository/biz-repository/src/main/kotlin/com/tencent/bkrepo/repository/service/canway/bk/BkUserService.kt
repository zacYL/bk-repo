package com.tencent.bkrepo.repository.service.canway.bk

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.artifact.util.okhttp.CertTrustManager.disableValidationSSLSocketFactory
import com.tencent.bkrepo.common.artifact.util.okhttp.CertTrustManager.disableValidationTrustManager
import com.tencent.bkrepo.common.artifact.util.okhttp.CertTrustManager.trustAllHostname
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.service.util.HttpUtils
import com.tencent.bkrepo.repository.service.canway.conf.CanwayAuthConf
import com.tencent.bkrepo.repository.service.canway.pojo.BkUserInfo
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class BkUserService(
        canwayAuthConf: CanwayAuthConf
) {

    private val okHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(disableValidationSSLSocketFactory, disableValidationTrustManager)
            .hostnameVerifier(trustAllHostname)
            .connectTimeout(3L, TimeUnit.SECONDS)
            .readTimeout(5L, TimeUnit.SECONDS)
            .writeTimeout(5L, TimeUnit.SECONDS)
            .build()

    val bkHost = canwayAuthConf.bkHost
            ?:throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, "bkHost must be not null")
    val appCode = canwayAuthConf.appCode
    val appSecert = canwayAuthConf.appSecret

    fun getBkUser(): String {
        val bkToken = getBkToken()
        val uri = String.format(bkUserInfoApi, appCode, appSecert, bkToken)
        val requestUrl = "${bkHost.removeSuffix("/")}$uri"
        val request = Request.Builder()
                .url(requestUrl)
                .build()
        val responseContent = HttpUtils.doRequest(okHttpClient, request, 3, mutableSetOf(200)).content
        val bkUser = responseContent.readJsonString<BkUserInfo>().data
                ?: throw ErrorCodeException(CommonMessageCode.SERVICE_CALL_ERROR, "Can not load user info")

        return bkUser.bk_username
    }

    private fun getBkToken(): String {
        val request = HttpContextHolder.getRequest()
        val cookies = request.cookies
        for (cookie in cookies) {
            if(cookie.name == "bk_token") return cookie.value
        }
        throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, "Can not found bk_token in cookies")
    }


    companion object{
        val logger: Logger = LoggerFactory.getLogger(BkUserService::class.java)
        const val bkUserInfoApi = "/api/c/compapi/v2/bk_login/get_user/?bk_app_code=%s&bk_app_secret=%s&bk_token=%s"
    }
}