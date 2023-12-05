package com.tencent.bkrepo.repository.controller.user

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.pojo.storage.DiskCleanInfo
import com.tencent.bkrepo.repository.service.repo.StorageCleanService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Api("全局制品清理接口")
@RestController
@Principal(PrincipalType.ADMIN)
@RequestMapping("/api/clean")
class UserCleanController(
    private val StorageCleanService: StorageCleanService
) {
    @ApiOperation("磁盘清理预估")
    @GetMapping("/disk/compute")
    fun computeCleanDisk(): Response<DiskCleanInfo> {
        return ResponseBuilder.success(StorageCleanService.computeCleanDisk())
    }

    @ApiOperation("执行磁盘清理")
    @PostMapping("/disk/execute")
    fun executeDiskClean(): Response<Void> {
        StorageCleanService.executeDiskClean()
        return ResponseBuilder.success()
    }
}
