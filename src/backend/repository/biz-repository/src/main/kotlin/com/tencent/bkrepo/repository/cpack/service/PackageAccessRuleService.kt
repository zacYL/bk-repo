package com.tencent.bkrepo.repository.cpack.service

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.repository.pojo.packages.PackageAccessRule
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.request.PackageAccessRuleRequest


interface PackageAccessRuleService {

    fun createRule(request: PackageAccessRuleRequest)

    fun deleteRule(request: PackageAccessRuleRequest)

    fun listRulePage(
        projectId: String,
        pageNumber: Int,
        pageSize: Int,
        type: PackageType?,
        key: String?,
        version: String?,
        pass: Boolean?
    ): Page<PackageAccessRule>

    fun getMatchedRules(projectId: String, type: String, fullName: String?): List<PackageAccessRule>

    fun checkPackageAccessRule(projectId: String, packageKey: String, version: String): Boolean
}
