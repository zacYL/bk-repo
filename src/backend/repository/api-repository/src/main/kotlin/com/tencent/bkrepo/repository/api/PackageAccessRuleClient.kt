package com.tencent.bkrepo.repository.api

import com.tencent.bkrepo.common.api.constant.REPOSITORY_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.repository.pojo.packages.PackageAccessRule
import io.swagger.annotations.ApiOperation
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Primary
@FeignClient(REPOSITORY_SERVICE_NAME, contextId = "PackageAccessRuleClient")
@RequestMapping("/service/package-access-rule/{projectId}")
interface PackageAccessRuleClient {

    @ApiOperation("查询匹配的制品规则")
    @GetMapping
    fun getMatchedRules(
        @PathVariable projectId: String,
        @RequestParam type: String,
        @RequestParam fullName: String? = null,
    ): Response<List<PackageAccessRule>>

    @ApiOperation("校验是否通过制品规则")
    @GetMapping("/check")
    fun checkPackageAccessRule(
        @PathVariable projectId: String,
        @RequestParam packageKey: String,
        @RequestParam version: String
    ): Response<Boolean>
}
