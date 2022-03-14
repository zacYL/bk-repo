package com.tencent.bkrepo.scanner.service

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.scanner.pojo.ArtifactCountInfo
import com.tencent.bkrepo.scanner.pojo.ArtifactLeakInfo
import com.tencent.bkrepo.scanner.pojo.enums.LeakType
import com.tencent.bkrepo.scanner.pojo.request.AtomScanRequest
import com.tencent.bkrepo.scanner.pojo.request.BatchScanRequest
import com.tencent.bkrepo.scanner.pojo.request.SingleScanRequest

interface ScanService {

    fun batchScan(userId: String, request: BatchScanRequest): Boolean

    fun stopScan(userId: String, projectId: String, recordId: String): Boolean

    fun artifactLeak(
        projectId: String,
        recordId: String,
        cveId: String?,
        leakType: LeakType?,
        pageNumber: Int,
        pageSize: Int
    ): Page<ArtifactLeakInfo?>

    fun artifactCount(projectId: String, recordId: String): ArtifactCountInfo

    fun atomScan(userId: String, request: AtomScanRequest)

    fun singleScan(userId: String, request: SingleScanRequest): Boolean

}
