package com.tencent.bkrepo.opdata.service

import com.tencent.bkrepo.opdata.pojo.ArtifactMetricsData
import com.tencent.bkrepo.opdata.pojo.VersionOpUpdateRequest
import java.time.LocalDateTime

interface VersionOpService {
    fun update(request: VersionOpUpdateRequest): Boolean

    fun getLatestModifiedTime(): LocalDateTime?

    fun versionSortByDownloads(projectId: String?, repoName: String?): List<ArtifactMetricsData>
}
