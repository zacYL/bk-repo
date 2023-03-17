package com.tencent.bkrepo.auth.service.impl

import com.tencent.bkrepo.auth.job.BkDepartmentCache
import com.tencent.bkrepo.auth.service.DepartmentService
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.devops.BK_WHITELIST_USER
import com.tencent.bkrepo.common.devops.DEPLOY_CANWAY
import com.tencent.bkrepo.common.devops.client.BkClient
import com.tencent.bkrepo.common.devops.client.DevopsClient
import com.tencent.bkrepo.common.devops.pojo.BkCertificate
import com.tencent.bkrepo.common.devops.pojo.BkChildrenDepartment
import com.tencent.bkrepo.common.devops.pojo.BkDepartmentUser
import com.tencent.bkrepo.common.devops.pojo.CertType
import com.tencent.bkrepo.common.devops.util.http.DevopsHttpUtils.getBkToken
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.Collections

@Service
@ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = DEPLOY_CANWAY, matchIfMissing = true)
class CanwayDepartmentServiceImpl(
    private val bkClient: BkClient,
    private val devopsClient: DevopsClient,
    private val bkDepartmentCache: BkDepartmentCache
) : DepartmentService {
    override fun listDepartmentById(
        userId: String,
        departmentId: Int?,
        projectId: String
    ): List<BkChildrenDepartment>? {
        val ciPermissionDepartment = devopsClient.departmentsByProjectId(projectId)?.let { list ->
            list.map { it.id }
        } ?: return null
        return if (departmentId == null) {
            // 获取根部门
            bkClient.listDepartments(
                bkUsername = BK_WHITELIST_USER,
                bkToken = getBkToken(),
                lookupField = "level",
                exactLookups = "0"
            )?.onEach {
                it.permission = ciPermissionDepartment.contains(it.id)
            }
        } else {
            // 获取[departmentId]的子部门
            bkClient.listDepartments(
                bkUsername = BK_WHITELIST_USER,
                bkToken = getBkToken(),
                lookupField = "parent",
                exactLookups = departmentId.toString()
            )?.onEach { department ->
                val bkChildrenDepartment = bkDepartmentCache.allBkDepartment.find { it.id == department.id }
                department.permission = bkChildrenDepartment?.parentDepartmentIds
                    ?.let {
                        !Collections.disjoint(
                            ciPermissionDepartment,
                            mutableListOf<String>().apply { addAll(it); add(bkChildrenDepartment.id) })
                    } ?: false
            }
        }
    }
    override fun listDepartmentByIds(
        userId: String,
        departmentIds: List<Int>
    ): List<BkChildrenDepartment>? {
        return bkClient.listDepartments(
            bkUsername = BK_WHITELIST_USER,
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

    override fun getUsersByDepartmentId(username: String?, departmentId: Int): List<BkDepartmentUser>? {
        return bkClient.listDepartmentProfiles(
            bkUsername = username,
            bkToken = getBkToken(),
            id = departmentId.toString(),
            recursive = true
        )
    }

    override fun listDepartmentByProjectId(userId: String, projectId: String): List<String> {
        return devopsClient.departmentsByProjectId(projectId)?.map { it.id } ?: listOf()
    }
    companion object {
        private val logger = LoggerFactory.getLogger(CanwayDepartmentServiceImpl::class.java)
    }
}
