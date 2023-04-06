package com.tencent.bkrepo.analyst.service.impl

import com.tencent.bkrepo.analyst.dao.CveWhitelistDao
import com.tencent.bkrepo.analyst.model.TCveWhitelist
import com.tencent.bkrepo.analyst.pojo.response.CveWhitelistInfo
import com.tencent.bkrepo.analyst.service.CveWhitelistService
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.EscapeUtils
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CveWhitelistServiceImpl(
    private val cveWhitelistDao: CveWhitelistDao
) : CveWhitelistService {
    override fun insert(cveId: String, userId: String) {
        if (getByCveId(cveId) != null) return
        cveWhitelistDao.insert(transformToT(cveId, userId))
    }

    override fun insertBatch(cveIds: List<String>, userId: String) {
        cveWhitelistDao.insert(
            cveIds.filter { cveId -> !(getByCveIds(cveIds).map { it?.cveId }.contains(cveId)) }
                .map { transformToT(it, userId) }
        )
    }

    override fun getByCveId(cveId: String): CveWhitelistInfo? {
        return cveWhitelistDao.getByCveId(cveId)?.let {
            transformToInfo(it)
        }
    }

    override fun getByCveIds(cveIds: List<String>): List<CveWhitelistInfo?> {
        val query = Query(Criteria.where(TCveWhitelist::cveId.name).`in`(cveIds))
        val result = cveWhitelistDao.find(query)
        return result.map { transformToInfo(it) }
    }

    override fun getCveList(): List<CveWhitelistInfo?> {
        val result = cveWhitelistDao.findAll()
        return result.map { transformToInfo(it) }
    }

    override fun deleteByCveId(cveId: String, userId: String) {
        logger.info("delete vul whitelist: [$cveId] by user: [$userId]")
        return cveWhitelistDao.deleteByCveId(cveId)
    }

    override fun searchByCveId(cveId: String?, pageNumber: Int?, pageSize: Int?): Page<CveWhitelistInfo> {
        if (pageNumber != null) {
            Preconditions.checkArgument(pageNumber >= 0, "pageNumber must be greater than or equal to 0")
        }
        if (pageSize != null) {
            Preconditions.checkArgument(pageSize >= 0, "pageSize must be greater than or equal to 0")
        }
        val pageRequest = Pages.ofRequest(page = pageNumber ?: default_pageNumber, size = pageSize ?: default_pageSize)
        val query = if (!cveId.isNullOrBlank()) {
            Query(Criteria.where(TCveWhitelist::cveId.name).regex(EscapeUtils.escapeRegex(cveId), "i"))
        } else {
            Query()
        }
        val totalRecord = cveWhitelistDao.count(query)
        val page = cveWhitelistDao.find(
            query
                .with(pageRequest)
                .with(Sort.by(TCveWhitelist::lastModifiedDate.name).descending())
        ).map { transformToInfo(it) }.toList()
        return Pages.ofResponse(pageRequest, totalRecord, page)
    }

    private fun transformToInfo(vulWhitelist: TCveWhitelist): CveWhitelistInfo {
        return CveWhitelistInfo(
            cveId = vulWhitelist.cveId,
            description = vulWhitelist.description,
            createdBy = vulWhitelist.createdBy,
            createdDate = vulWhitelist.createdDate,
            lastModifiedBy = vulWhitelist.lastModifiedBy,
            lastModifiedDate = vulWhitelist.lastModifiedDate
        )
    }

    private fun transformToT(cveId: String, userId: String): TCveWhitelist {
        return TCveWhitelist(
            cveId = cveId,
            description = null,
            createdBy = userId,
            createdDate = LocalDateTime.now(),
            lastModifiedBy = userId,
            lastModifiedDate = LocalDateTime.now()
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CveWhitelistServiceImpl::class.java)
        private const val default_pageNumber = 1
        private const val default_pageSize = 20
    }
}
