package com.tencent.bkrepo.repository.service.canway.service

import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.repository.service.canway.BELONGCODE
import com.tencent.bkrepo.repository.service.canway.RESOURCECODE
import com.tencent.bkrepo.repository.service.canway.aspect.CanwayRepositoryAspect
import com.tencent.bkrepo.repository.service.canway.conf.CanwayAuthConf
import com.tencent.bkrepo.repository.service.canway.http.CanwayHttpUtils
import com.tencent.bkrepo.repository.service.canway.pojo.CanwayPermissionRequest
import com.tencent.bkrepo.repository.service.canway.pojo.CanwayPermissionResponse
import com.tencent.bkrepo.repository.service.canway.pojo.CanwayResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CanwayPermissionService(
    canwayAuthConf: CanwayAuthConf
) {

    private val devopsHost = canwayAuthConf.devopsHost!!.removeSuffix("/")

    private fun checkUserHasProjectPermission(operator: String): Boolean {
        val canwayPermissionResponse = getCanwayPermissionInstance(
            "bk_ci", operator, "create", "system", "project"
        )
        return checkInstance("bk_ci", canwayPermissionResponse)
    }

    fun checkCanwayPermission(projectId: String, repoName: String?, operator: String, action: String): Boolean {
        if (checkUserHasProjectPermission(operator)) return true
        val canwayPermissionResponse = getCanwayPermissionInstance(projectId, operator, action, BELONGCODE, RESOURCECODE)
        return checkInstance(repoName, canwayPermissionResponse)
    }

    private fun checkInstance(repoName: String?, canwayPermission: CanwayPermissionResponse?): Boolean {
        canwayPermission?.let {
            return matchInstance(repoName, it.instanceCodes.first().resourceInstance)
        }
        return false
    }

    private fun matchInstance(repoName: String?, instances: Set<String>?): Boolean {
        instances?.let {
            if (repoName == null) {
                if (it.contains("*")) return true
            } else {
                if (it.contains("*") || it.contains(repoName)) return true
            }
        }
        return false
    }

    fun getCanwayPermissionInstance(
        projectId: String,
        operator: String,
        action: String,
        belongCode: String,
        resourceCode: String
    ):
        CanwayPermissionResponse? {
            val canwayCheckPermissionRequest = CanwayPermissionRequest(
                userId = operator,
                belongCode = belongCode,
                belongInstance = projectId,
                resourcesActions = setOf(
                    CanwayPermissionRequest.CanwayAction(
                        actionCode = action,
                        resourceCode = resourceCode,
                        resourceInstance = setOf(
                            CanwayPermissionRequest.CanwayAction.CanwayInstance(
                                resourceCode = resourceCode
                            )
                        )
                    )
                )
            ).toJsonString()
            val ciAddResourceUrl = "$devopsHost${CanwayRepositoryAspect.ci}$ciCheckPermissionApi"
            val responseContent = CanwayHttpUtils.doPost(ciAddResourceUrl, canwayCheckPermissionRequest).content

            return responseContent.readJsonString<CanwayResponse<CanwayPermissionResponse>>().data
        }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayPermissionService::class.java)
        const val ciCheckPermissionApi = "/api/service/resource_instance/query"
    }
}
