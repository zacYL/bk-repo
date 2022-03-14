package com.tencent.bkrepo.scanner.pojo.context

data class PlanArtifactContext(
    val projectId: String,
    val planId: String,
    val artifactName: String?,
    val highestLeakLevel: String?,
    val repoType: String?,
    val repoName: String?,
    val status: String?,
    val startTime: String?,
    val endTime: String?,
    val pageNumber: Int,
    val pageSize: Int
)
