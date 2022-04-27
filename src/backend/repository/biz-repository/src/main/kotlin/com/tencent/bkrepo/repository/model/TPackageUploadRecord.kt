package com.tencent.bkrepo.repository.model

import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document

@Document("package_upload_record")
@CompoundIndexes(
    CompoundIndex(
        name = "package_upload_idx",
        def = "{'projectId': 1, 'repoName': 1, 'key': 1, 'version': 1}",
        background = true,
        unique = true
    )
)
data class TPackageUploadRecord(
    var id: String? = null,
    var projectId: String,
    var repoName: String,
    var key: String,
    var name: String,
    var version: String,
    var date: String,
    var userId: String
)
