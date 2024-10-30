package com.tencent.bkrepo.repository.cpack.controller.user

import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.repository.cpack.service.impl.CanwayProjectService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.PathVariable

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
        @ApiParam(value = "租户ID", required = true)
        tenantId: String,
        @PathVariable
        @ApiParam(value = "协同模板ID", required = true)
        issueId: String
    ) {
        canwayProjectService.canwayProjectMigrate(userId, tenantId, issueId)
    }
}
