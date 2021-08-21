package com.tencent.bkrepo.auth.service.canway

import com.tencent.bkrepo.auth.service.DepartmentService
import com.tencent.bkrepo.auth.service.canway.conf.CanwayAuthConf
import com.tencent.bkrepo.auth.service.canway.pojo.bk.BkResponse
import com.tencent.bkrepo.auth.service.canway.pojo.bk.BkChildrenDepartment
import com.tencent.bkrepo.auth.service.canway.pojo.bk.BkPage
import com.tencent.bkrepo.auth.service.canway.pojo.bk.BkDepartment
import com.tencent.bkrepo.auth.service.canway.pojo.bk.BkDepartmentUser
import com.tencent.bkrepo.auth.service.canway.pojo.bk.BkParentDepartment
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.devops.http.CanwayHttpUtils
import com.tencent.bkrepo.common.devops.pojo.BkCertificate
import com.tencent.bkrepo.common.devops.pojo.CertType
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CanwayDepartmentServiceImpl(
    canwayAuthConf: CanwayAuthConf
) : DepartmentService {

    val paasHost = canwayAuthConf.host

    // todo 复用 BkAuthConfig
    val bkAppCode = canwayAuthConf.appCode
    val bkAppSecret = canwayAuthConf.appSecret

    override fun listDepartmentById(username: String?, departmentId: Int?): List<BkChildrenDepartment>? {
        val bkCertificate = getBkCertificate(username)
        if (departmentId != null) {
            val json = String.format(
                jsonFormat,
                bkAppCode,
                bkAppSecret,
                bkCertificate.certType.value,
                bkCertificate.value,
                departmentId
            )
            val requestUrl = "${paasHost.removeSuffix("/")}$paasBatchDepartmentUrl"
            val responseContent = CanwayHttpUtils.doPost(requestUrl, json).content
            val departments = responseContent.readJsonString<BkResponse<List<BkParentDepartment>>>()
            return departments.data?.first()?.children
        } else {
            val company = getCompanyId(bkCertificate) ?: return null
            return company.map { transferCanwayChildrenDepartment(it) }
        }
    }

    override fun listDepartmentByIds(username: String?, departmentIds: List<Int>): List<BkChildrenDepartment> {
        val bkCertificate = getBkCertificate(username)
        val list = mutableListOf<BkChildrenDepartment>()
        for (departmentId in departmentIds) {
            val url = "${paasHost.removeSuffix("/")}$paasListDepartmentUrl"
            val requestUrl = String.format(
                paasListDepartmentRequest, url, bkAppCode, bkAppSecret,
                bkCertificate.certType.value, bkCertificate.value,
                departmentId, listDepartmentField
            )
            val responseContent = CanwayHttpUtils.doGet(requestUrl).content
            val department = responseContent.readJsonString<BkResponse<BkPage<BkDepartment>>>().data?.results?.first()
                ?: throw ErrorCodeException(
                    CommonMessageCode.RESOURCE_NOT_FOUND,
                    "Can not load departmentId: $departmentId"
                )
            list.add(BkChildrenDepartment(department.id.toString(), department.name, null, null, null))
        }
        return list
    }

    /**
     * 兼容蓝鲸 username 和cookie两种认证模式
     */
    fun getBkCertificate(username: String?): BkCertificate {
        val request = HttpContextHolder.getRequest()
        val cookies = request.cookies
        request.getAttribute(USER_KEY)?.let {
            return BkCertificate(CertType.USERNAME, it as String)
        }
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
            department.id.toString(),
            department.name,
            department.order,
            department.parent,
            null
        )
    }

    /**
     * 查询蓝鲸level:0 的部门
     */
    fun getCompanyId(bkCertificate: BkCertificate): List<BkDepartment>? {

        val url = "${paasHost.removeSuffix("/")}$paasListDepartmentUrl"
        val requestUrl = String.format(
            paasDepartmentApi,
            url,
            bkAppCode,
            bkAppSecret,
            bkCertificate.certType.value,
            bkCertificate.value
        )
        val responseContent = CanwayHttpUtils.doGet(requestUrl).content
        return responseContent.readJsonString<BkResponse<BkPage<BkDepartment>>>().data?.results
    }

    override fun getUsersByDepartmentId(username: String?, departmentId: Int): Set<BkDepartmentUser>? {
        val bkCertificate = getBkCertificate(username)
        val uri = String.format(
            getUsersByDepartmentIdApi,
            bkAppCode,
            bkAppSecret,
            bkCertificate.certType.value,
            bkCertificate.value,
            departmentId
        )
        val requestUrl = "${paasHost.removeSuffix("/")}$uri"
        val responseContent = CanwayHttpUtils.doGet(requestUrl).content
        return responseContent.readJsonString<BkResponse<Set<BkDepartmentUser>>>().data
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
        const val paasDepartmentApi =
            "%s?bk_app_code=%s&bk_app_secret=%s&%s=%s&lookup_field=level&exact_lookups=0&fields=id,name"
        const val paasListDepartmentRequest =
            "%s?bk_app_code=%s&bk_app_secret=%s&%s=%s&exact_lookups=%d&lookup_field=%s&page=1&page_size=1"
        const val listDepartmentField = "id"
        const val getUsersByDepartmentIdApi =
            "/api/c/compapi/v2/usermanage/list_department_profiles/?bk_app_code=%s&bk_app_secret=%s&%s=%s&id=%d&recursive=true&no_page=true"
    }
}
