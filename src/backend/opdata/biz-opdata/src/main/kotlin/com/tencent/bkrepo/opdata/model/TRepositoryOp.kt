package com.tencent.bkrepo.opdata.model

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("repository_op")
@CompoundIndexes(
    CompoundIndex(
        name = "project_repo_type",
        def = "{'projectId':1, 'repoName':1, 'repoType':1}", background = true, unique = true
    )
)
data class TRepositoryOp(
    val id: String? = null,
    val projectId: String,
    val repoName: String,
    val repoType: RepositoryType,
    val visits: Long,
    val downloads: Long,
    val uploads: Long,
    val usedCapacity: Long,
    val packages: Long,
    val latestModifiedDate: LocalDateTime
)
