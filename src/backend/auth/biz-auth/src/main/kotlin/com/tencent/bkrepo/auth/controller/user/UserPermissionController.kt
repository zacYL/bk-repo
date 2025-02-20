package com.tencent.bkrepo.auth.controller.user

import com.tencent.bkrepo.auth.constant.AUTH_API_PERMISSION_PREFIX
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(AUTH_API_PERMISSION_PREFIX)
class UserPermissionController(
    private val permissionService: PermissionService
) {

    @GetMapping("/admin")
    fun isAdmin(
        @RequestAttribute userId: String,
        @RequestParam projectId: String
    ): Response<Boolean> {
        val result = permissionService.checkPermission(
            CheckPermissionRequest(
                uid = userId,
                resourceType = ResourceType.PROJECT,
                projectId = projectId,
                action = PermissionAction.MANAGE
            )
        )
        return ResponseBuilder.success(result)
    }

}
