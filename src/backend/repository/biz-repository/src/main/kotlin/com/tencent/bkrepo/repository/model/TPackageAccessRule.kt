package com.tencent.bkrepo.repository.model

import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.VersionRuleType
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("package_access_rule")
@CompoundIndexes(
    CompoundIndex(
        name = "package_access_rule_idx",
        def = "{'projectId': 1, 'packageType': 1, 'key': 1, 'version': 1, 'versionRuleType': 1, 'pass': 1}",
        background = true,
        unique = true
    )
)
data class TPackageAccessRule(
    var id: String? = null,
    var createdBy: String,
    var createdDate: LocalDateTime,
    var projectId: String,
    var packageType: PackageType,
    var key: String,
    var version: String? = null,
    var versionRuleType: VersionRuleType? = null,
    var pass: Boolean
)
