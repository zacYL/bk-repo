package com.tencent.bkrepo.repository.service.repo.impl

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.constant.PUBLIC_PROXY_PROJECT
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.devops.DEPLOY_CANWAY
import com.tencent.bkrepo.common.devops.client.DevopsProjectClient
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.repository.dao.RepositoryDao
import com.tencent.bkrepo.repository.model.TRepository
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import com.tencent.bkrepo.repository.service.repo.SoftwareRepositoryService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class SoftwareRepositoryServiceImpl(
    private val repositoryDao: RepositoryDao
) : SoftwareRepositoryService {

    @Value("\${auth.realm}")
    lateinit var realm: String

    @Autowired(required = false)
    lateinit var devopsProjectClient: DevopsProjectClient

    override fun listRepoPage(
        projectId: String?,
        pageNumber: Int,
        pageSize: Int,
        name: String?,
        type: RepositoryType?
    ): Page<RepositoryInfo> {
        val ciProjects = if (realm == DEPLOY_CANWAY) {
            val projects = devopsProjectClient.projects()
            if (projects == null || projects.isEmpty()) {
                logger.info("no ciProject found")
                return Pages.buildPage(emptyList(), 1, 20)
            }
            projects.map { it.projectCode }
        } else { null }
        ciProjects?.let { logger.info("ciProjects: ${it.toJsonString()}") }
        val query = buildListQuery(projectId, name, type, includeGeneric = true, ciProjects = ciProjects)
        val pageRequest = Pages.ofRequest(pageNumber, pageSize)
        val totalRecords = repositoryDao.count(query)
        val records = repositoryDao.find(query.with(pageRequest)).map { convertToInfo(it)!! }
        return Pages.ofResponse(pageRequest, totalRecords, records)
    }

    override fun listRepo(
        projectId: String?,
        name: String?,
        type: RepositoryType?,
        includeGeneric: Boolean
    ): List<RepositoryInfo> {
        val ciProjects = if (realm == DEPLOY_CANWAY) {
            val projects = devopsProjectClient.projects()
            if (projects == null || projects.isEmpty()) {
                logger.info("no ciProject found")
                return listOf()
            }
            projects.map { it.projectCode }
        } else { null }
        ciProjects?.let { logger.info("ciProjects: ${it.toJsonString()}") }
        val query = buildListQuery(projectId, name, type, includeGeneric, ciProjects = ciProjects)
        return repositoryDao.find(query).map { convertToInfo(it)!! }
    }

    /**
     * 构造list查询条件
     */
    private fun buildListQuery(
        projectId: String?,
        repoName: String? = null,
        repoType: RepositoryType? = null,
        includeGeneric: Boolean,
        ciProjects: List<String>? = null
    ): Query {
        val publicCriteria = where(TRepository::public).`is`(true)
        val systemCriteria = where(TRepository::configuration).regex("\\\"system\\\"( )?:( )?true")
        val criteria = Criteria()
        if (projectId != null && projectId.isNotBlank()) {
            criteria.and(TRepository::projectId).`is`(projectId)
        } else if (ciProjects != null && ciProjects.isNotEmpty()) {
            criteria.and(TRepository::projectId).inValues(ciProjects)
        } else {
            criteria.and(TRepository::projectId).ne(PUBLIC_PROXY_PROJECT)
        }
        criteria.and(TRepository::display).ne(false)
        if (repoType == null && !includeGeneric) criteria.and(TRepository::type).ne(RepositoryType.GENERIC)
        if (repoType != null) {
            criteria.and(TRepository::type).isEqualTo(repoType)
        }
        criteria.orOperator(publicCriteria, systemCriteria)
        repoName?.takeIf { it.isNotBlank() }?.apply { criteria.and(TRepository::name).regex("^$this") }
        return Query(criteria).with(Sort.by(Sort.Direction.DESC, TRepository::createdDate.name))
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SoftwareRepositoryServiceImpl::class.java)
        private fun convertToInfo(tRepository: TRepository?): RepositoryInfo? {
            return tRepository?.let {
                RepositoryInfo(
                    name = it.name,
                    type = it.type,
                    category = it.category,
                    public = it.public,
                    description = it.description,
                    configuration = it.configuration.readJsonString(),
                    storageCredentialsKey = it.credentialsKey,
                    projectId = it.projectId,
                    createdBy = it.createdBy,
                    createdDate = it.createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    lastModifiedBy = it.lastModifiedBy,
                    lastModifiedDate = it.lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    quota = it.quota,
                    used = it.used
                )
            }
        }
    }
}
