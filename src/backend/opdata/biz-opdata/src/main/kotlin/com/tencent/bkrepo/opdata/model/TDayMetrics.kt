package com.tencent.bkrepo.opdata.model

import com.tencent.bkrepo.opdata.enum.ArtifactOperationType
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate

@Document("day_metrics")
@CompoundIndexes(
    CompoundIndex(
        name = "day_index",
        def = "{'day':1, 'projectId':1, 'repoName':1, 'operationType':1}",
        unique = true,
        background = true
    )
)
data class TDayMetrics(
    val day: LocalDate,
    val projectId: String,
    val repoName: String,
    val operationType: ArtifactOperationType,
    val count: Long
)
