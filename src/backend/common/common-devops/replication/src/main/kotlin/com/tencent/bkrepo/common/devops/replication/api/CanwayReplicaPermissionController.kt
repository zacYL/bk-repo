package com.tencent.bkrepo.common.devops.replication.api

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.devops.enums.CanwayPermissionType
import com.tencent.bkrepo.common.devops.replication.service.CanwayReplicaPermissionService
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@Api("canway 鉴权")
@RestController
@RequestMapping("/api/permission")
class CanwayReplicaPermissionController {

    @Autowired
    lateinit var canwayReplicaPermissionService: CanwayReplicaPermissionService

    @ApiOperation("根据项目、仓库、动作查询用户是否有相应权限")
    @GetMapping("/{projectId}/{action}")
    fun checkCanwayPermission(
        @RequestAttribute userId: String,
        @PathVariable projectId: String,
        @PathVariable action: String,
        @RequestParam repoName: String?
    ): Response<Boolean> {
        return ResponseBuilder.success(
            canwayReplicaPermissionService.checkCanwayPermission(
                projectId = projectId,
                repoName = repoName,
                userId = userId,
                action = CanwayPermissionType.valueOf(action.toUpperCase())
            )
        )
    }

    @ApiOperation("根据用户判断有项目何动作权限")
    @GetMapping("/{projectId}/query")
    fun checkPermissionQuery(
        @RequestAttribute userId: String,
        @PathVariable projectId: String,
        @RequestParam repoName: String?
    ): Response<List<String>> {
        return ResponseBuilder.success(
            canwayReplicaPermissionService.checkPermissionQuery(
                projectId = projectId,
                userId = userId
            )
        )
    }
}
