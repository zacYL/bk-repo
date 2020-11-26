package com.tencent.bkrepo.auth.service.canway

import com.tencent.bkrepo.auth.service.DepartmentService
import com.tencent.bkrepo.auth.service.canway.pojo.*
import com.tencent.bkrepo.auth.util.HttpUtils
import com.tencent.bkrepo.common.api.util.readJsonString
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CanwayDepartmentServiceImpl(
        canwayAuthConf: CanwayAuthConf
) : DepartmentService {

    val paasHost = canwayAuthConf.devopsGatewayApi
    val paasListDepartmentUrl = "/api/c/compapi/v2/usermanage/list_departments/"
    val paasBatchDepartmentUrl = "/api/c/compapi/v2/usermanage/department_batch/"
    // todo 复用 BkAuthConfig
    val bkAppCode = canwayAuthConf.appCode
    val bkAppSecret = canwayAuthConf.appSecret
    // todo 兼容username 和bk_token 两种模式
    val paasDepartmentApi = "%s?bk_app_code=%s&bk_app_secret=%s&bk_username=%s&page=1&page_size=1"

    override fun listDepartmentById(username: String, departmentId: Int?): List<CanwayChildrenDepartmentPojo>? {
        if (departmentId != null) {
            val mediaType = MediaType.parse("application/json; charset=utf-8")
            val json = String.format(jsonFormat, bkAppCode, bkAppSecret, username, departmentId)
            val requestUrl = "${paasHost?.removeSuffix("/")}$paasBatchDepartmentUrl"
            val body = RequestBody.create(mediaType, json)
            val request = Request.Builder()
                    .url(requestUrl)
                    .post(body)
                    .build()
            val responseContent = HttpUtils.doRequest(OkHttpClient(), request, 3, mutableSetOf(200)).content
            val departments = responseContent.readJsonString<CanwayDepartmentResponse<List<CanwayParentDepartmentPojo>>>()
            return departments.data.first().children
        } else {
            val company = getCompanyId(username)
            return listOf(company).map { transferCanwayChildrenDepartment(company) }
        }
    }

    fun transferCanwayChildrenDepartment(department: CanwayDepartmentPojo): CanwayChildrenDepartmentPojo {
        return CanwayChildrenDepartmentPojo(
                department.id,
                department.order,
                department.name,
                department.parent,
                null
        )
    }

    fun getCompanyId(username: String): CanwayDepartmentPojo {
        //查出总公司id
        val url = "${paasHost?.removeSuffix("/")}$paasListDepartmentUrl"
        val requestUrl = String.format(paasDepartmentApi,url, bkAppCode, bkAppSecret, username)
        val request = Request.Builder()
                .url(requestUrl)
                .build()
        val response = HttpUtils.doRequest(OkHttpClient(), request, 3, mutableSetOf(200))
        val responseContent =  response.content
        return responseContent.readJsonString<CanwayDepartmentResponse<CanwayDepartmentPage>>().data.results!!.first()
    }

    companion object{
        val logger: Logger = LoggerFactory.getLogger(CanwayDepartmentServiceImpl::class.java)
        val jsonFormat = "{\n" +
                "    \"bk_app_code\":\"%s\",\n" +
                "    \"bk_app_secret\":\"%s\",\n" +
                "    \"bk_username\":\"%s\",\n" +
                "    \"id_list\": [%d]\n" +
                "}"
    }
}