package com.tencent.bkrepo.auth.controller.user

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestParam

@RestController
@RequestMapping("/api/permission")
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
                resourceType = ResourceType.PROJECT.name,
                projectId = projectId,
                action = PermissionAction.MANAGE.name
            )
        )
        return ResponseBuilder.success(result)
    }

    @ApiOperation("获取项目内置权限列表")
    @GetMapping("/list/inproject")
    fun listProjectBuiltinPermission(
        @ApiParam(value = "项目ID")
        @RequestParam projectId: String
    ): Response<List<Permission>> {
        return ResponseBuilder.success(permissionService.listProjectBuiltinPermission(projectId))
    }
}
