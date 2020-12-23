package com.tencent.bkrepo.repository.service.canway.bk

import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.service.canway.BKUSERNAME
import com.tencent.bkrepo.repository.service.canway.conf.CanwayAuthConf
import com.tencent.bkrepo.repository.service.canway.http.CanwayHttpUtils
import com.tencent.bkrepo.repository.service.canway.pojo.BkCertificate
import com.tencent.bkrepo.repository.service.canway.pojo.BkUserData
import com.tencent.bkrepo.repository.service.canway.pojo.BkUserInfo
import com.tencent.bkrepo.repository.service.canway.pojo.CertType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BkUserService(
    canwayAuthConf: CanwayAuthConf
) {

    val bkHost = canwayAuthConf.bkHost
        ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, "bkHost must be not null")
    val appCode = canwayAuthConf.appCode
    val appSecert = canwayAuthConf.appSecret

    fun getBkUser(): String {
        val bkCert = getBkToken()
        val uri = String.format(bkUserInfoApi, appCode, appSecert, bkCert.certType.value, bkCert.value)
        val requestUrl = "${bkHost.removeSuffix("/")}$uri"
        val responseContent = CanwayHttpUtils.doGet(requestUrl).content
        val bkUser = responseContent.readJsonString<BkUserInfo>().data
            ?: throw ErrorCodeException(CommonMessageCode.SERVICE_CALL_ERROR, "Can not load user info")

        return bkUser.bk_username
    }

    fun getBkUserByUserId(userId: String): BkUserData {
        val uri = String.format(bkUserInfoApi, appCode, appSecert, BKUSERNAME, userId)
        val requestUrl = "${bkHost.removeSuffix("/")}$uri"
        val responseContent = CanwayHttpUtils.doGet(requestUrl).content
        return responseContent.readJsonString<BkUserInfo>().data
            ?: throw ErrorCodeException(CommonMessageCode.SERVICE_CALL_ERROR, "Can not load user info")
    }

    private fun getBkToken(): BkCertificate {
        val request = HttpContextHolder.getRequest()
        request.cookies?.let {
            for (cookie in it) {
                if (cookie.name == "bk_token") return BkCertificate(CertType.TOKEN, cookie.value)
            }
        }
        request.getAttribute(USER_KEY)?.let {
            return BkCertificate(CertType.USERID, it as String)
        }
        throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, "Can not found bk_token in cookies")
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BkUserService::class.java)
        const val bkUserInfoApi = "/api/c/compapi/v2/bk_login/get_user/?bk_app_code=%s&bk_app_secret=%s&%s=%s"
    }
}
