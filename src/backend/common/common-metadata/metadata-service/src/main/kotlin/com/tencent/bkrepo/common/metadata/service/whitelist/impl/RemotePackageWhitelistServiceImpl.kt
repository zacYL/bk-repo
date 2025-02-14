package com.tencent.bkrepo.common.metadata.service.whitelist.impl

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.EscapeUtils
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.metadata.dao.whitelist.RemotePackageWhitelistDao
import com.tencent.bkrepo.common.metadata.exception.WhitelistNotFoundException
import com.tencent.bkrepo.common.metadata.model.TRemotePackageWhitelist
import com.tencent.bkrepo.common.metadata.service.whitelist.RemotePackageWhitelistService
import com.tencent.bkrepo.common.metadata.util.WhitelistUtils
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.repository.pojo.whitelist.CreateRemotePackageWhitelistRequest
import com.tencent.bkrepo.repository.pojo.whitelist.RemotePackageWhitelist
import com.tencent.bkrepo.repository.pojo.whitelist.UpdateRemotePackageWhitelistRequest
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
                                    it[0].versions?.let { existsVersions -> addAll(existsVersions) }
                                    request.versions?.let { requestVersions -> addAll(requestVersions) }
                                }.distinct(),
                        )
                )
            }
        }
        return true
    }

    override fun batchWhitelist(request: List<CreateRemotePackageWhitelistRequest>): Int {
        var count = 0
        for (i in request.indices) {
            try {
                request[i].let {
                    if (WhitelistUtils.packageKeyValid(it.packageKey, it.type)) {
                        createWhitelist(it)
                        count++
                    }
                }
            } catch (e: Exception) {
                logger.error("batch whitelist error, index: $i, data: ${request[i]}", e)
            }
        }
        return count
    }

    override fun updateWhitelist(id: String, request: UpdateRemotePackageWhitelistRequest): Boolean {
        val requestPackageKey = request.packageKey?.trim()
        val requestVersions = request.versions
                ?.filter { it.isNotBlank() }?.distinct()?.sorted()
        val oldWhitelist = getWhitelist(id) ?: throw WhitelistNotFoundException(id)
        if (request.type != null) {
            requestPackageKey?.let { WhitelistUtils.packageKeyValidThrow(it, request.type!!) }
        } else {
            requestPackageKey?.let { WhitelistUtils.packageKeyValidThrow(it, oldWhitelist.type) }
        }
        with(oldWhitelist) {
            Triple(
                    request.type?.let { if (type != it) it else null },
                    requestPackageKey?.let { if (packageKey != it) it else null },
                    requestVersions?.let { if (versions?.sorted() != it) it else null }
            ).apply {
                if (first == null && second == null && third == null) return true
            }
        }.apply {
            // packageKey or type changed, check if exists
            if (first != null || second != null) {
                val newType = first ?: oldWhitelist.type
                val newPackageKey = second ?: oldWhitelist.packageKey
                page(newType, newPackageKey, null, null, null, false).records.let {
                    if (it.isNotEmpty()) {
                        throw ErrorCodeException(CommonMessageCode.RESOURCE_EXISTED,
                                "type=$newType; packageKey=$newPackageKey ")
                    }
                }
            }

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
            Preconditions.checkArgument(pageNumber >= 0,
                    "pageNumber must be greater than or equal to 0")
        }
        if (pageSize != null) {
            Preconditions.checkArgument(pageSize >= 0, "pageSize must be greater than or equal to 0")
        }
        val pageRequest = Pages.ofRequest(page = pageNumber ?: default_pageNumber, size = pageSize ?: default_pageSize)
        val criteria = Criteria()
        type?.let { criteria.and(TRemotePackageWhitelist::type.name).`is`(it) }
        if (!packageKey.isNullOrBlank()) {
            criteria.and(TRemotePackageWhitelist::packageKey.name).apply {
                if(regex) regex(EscapeUtils.escapeRegex(packageKey), "i")
                else `is`(packageKey)
            }
        }
        if (!version.isNullOrBlank()) {
            criteria.orOperator(
                Criteria.where(TRemotePackageWhitelist::versions.name).`in`(version),
                Criteria.where(TRemotePackageWhitelist::versions.name).`is`(null),
                Criteria.where(TRemotePackageWhitelist::versions.name).size(0)
            )
        }
        val query = Query(criteria)
        val totalRecord = remotePackageWhitelistDao.count(query)
        val page = remotePackageWhitelistDao.find(
                query
                        .with(pageRequest)
                        .with(Sort.by(TRemotePackageWhitelist::lastModifiedDate.name).descending())
        ).map { transToInfo(it) }.toList()
        return Pages.ofResponse(pageRequest, totalRecord, page)
    }

    override fun existWhitelist(type: RepositoryType?, packageKey: String?, version: String?): Boolean {
        return page(
            type = type,
            packageKey = packageKey,
            version = version,
            regex = false,
            pageNumber = 1,
            pageSize = 1
        ).records.isNotEmpty()
    }

    private fun transToT(
            createRemotePackageWhitelistRequest: CreateRemotePackageWhitelistRequest,
            userId: String
    ): TRemotePackageWhitelist {
        return TRemotePackageWhitelist(
                type = createRemotePackageWhitelistRequest.type,
                packageKey = createRemotePackageWhitelistRequest.packageKey.trim(),
                versions = createRemotePackageWhitelistRequest.versions
                        ?.filter { it.isNotBlank() }?.distinct()?.sorted(),
                createdBy = userId,
                createdDate = LocalDateTime.now(),
                lastModifiedBy = userId,
                lastModifiedDate = LocalDateTime.now()
        )
    }

    private fun versionsFilter(versions: List<String>?): List<String>? {
        val res = versions?.distinct()?.filter { it.isNotBlank() }
        return res?.let { it.ifEmpty { null } }
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
