package com.tencent.bkrepo.scanner.pojo.context

import com.tencent.bkrepo.scanner.config.ExecutorConfig

data class FileScanContext(
    val taskId: String,
    val config: ExecutorConfig,
    val projectId: String,
    val repoName: String,
    val fullPath: String
)
