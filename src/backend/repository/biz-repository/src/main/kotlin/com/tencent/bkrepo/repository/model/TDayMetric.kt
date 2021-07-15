package com.tencent.bkrepo.repository.model

import com.tencent.bkrepo.repository.pojo.log.OperateType
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate

@Document("day_metric")
@CompoundIndexes(
    CompoundIndex(
        name = "day_index",
        def = "{'day':1, 'projectId':1, 'repoName':1, 'operateType':1}",
        unique = true,
        background = true
    )
)
data class TDayMetric(
    val day: LocalDate,
    val projectId: String,
    val repoName: String,
    val operateType: OperateType,
    val count: Long
)
