package com.tencent.bkrepo.auth.service.impl

import com.google.common.cache.CacheBuilder
import com.tencent.bkrepo.auth.CI_TENANT
import com.tencent.bkrepo.auth.service.DepartmentService
import com.tencent.bkrepo.common.api.constant.CharPool
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.devops.DEPLOY_CANWAY
import com.tencent.bkrepo.common.devops.client.BkClient
import com.tencent.bkrepo.common.devops.client.DevopsClient
import com.tencent.bkrepo.common.devops.pojo.BkCertificate
import com.tencent.bkrepo.common.devops.pojo.BkChildrenDepartment
import com.tencent.bkrepo.common.devops.pojo.BkDepartmentUser
import com.tencent.bkrepo.common.devops.pojo.CertType
import com.tencent.bkrepo.common.devops.pojo.DevopsDepartment
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
@ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = DEPLOY_CANWAY, matchIfMissing = true)
class CanwayDepartmentServiceImpl(
    private val bkClient: BkClient,
    private val devopsClient: DevopsClient
) : DepartmentService {
    override fun listDepartmentById(
        userId: String,
        projectId: String?,
        departmentId: Int?
    ): List<BkChildrenDepartment>? {
        val devopsDepartment = getTenantId()?.let { departmentsByUserIdAndTenantId(userId, it) }
        val departments = if (departmentId == null) {
            // 获取根部门
            bkClient.listDepartments(
                bkUsername = userId,
                bkToken = getBkToken(),
                lookupField = "level",
                exactLookups = "0"
            )
        } else {
            // 获取[departmentId]的子部门
            bkClient.listDepartments(
                bkUsername = userId,
                bkToken = getBkToken(),
                lookupField = "parent",
                exactLookups = departmentId.toString()
            )
        }
        return if (!devopsDepartment.isNullOrEmpty()) {
            departments?.filter { devopsDepartment.contains(it) }
        } else {
            logger.info("devops department is empty")
            departments
        }
    }
    override fun listDepartmentByIds(
        userId: String,
        username: String?,
        departmentIds: List<Int>
    ): List<BkChildrenDepartment>? {
        return bkClient.listDepartments(
            bkUsername = username,
            bkToken = getBkToken(),
            lookupField = "id",
            exactLookups = departmentIds.joinToString(",")
        )
    }
    private fun departmentsByUserIdAndTenantId(userId: String, tenantId: String): List<BkChildrenDepartment>? {
        return devopsDepartmentCache.getIfPresent(UserDepartmentId(userId, tenantId)) ?: run {
            val devopsDepartment = devopsClient.departmentsByUserIdAndTenantId(userId, tenantId)
            val resultList = mutableListOf<BkChildrenDepartment>()
            devopsDepartment?.forEach { addDepartment(resultList, it) }
            resultList.apply { devopsDepartmentCache.put(UserDepartmentId(userId, tenantId), this) }
        }
    }

    private fun addDepartment(departmentList: MutableList<BkChildrenDepartment>, devopsDepartment: DevopsDepartment) {
        devopsDepartment.apply {
            departmentList.add(BkChildrenDepartment(id, name, null, null, null))
            children?.forEach {
                addDepartment(departmentList, it)
            }
        }
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

    fun getTenantId(): String? {
        val request = HttpContextHolder.getRequest()
        val cookies = request.cookies
        if (cookies != null) {
            for (cookie in cookies) {
                if (cookie.name == CI_TENANT) return cookie.value
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

    data class UserDepartmentId(val userId: String, val tenantId: String) {
        override fun toString(): String {
            return StringBuilder(userId).append(CharPool.SLASH).append(tenantId).toString()
        }
    }
    companion object {
        private val devopsDepartmentCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build<UserDepartmentId, List<BkChildrenDepartment>>()
        private val logger = LoggerFactory.getLogger(CanwayDepartmentServiceImpl::class.java)
    }
}
