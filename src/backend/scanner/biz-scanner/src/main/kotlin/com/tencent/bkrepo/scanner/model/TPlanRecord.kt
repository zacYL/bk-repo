package com.tencent.bkrepo.scanner.model

import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * 方案记录结果模型
 */
@Document("plan_record")
@CompoundIndexes(
    CompoundIndex(
        name = "unique_index",
        def = "{'planId': 1, 'triggerMethod': 1, 'repoType': 1, 'repoName': 1, 'packageKey': 1, 'version': 1}",
        unique = true, background = true
    )
)
data class TPlanRecord(
    var id: String? = null,
    var createdBy: String,
    var createdDate: LocalDateTime,
    var lastModifiedBy: String,
    var lastModifiedDate: LocalDateTime,

    val planId: String,
    /**
     * 触发方式
     */
    val triggerMethod: String,
    val projectId: String,
    val packageKey: String? = null,
    val version: String? = null,

    var fullPath: String? = null,
    val repoName: String,
    val repoType: String,
    /**
     * 制品名称
     */
    val artifactName: String,
    /**
     * 扫描状态
     *
     */
    val scanStatus: String,
    /**
     * 扫描工具
     */
    val scanTool: String,
    /**
     * 容器ID
     */
    var containerId: String? = null,
    /**
     * 扫描持续时长
     */
    val duration: Long = 0,
    /**
     * 质量结果
     */
    val qualityResult: String? = null,

    /**
     * cve原始报告内容
     */
    val cveContent: String = "",
    /**
     * 最高漏洞等级
     */
    var highestLeakLevel: String? = null,
    /**
     * 危急漏洞数
     */
    var critical: Int = 0,
    /**
     * 高危漏洞数
     */
    var high: Int = 0,
    /**
     * 中危漏洞数
     */
    var medium: Int = 0,
    /**
     * 低危漏洞数
     */
    var low: Int = 0,

    /**
     * 是否删除
     * 每次插入把历史数据删除
     */
    val delete: Boolean = false

)
