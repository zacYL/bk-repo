package com.tencent.bkrepo.scanner.model

import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document

@Document("dependency_check")
@CompoundIndexes(
    CompoundIndex(
        name = "unique_index",
        def = "{'projectId': 1, 'repoName': 1, 'packageKey': 1, 'version': 1, 'scanId': 1}",
        unique = true, background = true
    )
)
data class TDependency(
    val projectId: String,
    val repoName: String,
    val packageKey: String,
    val version: String,
    val applyId: String,
    val description: String?,
    val evidenceCollected: String,
    val fileName: String,
    val isVirtual: Boolean,
    val license: String?,
    val md5: String,
    val packages: String,
    val sha1: String,
    val sha256: String,
    val vulnerabilityIds: String?,
    val vulnerabilities: List<String>?
)
