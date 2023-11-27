package com.tencent.bkrepo.auth.controller.user

import com.tencent.bkrepo.auth.pojo.CanwayBkrepoPermission
import com.tencent.bkrepo.auth.service.impl.ExtPermissionServiceImpl
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestParam

@RestController
@RequestMapping("/api/permission")
class ExtPermissionController(
    private val extPermissionServiceImpl: ExtPermissionServiceImpl
) {
    @Principal(PrincipalType.ADMIN)
    @GetMapping("/migrate")
    fun migrate() {
        extPermissionServiceImpl.migHistoryPermissionData()
    }

    @Principal(PrincipalType.ADMIN)
    @PostMapping("/migrateToDevOps")
    fun migrateToDevOps() {
        extPermissionServiceImpl.migrateToDevOps()
    }

    @ApiOperation("获取用户的项目/仓库权限列表")
    @GetMapping("/list/indevops")
    fun listDevOpsPermission(
        @RequestAttribute userId: String,
        @ApiParam(value = "项目ID")
        @RequestParam projectId: String,
        @ApiParam(value = "仓库名称")
        @RequestParam repoName: String?
    ): Response<List<CanwayBkrepoPermission>> {
        return ResponseBuilder.success(extPermissionServiceImpl.listDevOpsPermission(userId, projectId, repoName))
    }
}
