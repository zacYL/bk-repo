package com.tencent.bkrepo.scanner.pojo.cve

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class CvesecItem(
    val cve: String,
    @JsonProperty("cve_status")
    val cveStatus: String,
    @JsonProperty("cve_year")
    val cveYear: String,
    val cvss: Double,
    @JsonProperty("cvss_rank")
    val cvssRank: String,
    @JsonProperty("cvss_v2")
    val cvssV2: CvssV2?,
    @JsonProperty("cvss_v3")
    val cvssV3: CvssV3?,
    val description: String,
    @JsonProperty("err_description")
    val errDescription: ErrDescription?,
    val exp: Any?,
    @JsonProperty("lib_description")
    val libDescription: String,
    @JsonProperty("lib_name")
    val libName: String,
    @JsonProperty("nvtools_cveinfo")
    val nvtoolsCveinfo: NvtoolsCveinfo,
    val patch: Any?,
    val path: String,
    val poc: Any?,
    val signature: String,
    @JsonProperty("task_id")
    val taskId: Long,
    val version: String
)