package com.tencent.bkrepo.repository.controller.user

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.service.metadata.MetadataRepairService
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/repair")
class UserMetadataRepairController(
    private val metadataRepairService: MetadataRepairService
) {
    @ApiOperation("修正元数据的历史数据")
    @Principal(PrincipalType.ADMIN)
    @PostMapping("/metadataUpdate")
    fun metadataUpdate(
    ): Response<Void> {
        metadataRepairService.metadataUpdate()
        return ResponseBuilder.success()
    }

}