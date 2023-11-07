package com.tencent.bkrepo.common.devops.repository.api

import com.tencent.bkrepo.common.devops.repository.service.CanwayProjectService
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.PathVariable

@Api("canway 项目")
@RestController
@RequestMapping("/api/cw/project")
class CanwayProjectController {
    @Autowired
    lateinit var canwayProjectService: CanwayProjectService

    @Principal(PrincipalType.ADMIN)
    @ApiOperation("独立制品库->集成制品库 迁移项目")
    @PostMapping("/migrate/{tenantId}/{issueId}")
    fun canwayProjectMigrate(
        @RequestAttribute userId: String,
        @PathVariable
        @Parameter(description = "租户ID", required = true)
        tenantId: String,
        @PathVariable
        @Parameter(description = "协同模板ID", required = true)
        issueId: String
    ) {
        canwayProjectService.canwayProjectMigrate(userId, tenantId, issueId)
    }
}
