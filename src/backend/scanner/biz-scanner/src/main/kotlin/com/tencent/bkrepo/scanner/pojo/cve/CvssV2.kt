package com.tencent.bkrepo.scanner.pojo.cve

data class CvssV2(
    val access_complexity: String,
    val access_vector: String,
    val authentication: String,
    val availability_impact: String,
    val base_score: Double,
    val confidentiality_impact: String,
    val integrity_impact: String
)