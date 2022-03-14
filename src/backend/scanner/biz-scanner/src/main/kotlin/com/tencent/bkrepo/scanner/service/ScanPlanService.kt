package com.tencent.bkrepo.scanner.service

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.scanner.pojo.ArtifactRelationPlan
import com.tencent.bkrepo.scanner.pojo.ScanArtifactInfo
import com.tencent.bkrepo.scanner.pojo.ScanPlanBase
import com.tencent.bkrepo.scanner.pojo.ScanPlanInfo
import com.tencent.bkrepo.scanner.pojo.context.ArtifactPlanContext
import com.tencent.bkrepo.scanner.pojo.context.PlanArtifactContext
import com.tencent.bkrepo.scanner.pojo.enums.PlanType
import com.tencent.bkrepo.scanner.pojo.request.ScanPlanRequest

interface ScanPlanService {

    fun createScanPlan(userId: String, request: ScanPlanRequest): Boolean

    fun scanPlanList(projectId: String, type: PlanType?): List<ScanPlanBase>

    fun scanPlanList(
        projectId: String,
        type: PlanType?,
        name: String?,
        pageNumber: Int,
        pageSize: Int
    ): Page<ScanPlanInfo?>

    fun getScanPlanBase(projectId: String, id: String): ScanPlanBase?

    fun updateStatus(userId: String, projectId: String, id: String): Boolean

    fun updateScanPlan(userId: String, request: ScanPlanRequest): Boolean

    fun countInfo(projectId: String, id: String): ScanPlanInfo?

    fun planArtifactList(paramContext: PlanArtifactContext): Page<ScanArtifactInfo?>

    fun artifactPlanList(paramContext: ArtifactPlanContext): List<ArtifactRelationPlan>?

    fun artifactPlanStatus(paramContext: ArtifactPlanContext): String?

}
