package com.tencent.bkrepo.scanner.pojo.cve

data class NvtoolsCveinfo(
    val can_be_fixed: String,
    val category: String,
    val category_type: String,
    val component: String,
    val cve_id: String,
    val cvss_v2: Any?,
    val cvss_v2_score: String,
    val cvss_v2_vector: String,
    val cvss_v3: CvssV3X?,
    val cvss_v3_score: String,
    val cvss_v3_vector: String,
    val cwe_id: String,
    val defense_solution: String,
    val des: String,
    val dynamic_level: String,
    val is_suggest: Boolean,
    val level: String,
    val name: String,
    val official_solution: String,
    val poc_id: String,
    val reference: String?,
//    val reference: ArrayList<String>,
    val submit_time: String,
    val version: String,
    val version_fixed: String
)