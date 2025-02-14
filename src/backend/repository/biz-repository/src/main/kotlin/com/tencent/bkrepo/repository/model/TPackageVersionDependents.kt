package com.tencent.bkrepo.repository.model

import com.tencent.bkrepo.common.metadata.model.TMetadata
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document

@CompoundIndexes(
    CompoundIndex(
        def = "{'projectId': 1, 'repoName': 1, 'packageKey': 1, 'version': 1}",
        unique = true,
        background = true
    )
)
@Document("package_version_dependents")
data class TPackageVersionDependents(
    val id: String?,
    val projectId: String,
    val repoName: String,
    val packageKey: String,
    val version: String,
    val ext: List<TMetadata>? = null,
    val dependents: Set<String>? = null
)
