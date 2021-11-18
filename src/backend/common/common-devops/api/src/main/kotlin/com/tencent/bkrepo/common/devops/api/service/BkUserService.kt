package com.tencent.bkrepo.common.devops.api.service

import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.devops.api.BKUSERNAME
import com.tencent.bkrepo.common.devops.api.conf.DevopsConf
import com.tencent.bkrepo.common.devops.api.pojo.BkUserData
import com.tencent.bkrepo.common.devops.api.pojo.BkUserInfo
import com.tencent.bkrepo.common.devops.api.pojo.BkCertificate
import com.tencent.bkrepo.common.devops.api.pojo.CertType
import com.tencent.bkrepo.common.devops.api.pojo.BkPage
import com.tencent.bkrepo.common.devops.api.pojo.BkUser
import com.tencent.bkrepo.common.devops.api.pojo.BkResponse
import com.tencent.bkrepo.common.devops.api.util.http.CanwayHttpUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BkUserService(
    devopsConf: DevopsConf
) {

    val bkHost = devopsConf.bkHost
    val appCode = devopsConf.appCode
    val appSecret = devopsConf.appSecret

    fun getBkUser(): String {
        val bkCert = getBkCert()
        val uri = String.format(bkUserInfoApi, appCode, appSecret, bkCert.certType.value, bkCert.value)
        val requestUrl = "${bkHost.removeSuffix("/")}$uri"
        val responseContent = CanwayHttpUtils.doGet(requestUrl).content
        val bkUser = responseContent.readJsonString<BkUserInfo>().data
            ?: throw ErrorCodeException(CommonMessageCode.SERVICE_CALL_ERROR, "Can not load user info")
        return bkUser.bk_username
    }

    fun getBkUserByUserId(userId: String): BkUserData {
        val uri = String.format(bkUserInfoApi, appCode, appSecret, BKUSERNAME, userId)
        val requestUrl = "${bkHost.removeSuffix("/")}$uri"
        val responseContent = CanwayHttpUtils.doGet(requestUrl).content
        return responseContent.readJsonString<BkUserInfo>().data
            ?: throw ErrorCodeException(CommonMessageCode.SERVICE_CALL_ERROR, "Can not load user info")
    }

    private fun getBkCert(): BkCertificate {
        val request = HttpContextHolder.getRequest()
        request.getAttribute(USER_KEY)?.let {
            return BkCertificate(CertType.USERID, it as String)
        }
        request.cookies?.let {
            for (cookie in it) {
                if (cookie.name == "bk_token") return BkCertificate(CertType.TOKEN, cookie.value)
            }
        }
        throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, "Can not found bk_token in cookies")
    }

    fun getBkUsers(page: Int, pageSize: Int): BkPage<BkUser>? {
        val uri = String.format(bkUserApi, appCode, appSecret, page, pageSize)
        val requestUrl = "${bkHost.removeSuffix("/")}$uri"
        val responseContent = CanwayHttpUtils.doGet(requestUrl).content
        val bkResponse = responseContent.readJsonString<BkResponse<BkPage<BkUser>>>()
        return bkResponse.data
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BkUserService::class.java)
        const val bkUserInfoApi = "/api/c/compapi/v2/bk_login/get_user/?bk_app_code=%s&bk_app_secret=%s&%s=%s"
        const val bkUserApi =
            "/api/c/compapi/v2/usermanage/list_users/?bk_app_code=%s&username=admin&bk_app_secret=%s&fields=username,display_name&page=%d&page_size=%d"
    }
}
