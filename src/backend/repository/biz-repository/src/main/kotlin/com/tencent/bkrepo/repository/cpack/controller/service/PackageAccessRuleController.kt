package com.tencent.bkrepo.repository.cpack.controller.service

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.api.PackageAccessRuleClient
import com.tencent.bkrepo.repository.cpack.service.PackageAccessRuleService
import com.tencent.bkrepo.repository.pojo.packages.PackageAccessRule
import org.springframework.web.bind.annotation.RestController

@RestController
class PackageAccessRuleController(
    private val packageAccessRuleService: PackageAccessRuleService
) : PackageAccessRuleClient {
    override fun getMatchedRules(
        projectId: String,
        type: String,
        fullName: String?
    ): Response<List<PackageAccessRule>> {
        return ResponseBuilder.success(packageAccessRuleService.getMatchedRules(projectId, type, fullName))
    }
}
