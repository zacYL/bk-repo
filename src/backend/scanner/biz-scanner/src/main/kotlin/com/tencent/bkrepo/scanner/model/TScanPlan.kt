package com.tencent.bkrepo.scanner.model

import com.tencent.bkrepo.scanner.pojo.enums.PlanType
import com.tencent.bkrepo.scanner.pojo.request.ArtifactRule
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * 扫描方案模型
 */
@Document("scan_plan")
@CompoundIndexes(
    CompoundIndex(
        name = "unique_index",
        def = "{'projectId': 1, 'name': 1, 'type': 1, 'status': 1}",
        unique = true, background = true
    )
)
data class TScanPlan(
    var id: String? = null,
    var createdBy: String,
    var createdDate: LocalDateTime,
    var lastModifiedBy: String,
    var lastModifiedDate: LocalDateTime,

    val projectId: String,
    val name: String,
    val type: PlanType,
    val description: String? = null,
    /**
     * 扫描规则漏洞等级
     */
    val severities: List<String>? = null,
    /**
     * CVE 漏洞白名单
     */
    val whitelists: List<String>? = null,

    /**
     * 开启自动扫描
     */
    val autoScan: Boolean = false,
    /**
     * 自动扫描仓库
     */
    val repoNameList: List<String> = emptyList(),
    /**
     * 自动扫描制品规则
     */
    val artifactRules: List<ArtifactRule> = emptyList(),
    /**
     * 是否删除
     */
    val delete: Boolean = false
)
