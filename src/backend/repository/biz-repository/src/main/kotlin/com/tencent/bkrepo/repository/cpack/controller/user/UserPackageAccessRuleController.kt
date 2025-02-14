package com.tencent.bkrepo.repository.cpack.controller.user

import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_NUMBER
import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_SIZE
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.common.metadata.cpack.service.PackageAccessRuleService
import com.tencent.bkrepo.repository.pojo.packages.PackageAccessRule
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.VersionRuleType
import com.tencent.bkrepo.repository.pojo.packages.request.PackageAccessRuleRequest
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Api("CPack制品黑白名单规则")
@RestController
@RequestMapping("/api/package/access-rule")
class UserPackageAccessRuleController(
    private val packageAccessRuleService: PackageAccessRuleService
) {

    @ApiOperation("创建规则")
    @Principal(type = PrincipalType.ADMIN)
    @PostMapping("/create")
    fun createRule(
        @RequestBody packageAccessRuleRequest: PackageAccessRuleRequest
    ): Response<Void> {
        packageAccessRuleService.createRule(packageAccessRuleRequest)
        return ResponseBuilder.success()
    }

    @ApiOperation("移除规则")
    @Principal(type = PrincipalType.ADMIN)
    @DeleteMapping("/delete")
    fun deleteRule(
        @RequestParam(required = true) projectId: String,
        @RequestParam(required = true) packageType: PackageType,
        @RequestParam(required = true) key: String,
        @RequestParam(required = false) version: String? = null,
        @RequestParam(required = false) versionRuleType: VersionRuleType? = null,
        @RequestParam(required = true) pass: Boolean,
    ): Response<Void> {
        val request = PackageAccessRuleRequest(
            projectId = projectId,
            packageType = packageType,
            key = key,
            version = version,
            versionRuleType = versionRuleType,
            pass = pass
        )
        packageAccessRuleService.deleteRule(request)
        return ResponseBuilder.success()
    }

    @ApiOperation("规则分页")
    @Principal(type = PrincipalType.ADMIN)
    @GetMapping("/page")
    fun listRulePage(
        @RequestParam(required = true) projectId: String,
        @RequestParam(required = false, defaultValue = "$DEFAULT_PAGE_NUMBER") pageNumber: Int = DEFAULT_PAGE_NUMBER,
        @RequestParam(required = false, defaultValue = "$DEFAULT_PAGE_SIZE") pageSize: Int = DEFAULT_PAGE_SIZE,
        @RequestParam(required = false) packageType: PackageType? = null,
        @RequestParam(required = false) key: String? = null,
        @RequestParam(required = false) version: String? = null,
        @RequestParam(required = false) pass: Boolean? = null,
    ): Response<Page<PackageAccessRule>> {
        return ResponseBuilder.success(
            packageAccessRuleService.listRulePage(projectId, pageNumber, pageSize, packageType, key, version, pass)
        )
    }

    @ApiOperation("检查是否被制品规则拦截")
    @Principal(type = PrincipalType.GENERAL)
    @GetMapping("/check/{projectId}")
    fun checkPackageAccessRule(
        @PathVariable projectId: String,
        @RequestParam(required = true) packageKey: String,
        @RequestParam(required = true) version: String
    ): Response<Boolean> {
        return ResponseBuilder.success(
            packageAccessRuleService.checkPackageAccessRule(projectId, packageKey, version)
        )
    }
}
