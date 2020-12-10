package com.tencent.bkrepo.auth.service.canway

import com.tencent.bkrepo.auth.service.DepartmentService
import com.tencent.bkrepo.auth.service.canway.conf.CanwayAuthConf
import com.tencent.bkrepo.auth.service.canway.http.CertTrustManager
import com.tencent.bkrepo.auth.service.canway.pojo.BkDepartment
import com.tencent.bkrepo.auth.service.canway.pojo.BkChildrenDepartment
import com.tencent.bkrepo.auth.service.canway.pojo.BkResponse
import com.tencent.bkrepo.auth.service.canway.pojo.BkCertificate
import com.tencent.bkrepo.auth.service.canway.pojo.CertType
import com.tencent.bkrepo.auth.service.canway.pojo.BkPage
import com.tencent.bkrepo.auth.service.canway.pojo.BkParentDepartment
import com.tencent.bkrepo.auth.util.HttpUtils
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.OkHttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class CanwayDepartmentServiceImpl(
    canwayAuthConf: CanwayAuthConf
) : DepartmentService {

    private val okHttpClient = OkHttpClient.Builder()
        .sslSocketFactory(CertTrustManager.disableValidationSSLSocketFactory, CertTrustManager.disableValidationTrustManager)
        .hostnameVerifier(CertTrustManager.trustAllHostname)
        .connectTimeout(3L, TimeUnit.SECONDS)
        .readTimeout(5L, TimeUnit.SECONDS)
        .writeTimeout(5L, TimeUnit.SECONDS)
        .build()

    val paasHost = canwayAuthConf.bkHost
    // todo 复用 BkAuthConfig
    val bkAppCode = canwayAuthConf.appCode
    val bkAppSecret = canwayAuthConf.appSecret

    override fun listDepartmentById(username: String?, departmentId: Int?): List<BkChildrenDepartment>? {
        val bkCertificate = getBkCertificate(username)
        if (departmentId != null) {
            val mediaType = MediaType.parse("application/json; charset=utf-8")
            val json = String.format(jsonFormat, bkAppCode, bkAppSecret, bkCertificate.certType.value, bkCertificate.value, departmentId)
            val requestUrl = "${paasHost?.removeSuffix("/")}$paasBatchDepartmentUrl"
            val body = RequestBody.create(mediaType, json)
            val request = Request.Builder()
                .url(requestUrl)
                .post(body)
                .build()
            val responseContent = HttpUtils.doRequest(okHttpClient, request, 3, mutableSetOf(200)).content
            val departments = responseContent.readJsonString<BkResponse<List<BkParentDepartment>>>()
            return departments.data?.first()?.children
        } else {
            val company = getCompanyId(bkCertificate)
            return listOf(company).map { transferCanwayChildrenDepartment(company) }
        }
    }

    override fun listDepartmentByIds(username: String?, departmentIds: List<Int>): List<BkChildrenDepartment> {
        val bkCertificate = getBkCertificate(username)
        val list = mutableListOf<BkChildrenDepartment>()
        for (departmentId in departmentIds) {
            val url = "${paasHost?.removeSuffix("/")}$paasListDepartmentUrl"
            val requestUrl = String.format(
                paasListDepartmentRequest, url, bkAppCode, bkAppSecret,
                bkCertificate.certType.value, bkCertificate.value,
                departmentId, listDepartmentField
            )
            val request = Request.Builder()
                .url(requestUrl)
                .build()
            val response = HttpUtils.doRequest(okHttpClient, request, 3, mutableSetOf(200))
            val responseContent = response.content
            val department = responseContent.readJsonString<BkResponse<BkPage<BkDepartment>>>().data?.results?.first()
                ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND, "Can not load departmentId: $departmentId")
            list.add(BkChildrenDepartment(department.id, department.name, null, null, null))
        }
        return list
    }

    /**
     * 兼容蓝鲸 username 和cookie两种认证模式
     */
    fun getBkCertificate(username: String?): BkCertificate {
        val cookies = HttpContextHolder.getRequest().cookies
        if (cookies != null) {
            for (cookie in cookies) {
                if (cookie.name == CertType.TOKEN.value) return BkCertificate(CertType.TOKEN, cookie.value)
            }
        }
        if (username != null) {
            return BkCertificate(CertType.USERNAME, username)
        }
        throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, "User authentication failed, can not found bk_token in cookie or username in request")
    }

    fun transferCanwayChildrenDepartment(department: BkDepartment): BkChildrenDepartment {
        return BkChildrenDepartment(
            department.id,
            department.name,
            department.order,
            department.parent,
            null
        )
    }

    fun getCompanyId(bkCertificate: BkCertificate): BkDepartment {
        // 查出总公司id
        val url = "${paasHost?.removeSuffix("/")}$paasListDepartmentUrl"
        val requestUrl = String.format(paasDepartmentApi, url, bkAppCode, bkAppSecret, bkCertificate.certType.value, bkCertificate.value)
        val request = Request.Builder()
            .url(requestUrl)
            .build()
        val response = HttpUtils.doRequest(okHttpClient, request, 3, mutableSetOf(200))
        val responseContent = response.content
        return responseContent.readJsonString<BkResponse<BkPage<BkDepartment>>>().data?.results?.first()
            ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND, "Can not found any companyId")
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayDepartmentServiceImpl::class.java)
        const val jsonFormat = "{\n" +
            "    \"bk_app_code\":\"%s\",\n" +
            "    \"bk_app_secret\":\"%s\",\n" +
            "    \"%s\":\"%s\",\n" +
            "    \"id_list\": [%d]\n" +
            "}"
        const val paasListDepartmentUrl = "/api/c/compapi/v2/usermanage/list_departments/"
        const val paasBatchDepartmentUrl = "/api/c/compapi/v2/usermanage/department_batch/"
        const val paasDepartmentApi = "%s?bk_app_code=%s&bk_app_secret=%s&%s=%s&page=1&page_size=1"
        const val paasListDepartmentRequest = "%s?bk_app_code=%s&bk_app_secret=%s&%s=%s&exact_lookups=%d&lookup_field=%s&page=1&page_size=1"
        const val listDepartmentField = "id"
    }
}
