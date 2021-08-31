package com.tencent.bkrepo.opdata.model

import com.tencent.bkrepo.repository.pojo.packages.PackageType
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("package_version_op")
@CompoundIndexes(
    CompoundIndex(
        name = "project_repo_key_version",
        def = "{'projectId':1, 'repoName':1, 'packageKey':1, 'packageVersion':1 }", background = true, unique = true
    )
)
data class TVersionOp(
    var id: String?,
    var projectId: String,
    var repoName: String,
    var packageKey: String,
    val type: PackageType,
    var packageId: String,
    var packageName: String,
    var packageVersion: String,
    var size: Long,
    var downloads: Long,
    var lastModifiedDate: LocalDateTime
)
