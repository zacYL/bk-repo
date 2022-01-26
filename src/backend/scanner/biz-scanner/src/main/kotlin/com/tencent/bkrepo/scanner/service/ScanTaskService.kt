package com.tencent.bkrepo.scanner.service

interface ScanTaskService {
    fun createTask(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String,
        sha256: String
    ): Boolean
}
