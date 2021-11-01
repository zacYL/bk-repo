package com.tencent.bkrepo.opdata.model

import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate

@Document("day_metric")
@CompoundIndexes(
    CompoundIndex(
        name = "day_index",
        def = "{'day':1, 'projectId':1, 'repoName':1, 'repoType':1,'type':1}",
        unique = true,
        background = true
    )
)
data class TDayMetric(
    val day: LocalDate,
    val projectId: String,
    val repoName: String,
    val repoType: RepositoryType,
    val type: EventType,
    val count: Long
)
