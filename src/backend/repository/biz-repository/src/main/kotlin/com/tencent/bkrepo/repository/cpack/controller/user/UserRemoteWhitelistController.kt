package com.tencent.bkrepo.repository.cpack.controller.user

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.pojo.whitelist.CreateRemotePackageWhitelistRequest
import com.tencent.bkrepo.repository.pojo.whitelist.RemotePackageWhitelist
import com.tencent.bkrepo.repository.pojo.whitelist.UpdateRemotePackageWhitelistRequest
import com.tencent.bkrepo.common.metadata.service.whitelist.RemotePackageWhitelistService
import com.tencent.bkrepo.common.metadata.util.WhitelistUtils
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.constraints.Min

@RestController
@RequestMapping("/api/remote/whitelist")
@Principal(PrincipalType.ADMIN)
class UserRemoteWhitelistController(
        private val remotePackageWhitelistService: RemotePackageWhitelistService
) {

    @GetMapping("/optional/type")
    fun listOptionalType(): Response<List<RepositoryType>> {
        return ResponseBuilder.success(WhitelistUtils.optionalType())
    }

    @PutMapping("")
    fun createWhitelist(
            @RequestBody request: CreateRemotePackageWhitelistRequest
    ): Response<Boolean> {
        WhitelistUtils.packageKeyValidThrow(request.packageKey, request.type)
        return ResponseBuilder.success(remotePackageWhitelistService.createWhitelist(request))
    }

    @PutMapping("/batch")
    fun batchWhitelist(
            @RequestBody request: List<CreateRemotePackageWhitelistRequest>
    ): Response<Int> {
        return ResponseBuilder.success(remotePackageWhitelistService.batchWhitelist(request))
    }

    @PostMapping("/{id}")
    fun updateWhitelist(
            @PathVariable id: String,
            @RequestBody request: UpdateRemotePackageWhitelistRequest
    ): Response<Boolean> {
        return ResponseBuilder.success(remotePackageWhitelistService.updateWhitelist(id, request))
    }

    @DeleteMapping("/{id}")
    fun deleteWhitelist(
            @PathVariable id: String
    ): Response<Boolean> {
        return ResponseBuilder.success(remotePackageWhitelistService.deleteWhitelist(id))
    }

    @GetMapping("/{id}")
    fun getWhitelist(
            @PathVariable id: String
    ): Response<RemotePackageWhitelist?> {
        return ResponseBuilder.success(remotePackageWhitelistService.getWhitelist(id))
    }

    @GetMapping("/page")
    fun page(
            @RequestParam type: RepositoryType?,
            @RequestParam packageKey: String?,
            @RequestParam version: String?,
            @ApiParam(value = "当前页", required = false, defaultValue = "0")
            @RequestParam @Min(0, message = "pageNumber起始为0") pageNumber: Int?,
            @ApiParam(value = "分页大小", required = false, defaultValue = "20")
            @RequestParam @Min(1, message = "pageSize分页至少需有一条数据") pageSize: Int?,
            @ApiParam(value = "是否正则匹配", required = false, defaultValue = "true")
            @RequestParam regex: Boolean?
    ): Response<Page<RemotePackageWhitelist>> {
        return ResponseBuilder.success(
                remotePackageWhitelistService.page(type, packageKey, version, pageNumber, pageSize, regex?: true))
    }
}
