package com.tencent.bkrepo.common.devops.repository.service

import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.devops.api.BELONGCODE
import com.tencent.bkrepo.common.devops.api.CANWAY_PERMISSION_API
import com.tencent.bkrepo.common.devops.api.RESOURCECODE
import com.tencent.bkrepo.common.devops.api.conf.DevopsConf
import com.tencent.bkrepo.common.devops.api.enums.CanwayPermissionType
import com.tencent.bkrepo.common.devops.api.pojo.request.CanwayPermissionRequest
import com.tencent.bkrepo.common.devops.api.pojo.response.CanwayPermissionResponse
import com.tencent.bkrepo.common.devops.api.pojo.response.CanwayResponse
import com.tencent.bkrepo.common.devops.api.util.http.CanwayHttpUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CanwayPermissionService(
    devopsConf: DevopsConf
) {

    private val devopsHost = devopsConf.devopsHost.removeSuffix("/")

    private fun checkUserHasProjectPermission(operator: String): Boolean {
        val canwayPermissionResponse = getCanwayPermissionInstance(
            "bk_ci", operator, CanwayPermissionType.CREATE, "system", "project"
        )
        return checkInstance("bk_ci", canwayPermissionResponse)
    }

    fun checkCanwayPermission(
        projectId: String,
        repoName: String?,
        operator: String,
        action: CanwayPermissionType
    ): Boolean {
        if (checkUserHasProjectPermission(operator)) return true
        val canwayPermissionResponse = getCanwayPermissionInstance(
            projectId, operator, action, BELONGCODE, RESOURCECODE
        )
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
        action: CanwayPermissionType,
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
            val ciAddResourceUrl = "${devopsHost.removeSuffix("/")}$CANWAY_PERMISSION_API$ciCheckPermissionApi"
            val responseContent = CanwayHttpUtils.doPost(ciAddResourceUrl, canwayCheckPermissionRequest).content

            return responseContent.readJsonString<CanwayResponse<CanwayPermissionResponse>>().data
        }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayPermissionService::class.java)
        const val ciCheckPermissionApi = "/api/service/resource_instance/query"
    }
}
