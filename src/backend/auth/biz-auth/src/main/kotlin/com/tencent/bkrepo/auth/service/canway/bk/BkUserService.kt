package com.tencent.bkrepo.auth.service.canway.bk

import com.tencent.bkrepo.auth.constant.DEFAULT_PASSWORD
import com.tencent.bkrepo.auth.pojo.user.CreateUserRequest
import com.tencent.bkrepo.auth.pojo.user.UpdateUserRequest
import com.tencent.bkrepo.auth.pojo.user.User
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.auth.service.canway.conf.CanwayAuthConf
import com.tencent.bkrepo.auth.service.canway.http.CertTrustManager
import com.tencent.bkrepo.auth.service.canway.pojo.bk.BkUserInfo
import com.tencent.bkrepo.auth.service.canway.pojo.bk.BkCertificate
import com.tencent.bkrepo.auth.service.canway.pojo.bk.CertType
import com.tencent.bkrepo.auth.service.canway.pojo.bk.BkUser
import com.tencent.bkrepo.auth.service.canway.pojo.bk.BkPage
import com.tencent.bkrepo.auth.service.canway.pojo.bk.BkResponse
import com.tencent.bkrepo.auth.util.HttpUtils
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class BkUserService(
    canwayAuthConf: CanwayAuthConf
) {
    @Autowired
    lateinit var userService: UserService

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

    val bkHost = canwayAuthConf.host
    val appCode = canwayAuthConf.appCode
    val appSecret = canwayAuthConf.appSecret

    fun getBkUser(): String {
        val bkCert = getBkCert()
        val uri = String.format(bkUserInfoApi, appCode, appSecret, bkCert.certType.value, bkCert.value)
        val requestUrl = "${bkHost.removeSuffix("/")}$uri"
        val request = Request.Builder()
            .url(requestUrl)
            .build()
        val responseContent = HttpUtils.doRequest(okHttpClient, request, 3, mutableSetOf(200)).content
        val bkUser = responseContent.readJsonString<BkUserInfo>().data
            ?: throw ErrorCodeException(CommonMessageCode.SERVICE_CALL_ERROR, "Can not load user info")

        return bkUser.bk_username
    }

    private fun getBkCert(): BkCertificate {
        val request = HttpContextHolder.getRequest()
        request.getAttribute(USER_KEY)?.let {
            return BkCertificate(CertType.USERNAME, it as String)
        }
        val cookies = request.cookies
            ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, "bk_token must not be null")
        for (cookie in cookies) {
            if (cookie.name == "bk_token") return BkCertificate(CertType.TOKEN, cookie.value)
        }
        throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, "Can not found bk_token in cookies")
    }

    fun syncBkUser() {
        val count = getBkUsers(1, 1)?.count ?: return
        val pages = (count / pageSize).inc()
        for (i in 1..pages) {
            val userPage = getBkUsers(i, pageSize)
            checkUserExist(userPage)
        }
    }

    private fun checkUserExist(bkUsers: BkPage<BkUser>?) {
        bkUsers?.results ?: return
        for (bkUser in bkUsers.results) {
            val bkrepoUser = userService.getUserById(bkUser.username)
            if (bkrepoUser == null) {
                val createUserRequest = CreateUserRequest(
                    userId = bkUser.username,
                    name = bkUser.displayName,
                    pwd = DEFAULT_PASSWORD,
                    admin = false
                )
                userService.createUser(createUserRequest)
            } else {
                checkUserName(bkrepoUser, bkUser)
            }
        }
    }

    private fun checkUserName(bkrepoUser: User, bkUser: BkUser) {
        if (bkrepoUser.name != bkUser.displayName) {
            userService.updateUserById(
                bkrepoUser.userId,
                UpdateUserRequest(
                    name = bkUser.displayName
                )
            )
            logger.info("${bkrepoUser.userId} name : ${bkrepoUser.name} to ${bkUser.displayName}")
        }
    }

    private fun getBkUsers(page: Int, pageSize: Int): BkPage<BkUser>? {
        val uri = String.format(bkUserApi, appCode, appSecret, page, pageSize)
        val requestUrl = "${bkHost.removeSuffix("/")}$uri"
        val request = Request.Builder().url(requestUrl).build()
        val responseContent = HttpUtils.doRequest(okHttpClient, request, 3, mutableSetOf(200)).content
        val bkResponse = responseContent.readJsonString<BkResponse<BkPage<BkUser>>>()
        return bkResponse.data
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BkUserService::class.java)
        const val bkUserInfoApi = "/api/c/compapi/v2/bk_login/get_user/?bk_app_code=%s&bk_app_secret=%s&%s=%s"
        const val bkUserApi = "/api/c/compapi/v2/usermanage/list_users/?bk_app_code=%s&username=admin&bk_app_secret=%s&fields=username,display_name&page=%d&page_size=%d"
        const val pageSize = 50
    }
}
