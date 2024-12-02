package com.tencent.bkrepo.auth.controller.user

import com.tencent.bkrepo.auth.pojo.CanwayBkrepoPathPermission
import com.tencent.bkrepo.auth.pojo.CanwayBkrepoPermission
import com.tencent.bkrepo.auth.pojo.permission.CreateRepoPathResourceTypeRequest
import com.tencent.bkrepo.auth.pojo.permission.DeleteRepoPathResourceTypeRequest
import com.tencent.bkrepo.auth.pojo.permission.ListRepoPathInstanceRequest
import com.tencent.bkrepo.auth.pojo.permission.ListRepoPathResourceTypeRequest
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.pojo.permission.PermissionVO
import com.tencent.bkrepo.auth.pojo.permission.RepoPathResourceTypeInstance
import com.tencent.bkrepo.auth.pojo.permission.SaveRepoPathPermission
import com.tencent.bkrepo.auth.pojo.permission.UpdateRepoPathResourceTypeRequest
import com.tencent.bkrepo.auth.service.impl.ExtPermissionServiceImpl
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import net.canway.devops.auth.pojo.resource.action.ResourceActionVO
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@RestController
@RequestMapping("/api/permission")
class ExtPermissionController(
    private val extPermissionServiceImpl: ExtPermissionServiceImpl,
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

    @Principal(PrincipalType.ADMIN)
    @PostMapping("/migrate/delete/permission/repo_create_action")
    fun migrateTodeleteRepoCreateAction():Response<Boolean> {
        extPermissionServiceImpl.migrateTodeleteRepoCreateAction()
        return ResponseBuilder.success(true)
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

    @ApiOperation("创建仓库权限路径集合资源")
    @PostMapping("/resource_type/repo_path_collection/create")
    fun createRepoPathCollectionResourceType(
        @RequestAttribute userId: String,
        @RequestBody request: CreateRepoPathResourceTypeRequest
    ): Response<Boolean> {
        extPermissionServiceImpl.createRepoPathCollectionResourceType(userId, request)
        return ResponseBuilder.success(true)
    }

    @ApiOperation("修改仓库权限路径集合资源")
    @PostMapping("/resource_type/repo_path_collection/update")
    fun updateRepoPathCollectionResourceType(
        @RequestAttribute userId: String,
        @RequestBody request: UpdateRepoPathResourceTypeRequest
    ): Response<Boolean> {
        extPermissionServiceImpl.updateRepoPathCollectionResourceType(userId, request)
        return ResponseBuilder.success(true)
    }

    @ApiOperation("删除仓库权限路径集合资源")
    @PostMapping("/resource_type/repo_path_collection/delete")
    fun deleteRepoPathCollectionResourceType(
        @RequestAttribute userId: String,
        @RequestBody request: DeleteRepoPathResourceTypeRequest
    ): Response<Boolean> {
        extPermissionServiceImpl.deleteRepoPathCollectionResourceType(userId, request)
        return ResponseBuilder.success(true)
    }

    @ApiOperation("查询仓库权限路径集合资源")
    @PostMapping("/resource_type/repo_path_collection/list")
    fun listRepoPathCollectionResourceType(
        @RequestAttribute userId: String,
        @RequestBody request: ListRepoPathResourceTypeRequest
    ): Response<List<Permission>> {
        return ResponseBuilder.success(extPermissionServiceImpl.listRepoPathCollectionResourceType(userId, request))
    }

    @ApiOperation("（路径授权使用）查询项目下仓库权限路径集合资源实例")
    @PostMapping("/resource_type/repo_path_collection/instance/list")
    fun listRepoPathCollectionResourceInstance(
        @RequestAttribute userId: String,
        @RequestBody request: ListRepoPathInstanceRequest
    ): Response<List<RepoPathResourceTypeInstance>> {
        return ResponseBuilder.success(extPermissionServiceImpl.listRepoPathCollectionResourceInstance(userId, request))
    }

    @ApiOperation("（路径授权使用）查询权限路径资源动作")
    @PostMapping("/resource_type/repo_path_collection/action")
    fun listRepoPathCollectionResourceAction(
        @RequestAttribute userId: String,
    ): Response<List<ResourceActionVO>> {
        return ResponseBuilder.success(extPermissionServiceImpl.listRepoPathCollectionResourceAction(userId))
    }

    @Operation(summary = "（路径授权使用）查询仓库路径集合权限")
    @GetMapping("/repo_path_collection/{projectId}/{subjectCode}/{subjectId}/permission")
    fun listRepoPathCollectionPermissions(
        @PathVariable
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @PathVariable
        @Parameter(description = "授权主体类型", required = true)
        subjectCode: String,
        @PathVariable
        @Parameter(description = "授权主体Id", required = true)
        subjectId: String,
    ): Response<List<PermissionVO>> {
        return ResponseBuilder.success(
            extPermissionServiceImpl.listRepoPathCollectionPermissions(
                projectId,
                subjectCode,
                subjectId
            )
        )
    }

    @Operation(summary = "（路径授权使用）保存仓库路径集合权限")
    @PostMapping("/repo_path_collection/{projectId}/{subjectCode}/{subjectId}/permission/save")
    fun saveRepoPathCollectionPermissions(
        @RequestAttribute userId: String,
        @PathVariable
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @PathVariable
        @Parameter(description = "授权主体类型", required = true)
        subjectCode: String,
        @PathVariable
        @Parameter(description = "授权主体Id", required = true)
        subjectId: String,
        @RequestBody
        @Parameter(description = "权限信息", required = true)
        request: SaveRepoPathPermission
    ): Response<Boolean> {
        extPermissionServiceImpl.saveRepoPathCollectionPermissions(
            userId,
            projectId,
            subjectCode,
            subjectId,
            request
        )
        return ResponseBuilder.success(
            true
        )
    }

    @Operation(summary = "（路径授权使用）查询用户路径资源最终权限")
    @GetMapping("/repo_path_collection/{projectId}/user/permission/final")
    fun listUserRepoPathCollectionFinalPermissions(
        @RequestAttribute userId: String,
        @PathVariable
        @Parameter(description = "项目ID", required = true)
        projectId: String,
    ): Response<List<PermissionVO>> {
        return ResponseBuilder.success(
            extPermissionServiceImpl.listUserRepoPathCollectionFinalPermissions(
                userId,
                projectId
            )
        )
    }

    @ApiOperation("获取用户的仓库指定路径的动作权限")
    @GetMapping("/list/repo_path/permission/action")
    fun listRepoPathPermissionAction(
        @RequestAttribute userId: String,
        @ApiParam(value = "项目ID")
        @RequestParam projectId: String,
        @ApiParam(value = "仓库名称")
        @RequestParam repoName: String,
        @ApiParam(value = "仓库名称")
        @RequestParam path: String
    ): Response<CanwayBkrepoPathPermission> {
        return ResponseBuilder.success(
            extPermissionServiceImpl.listRepoPermissionAction(
                userId,
                projectId,
                repoName,
                path
            )
        )
    }
}
