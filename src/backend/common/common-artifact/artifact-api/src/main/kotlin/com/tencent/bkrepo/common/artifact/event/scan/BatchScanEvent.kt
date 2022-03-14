package com.tencent.bkrepo.common.artifact.event.scan

/**
 * 批量扫描事件
 */
data class BatchScanEvent(
    val userId: String,
    val projectId: String,
    val planId: String
)