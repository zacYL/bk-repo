package com.tencent.bkrepo.scanner.pojo.cve

data class CvssV3X(
    val attackComplexity: String,
    val attackVector: String,
    val availability_impact: String,
    val base_score: Double,
    val confidentiality_impact: String,
    val integrity_impact: String,
    val privileges_required: String,
    val scope: String,
    val user_interaction: String
)