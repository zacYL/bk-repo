package com.tencent.bkrepo.repository.controller.user

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.repository.pojo.storage.StoragePojo
import com.tencent.bkrepo.repository.service.repo.StorageManageService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.pojo.storage.RepoLogicStoragePojo
import com.tencent.bkrepo.repository.pojo.storage.RepoStorageInfoParam
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Api("存储管理")
@RestController
@Principal(PrincipalType.ADMIN)
@RequestMapping("/api/storage")
class UserStoragetroller(
    private val storageManageService: StorageManageService
) {
    @ApiOperation("获取磁盘数据")
    @GetMapping("/info")
    fun info(): Response<StoragePojo> {
        return ResponseBuilder.success(storageManageService.info())
    }

    @ApiOperation("获取项目仓库存储数据")
    @GetMapping("/info/repos")
    fun infoRepos(
        option: RepoStorageInfoParam
    ): Response<Page<RepoLogicStoragePojo>> {
        return ResponseBuilder.success(storageManageService.infoRepos(option))
    }
}
