package com.tencent.bkrepo.scanner.model

import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * 扫描漏洞结果模型
 * 漏洞明细
 */
@Document("scan_result_cve")
@CompoundIndexes(
    CompoundIndex(
        name = "unique_index",
        def = "{'recordId': 1, 'cveId': 1, 'severity': 1}",
        unique = true, background = true
    )
)
data class TScanResultCve(
    var id: String? = null,
    var createdBy: String,
    var createdDate: LocalDateTime,
    var lastModifiedBy: String,
    var lastModifiedDate: LocalDateTime,

    val recordId: String,
    val cveId: String,
    /**
     * 漏洞等级(CRITICAL, HIGH, LOW, MEDIUM)
     */
    val severity: String,
    /**
     * 所属依赖
     */
    val pkgName: String,
    /**
     * 引入版本
     */
    val installedVersion: String,
    /**
     * 漏洞标题
     */
    val title: String,
    /**
     * 漏洞描述
     */
    val description: String? = null,
    /**
     * 修复版本
     */
    val fixedVersion: String? = null,
    /**
     * 修复建议
     */
    val officialSolution: String? = null,
    /**
     * 相关信息
     */
    val reference: List<String>? = null
)
