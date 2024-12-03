package com.tencent.bkrepo.repository.pojo.packages

import java.time.LocalDateTime

data class PackageAccessRule(
    var createdBy: String,
    var createdDate: LocalDateTime,
    var projectId: String,
    var packageType: PackageType,
    var key: String,
    var version: String?,
    var versionRuleType: VersionRuleType?,
    var pass: Boolean,
    var expireDate: LocalDateTime?,
)
