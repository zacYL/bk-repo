package com.tencent.bkrepo.scanner.service

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.scanner.pojo.response.CveWhitelistInfo

interface CveWhitelistService {
    fun insert(cveId: String, userId: String)

    fun insertBatch(cveIds: List<String>, userId: String)

    fun getByCveId(cveId: String): CveWhitelistInfo?

    fun getByCveIds(cveIds: List<String>): List<CveWhitelistInfo?>

    fun getCveList(): List<CveWhitelistInfo?>

    fun deleteByCveId(cveId: String, userId: String)

    fun searchByCveId(cveId: String?, pageNumber: Int?, pageSize: Int?): Page<CveWhitelistInfo>
}
