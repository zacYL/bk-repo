package com.tencent.bkrepo.auth.service.impl

import com.tencent.bkrepo.auth.service.DepartmentService
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.devops.DEPLOY_CANWAY
import com.tencent.bkrepo.common.devops.client.BkClient
import com.tencent.bkrepo.common.devops.pojo.BkCertificate
import com.tencent.bkrepo.common.devops.pojo.BkChildrenDepartment
import com.tencent.bkrepo.common.devops.pojo.BkDepartmentUser
import com.tencent.bkrepo.common.devops.pojo.CertType
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = DEPLOY_CANWAY, matchIfMissing = true)
class CanwayDepartmentServiceImpl(
    private val bkClient: BkClient
) : DepartmentService {

    override fun listDepartmentById(username: String?, departmentId: Int?): List<BkChildrenDepartment>? {
        return if (departmentId == null) {
            // 获取根部门
            bkClient.listDepartments(
                bkUsername = username,
                bkToken = getBkToken(),
                lookupField = "level",
                exactLookups = "0"
            )
        } else {
            // 获取[departmentId]的子部门
            bkClient.listDepartments(
                bkUsername = username,
                bkToken = getBkToken(),
                lookupField = "parent",
                exactLookups = departmentId.toString()
            )
        }
    }

    override fun listDepartmentByIds(username: String?, departmentIds: List<Int>): List<BkChildrenDepartment>? {
        return bkClient.listDepartments(
            bkUsername = username,
            bkToken = getBkToken(),
            lookupField = "id",
            exactLookups = departmentIds.joinToString(",")
        )
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
        throw ErrorCodeException(
            CommonMessageCode.PARAMETER_MISSING,
            "User authentication failed, can not found bk_token in cookie or username in request"
        )
    }

    fun getBkToken(): String? {
        val request = HttpContextHolder.getRequest()
        val cookies = request.cookies
        if (cookies != null) {
            for (cookie in cookies) {
                if (cookie.name == CertType.TOKEN.value) return cookie.value
            }
        }
        return null
    }

    override fun getUsersByDepartmentId(username: String?, departmentId: Int): List<BkDepartmentUser>? {
        return bkClient.listDepartmentProfiles(
            bkUsername = username,
            bkToken = getBkToken(),
            id = departmentId.toString(),
            recursive = true
        )
    }
}
