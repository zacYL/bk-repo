package com.tencent.bkrepo.analyst.service

import com.tencent.bkrepo.analyst.pojo.response.CveWhitelistInfo
import com.tencent.bkrepo.common.api.pojo.Page

interface CveWhitelistService {
    fun insert(cveId: String, userId: String)

    fun insertBatch(cveIds: List<String>, userId: String)

    fun getByCveId(cveId: String): CveWhitelistInfo?

    fun getByCveIds(cveIds: List<String>): List<CveWhitelistInfo?>

    fun getCveList(): List<CveWhitelistInfo?>

    fun deleteByCveId(cveId: String, userId: String)

    fun searchByCveId(cveId: String?, pageNumber: Int?, pageSize: Int?): Page<CveWhitelistInfo>
}
