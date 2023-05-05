package com.tencent.bkrepo.common.devops.repository.service

import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.devops.CANWAY_AUTH_API
import com.tencent.bkrepo.common.devops.RESOURCECODE
import com.tencent.bkrepo.common.devops.conf.DevopsConf
import com.tencent.bkrepo.common.devops.enums.CanwayPermissionType
import com.tencent.bkrepo.common.devops.pojo.request.CanwayPermissionRequest
import com.tencent.bkrepo.common.devops.pojo.response.CanwayResponse
import com.tencent.bkrepo.common.devops.util.http.SimpleHttpUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CanwayPermissionService(
    devopsConf: DevopsConf
) {

    private val devopsHost = devopsConf.devopsHost.removeSuffix("/")


    @Suppress("TooGenericExceptionCaught")
    fun checkCanwayPermission(
        projectId: String,
        repoName: String?,
        userId: String,
        action: CanwayPermissionType
    ): Boolean {
        return try {
            checkPermissionInstance(
                projectId = projectId,
                userId = userId,
                action = action
            ) ?: false
        } catch (e: Exception) {
            logger.error("Devops permission request failed: ", e)
            false
        }
    }

    fun checkPermissionInstance(
        projectId: String,
        userId: String,
        action: CanwayPermissionType,
        resourceCode: String? = null,
        repoName: String? = null
    ): Boolean? {
        val canwayCheckPermissionRequest = CanwayPermissionRequest(
            userId = userId,
            instanceId = repoName ?: "*",
            resourceCode = resourceCode ?: RESOURCECODE,
            actionCodes = listOf(action.value),
        ).toJsonString()
        val ciAddResourceUrl =
            String.format("${devopsHost.removeSuffix("/")}$CANWAY_AUTH_API$ciCheckPermissionApi", projectId)
        val responseContent = SimpleHttpUtils.doPost(ciAddResourceUrl, canwayCheckPermissionRequest).content
        return responseContent.readJsonString<CanwayResponse<Boolean>>().data
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayPermissionService::class.java)
        const val ciCheckPermissionApi = "/api/service/project/%s/permission/validate"
    }
}
