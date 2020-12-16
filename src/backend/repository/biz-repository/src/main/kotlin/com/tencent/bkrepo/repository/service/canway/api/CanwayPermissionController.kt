package com.tencent.bkrepo.repository.service.canway.api

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.service.canway.service.CanwayPermissionService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam

@Api("canway 鉴权")
@RestController
@RequestMapping("/api/permission")
class CanwayPermissionController {

    @Autowired
    lateinit var canwayPermissionService: CanwayPermissionService

    @ApiOperation("根据项目、仓库、动作查询用户是否有相应权限")
    @GetMapping("/{projectId}/{action}")
    fun checkCanwayPermission(
        @RequestAttribute userId: String,
        @PathVariable projectId: String,
        @PathVariable action: String,
        @RequestParam repoName: String?
    ): Response<Boolean> {
        return ResponseBuilder.success(
            canwayPermissionService.checkCanwayPermission(
                projectId = projectId,
                repoName = repoName,
                operator = userId,
                action = action
            )
        )
    }
}
