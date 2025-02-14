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
    private val storageCleanService: StorageCleanService
) {
    @ApiOperation("磁盘清理预估")
    @GetMapping("/disk/compute")
    fun computeCleanDisk(): Response<DiskCleanInfo> {
        return ResponseBuilder.success(storageCleanService.computeCleanDisk())
    }

    @ApiOperation("执行磁盘清理")
    @Deprecated("使用Job服务执行清理任务")
    @PostMapping("/disk/execute")
    fun executeDiskClean(): Response<Void> {
        storageCleanService.executeDiskClean()
        return ResponseBuilder.success()
    }
}
