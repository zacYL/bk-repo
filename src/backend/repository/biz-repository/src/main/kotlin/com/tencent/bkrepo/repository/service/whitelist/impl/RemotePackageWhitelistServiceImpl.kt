package com.tencent.bkrepo.repository.service.whitelist.impl

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.EscapeUtils
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.repository.cpack.controller.RemotePackageWhitelist
import com.tencent.bkrepo.repository.dao.RemotePackageWhitelistDao
import com.tencent.bkrepo.repository.exception.WhitelistNotFoundException
import com.tencent.bkrepo.repository.model.TRemotePackageWhitelist
import com.tencent.bkrepo.repository.pojo.whitelist.CreateRemotePackageWhitelistRequest
import com.tencent.bkrepo.repository.pojo.whitelist.UpdateRemotePackageWhitelistRequest
import com.tencent.bkrepo.repository.service.whitelist.RemotePackageWhitelistService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class RemotePackageWhitelistServiceImpl(
        private val remotePackageWhitelistDao: RemotePackageWhitelistDao,
        private val mongoTemplate: MongoTemplate
) : RemotePackageWhitelistService {

    override fun createWhitelist(request: CreateRemotePackageWhitelistRequest): Boolean {
        page(request.type, request.packageKey, null, null, null, false).records.let {
            if (it.isEmpty()) {
                remotePackageWhitelistDao.insert(transToT(request, SecurityUtils.getUserId()))
            } else {
                updateWhitelist(
                        id = it[0].id,
                        request = UpdateRemotePackageWhitelistRequest(
                                versions = mutableListOf<String>().apply{
                                    it[0].versions?.let { it1 -> addAll(it1) }
                                    request.versions?.let { it1 -> addAll(it1) }
                                }.distinct(),
                        )
                )
            }
        }
        return true
    }

    override fun batchWhitelist(request: List<CreateRemotePackageWhitelistRequest>): Int {
        request.parallelStream()
        var count = 0
        for (i in request.indices) {
            try {
                createWhitelist(request[i])
                count++
            } catch (e: Exception) {
                logger.error("batch whitelist error, index: $i, data: ${request[i]}", e)
            }
        }
        return count
    }

    override fun updateWhitelist(id: String, request: UpdateRemotePackageWhitelistRequest): Boolean {
        val oldWhitelist = getWhitelist(id)?: throw WhitelistNotFoundException(id)
        with(oldWhitelist) {
            Triple(
                    request.type?.let { if (type != it) it else null },
                    request.packageKey?.let { if (packageKey != it) it else null },
                    request.versions?.let {
                        if (versions?.sorted() != it.sorted()) it.distinct() else null }
            ).apply {
                if(first == null && second == null && third == null) return true
            }
        }.apply {
            val query = Query(Criteria.where(TRemotePackageWhitelist::id.name).`is`(id))
            val update = Update().apply {
                first?.let { set(TRemotePackageWhitelist::type.name, it) }
                second?.let { set(TRemotePackageWhitelist::packageKey.name, it) }
                third?.let { set(TRemotePackageWhitelist::versions.name, it) }
                set(TRemotePackageWhitelist::lastModifiedBy.name, SecurityUtils.getUserId())
                set(TRemotePackageWhitelist::lastModifiedDate.name, LocalDateTime.now())
            }
            mongoTemplate.updateFirst(query, update, TRemotePackageWhitelist::class.java)
        }
        return true
    }

    override fun deleteWhitelist(id: String): Boolean {
        remotePackageWhitelistDao.removeById(id)
        return true
    }

    override fun getWhitelist(id: String): RemotePackageWhitelist? {
        return remotePackageWhitelistDao.findById(id)?.let { transToInfo(it) }
    }

    override fun page(
            type: RepositoryType?,
            packageKey: String?,
            version: String?,
            pageNumber: Int?,
            pageSize: Int?,
            regex: Boolean
    ): Page<RemotePackageWhitelist> {
        if (pageNumber != null) {
            Preconditions.checkArgument(pageNumber >= 0, "pageNumber must be greater than or equal to 0")
        }
        if (pageSize != null) {
            Preconditions.checkArgument(pageSize >= 0, "pageSize must be greater than or equal to 0")
        }
        val pageRequest = Pages.ofRequest(page = pageNumber ?: default_pageNumber, size = pageSize ?: default_pageSize)
        val criteria = Criteria()
        type?.let { criteria.and(TRemotePackageWhitelist::type.name).`is`(it) }
        packageKey?.let {
            if (regex) {
                criteria.and(TRemotePackageWhitelist::packageKey.name).regex(EscapeUtils.escapeRegex(it), "i")
            } else {
                criteria.and(TRemotePackageWhitelist::packageKey.name).`is`(it)
            }
        }
        version?.let { criteria.and(TRemotePackageWhitelist::versions.name).`in`(it) }
        val query = Query(criteria)
        val totalRecord = remotePackageWhitelistDao.count(query)
        val page = remotePackageWhitelistDao.find(
                query
                        .with(pageRequest)
                        .with(Sort.by(TRemotePackageWhitelist::lastModifiedDate.name).descending())
        ).map { transToInfo(it) }.toList()
        return Pages.ofResponse(pageRequest, totalRecord, page)
    }

    private fun transToT(
            createRemotePackageWhitelistRequest: CreateRemotePackageWhitelistRequest,
            userId: String
    ): TRemotePackageWhitelist {
        return TRemotePackageWhitelist(
                type = createRemotePackageWhitelistRequest.type,
                packageKey = createRemotePackageWhitelistRequest.packageKey,
                versions = createRemotePackageWhitelistRequest.versions?.distinct(),
                createdBy = userId,
                createdDate = LocalDateTime.now(),
                lastModifiedBy = userId,
                lastModifiedDate = LocalDateTime.now()
        )
    }

    private fun transToInfo(
            tRemotePackageWhitelist: TRemotePackageWhitelist,
    ): RemotePackageWhitelist {
        return RemotePackageWhitelist(
                id = tRemotePackageWhitelist.id!!,
                type = tRemotePackageWhitelist.type,
                packageKey = tRemotePackageWhitelist.packageKey,
                versions = tRemotePackageWhitelist.versions?.sorted(),
                createdBy = tRemotePackageWhitelist.createdBy,
                createdDate = tRemotePackageWhitelist.createdDate,
                lastModifiedBy = tRemotePackageWhitelist.lastModifiedBy,
                lastModifiedDate = tRemotePackageWhitelist.lastModifiedDate
        )
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(RemotePackageWhitelistServiceImpl::class.java)
        private const val default_pageNumber = 1
        private const val default_pageSize = 20
    }
}