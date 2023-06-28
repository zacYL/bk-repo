/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.repository.service.repo.impl

import com.tencent.bkrepo.auth.api.ServicePermissionResource
import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_SIZE
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.api.DefaultArtifactInfo
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode.REPOSITORY_NOT_FOUND
import com.tencent.bkrepo.common.artifact.path.PathUtils.ROOT
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.pojo.configuration.RepositoryConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.clean.CleanStatus
import com.tencent.bkrepo.common.artifact.pojo.configuration.clean.RepositoryCleanStrategy
import com.tencent.bkrepo.common.artifact.pojo.configuration.composite.CompositeConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.composite.ProxyChannelSetting
import com.tencent.bkrepo.common.artifact.pojo.configuration.composite.ProxyConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.local.LocalConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.virtual.VirtualConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.virtual.VirtualRepositoryMember
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.mongo.dao.AbstractMongoDao.Companion.ID
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.security.util.RsaUtils
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.SpringContextUtils.Companion.publishEvent
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.stream.event.supplier.EventSupplier
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.config.RepositoryProperties
import com.tencent.bkrepo.repository.dao.PackageDao
import com.tencent.bkrepo.repository.dao.RepositoryDao
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.model.TPackage
import com.tencent.bkrepo.repository.model.TRepository
import com.tencent.bkrepo.repository.pojo.project.RepoRangeQueryRequest
import com.tencent.bkrepo.repository.pojo.proxy.ProxyChannelCreateRequest
import com.tencent.bkrepo.repository.pojo.proxy.ProxyChannelDeleteRequest
import com.tencent.bkrepo.repository.pojo.proxy.ProxyChannelInfo
import com.tencent.bkrepo.repository.pojo.proxy.ProxyChannelUpdateRequest
import com.tencent.bkrepo.repository.pojo.repo.ConnectionStatusInfo
import com.tencent.bkrepo.repository.pojo.repo.RemoteUrlRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoListOption
import com.tencent.bkrepo.repository.pojo.repo.RepoUpdateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import com.tencent.bkrepo.repository.service.node.NodeService
import com.tencent.bkrepo.repository.service.repo.ProjectService
import com.tencent.bkrepo.repository.service.repo.ProxyChannelService
import com.tencent.bkrepo.repository.service.repo.RepositoryService
import com.tencent.bkrepo.repository.service.repo.StorageCredentialService
import com.tencent.bkrepo.repository.util.RepoCleanRuleUtils
import com.tencent.bkrepo.repository.util.RepoEventFactory.buildCreatedEvent
import com.tencent.bkrepo.repository.util.RepoEventFactory.buildDeletedEvent
import com.tencent.bkrepo.repository.util.RepoEventFactory.buildUpdatedEvent
import com.tencent.bkrepo.repository.util.RuleUtils
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 仓库服务实现类
 */
@Suppress("TooManyFunctions")
@Service
class RepositoryServiceImpl(
    private val repositoryDao: RepositoryDao,
    private val nodeService: NodeService,
    private val projectService: ProjectService,
    private val storageCredentialService: StorageCredentialService,
    private val proxyChannelService: ProxyChannelService,
    private val repositoryProperties: RepositoryProperties,
    private val servicePermissionResource: ServicePermissionResource,
    private val packageDao: PackageDao,
    private val nodeClient: NodeClient,
    private val eventSupplier: EventSupplier
) : RepositoryService {

    override fun getRepoInfo(projectId: String, name: String, type: String?): RepositoryInfo? {
        val tRepository = repositoryDao.findByNameAndType(projectId, name, type)
        return convertToInfo(tRepository)
    }

    override fun getRepoDetail(projectId: String, name: String, type: String?): RepositoryDetail? {
        val tRepository = repositoryDao.findByNameAndType(projectId, name, type)
        val storageCredentials = tRepository?.credentialsKey?.let { storageCredentialService.findByKey(it) }
        return convertToDetail(tRepository, storageCredentials)
    }

    override fun updateStorageCredentialsKey(projectId: String, repoName: String, storageCredentialsKey: String) {
        repositoryDao.findByNameAndType(projectId, repoName, null)?.run {
            oldCredentialsKey = credentialsKey
            credentialsKey = storageCredentialsKey
            repositoryDao.save(this)
        }
    }

    override fun listRepo(
        projectId: String,
        name: String?,
        type: String?,
        category: List<String>?
    ): List<RepositoryInfo> {
        val query = buildListQuery(projectId, name, type, category)
        return repositoryDao.find(query).map { convertToInfo(it)!! }
    }

    override fun listRepoPage(
        projectId: String,
        pageNumber: Int,
        pageSize: Int,
        option: RepoListOption
    ): Page<RepositoryInfo> {
        val query = buildListQuery(projectId, option.name, option.type, option.category)
        val pageRequest = Pages.ofRequest(pageNumber, pageSize)
        val totalRecords = repositoryDao.count(query)
        val records = repositoryDao.find(query.with(pageRequest)).map { convertToInfo(it)!! }
        return Pages.ofResponse(pageRequest, totalRecords, records)
    }

    override fun listPermissionPackageRepo(
        userId: String,
        projectId: String,
        option: RepoListOption
    ): List<RepositoryInfo> {
        return listPermissionRepo(userId, projectId, option).filter {
            it.type != RepositoryType.GENERIC
        }
    }

    override fun listPermissionRepo(
        userId: String,
        projectId: String,
        option: RepoListOption
    ): List<RepositoryInfo> {
        var names = servicePermissionResource.listPermissionRepo(
            projectId = projectId,
            userId = userId,
            appId = SecurityUtils.getPlatformId(),
            actions = option.actions
        ).data.orEmpty()
        if (!option.name.isNullOrBlank()) {
            names = names.filter { it.contains(option.name.orEmpty(), true) }
        }
        val criteria = where(TRepository::projectId).isEqualTo(projectId)
            .and(TRepository::display).ne(false)
            .and(TRepository::name).inValues(names)
            .and(TRepository::deleted).isEqualTo(null)
        option.type?.takeIf { it.isNotBlank() }?.apply { criteria.and(TRepository::type).isEqualTo(this.toUpperCase()) }
        option.category?.takeIf { it.isNotEmpty() }
            ?.apply { criteria.and(TRepository::category).inValues(this.map { it.toUpperCase() }) }
        val query = Query(criteria).with(Sort.by(Sort.Direction.DESC, TRepository::createdDate.name))
        return repositoryDao.find(query).map { convertToInfo(it)!! }
    }

    override fun listPermissionRepoPage(
        userId: String,
        projectId: String,
        pageNumber: Int,
        pageSize: Int,
        option: RepoListOption
    ): Page<RepositoryInfo> {
        val allRepos = listPermissionRepo(userId, projectId, option)
        return Pages.buildPage(allRepos, pageNumber, pageSize)
    }

    override fun listRepoByTypes(projectId: String, types: List<String>): List<RepositoryInfo> {
        val query = buildListQuery(projectId)
            .addCriteria(TRepository::type.inValues(types.map { it.toUpperCase() }))
        return repositoryDao.find(query).map { convertToInfo(it)!! }
    }

    override fun rangeQuery(request: RepoRangeQueryRequest): Page<RepositoryInfo?> {
        val limit = request.limit
        val skip = request.offset
        val projectId = request.projectId

        val criteria = if (request.repoNames.isEmpty()) {
            where(TRepository::projectId).isEqualTo(projectId)
        } else {
            where(TRepository::projectId).isEqualTo(projectId).and(TRepository::name).inValues(request.repoNames)
        }
        criteria.and(TRepository::deleted).isEqualTo(null)
        val totalCount = repositoryDao.count(Query(criteria))
        val records = repositoryDao.find(Query(criteria).limit(limit).skip(skip))
            .map { convertToInfo(it) }
        return Page(0, limit, totalCount, records)
    }

    override fun checkExist(projectId: String, name: String, type: String?): Boolean {
        return repositoryDao.findByNameAndType(projectId, name, type) != null
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun createRepo(repoCreateRequest: RepoCreateRequest): RepositoryDetail {
        with(repoCreateRequest) {
            Preconditions.matchPattern(name, REPO_NAME_PATTERN, this::name.name)
            Preconditions.checkArgument((description?.length ?: 0) <= REPO_DESC_MAX_LENGTH, this::description.name)
            Preconditions.checkArgument(checkInterceptorConfig(configuration), this::configuration.name)
            Preconditions.checkArgument(checkConfigurationType(category, configuration), this::configuration.name)
            // 确保项目一定存在
            if (!projectService.checkExist(projectId)) {
                throw ErrorCodeException(ArtifactMessageCode.PROJECT_NOT_FOUND, name)
            }
            // 确保同名仓库不存在
            if (checkExist(projectId, name)) {
                throw ErrorCodeException(ArtifactMessageCode.REPOSITORY_EXISTED, name)
            }
            // 解析存储凭证
            val credentialsKey = determineStorageKey(this)
            // 确保存储凭证Key一定存在
            val storageCredential = credentialsKey?.takeIf { it.isNotBlank() }?.let {
                storageCredentialService.findByKey(it) ?: throw ErrorCodeException(
                    CommonMessageCode.RESOURCE_NOT_FOUND,
                    it
                )
            }
            // 校验或初始化仓库配置
            val repoConfiguration = configuration?.also {
                if (it is VirtualConfiguration) {
                    checkVirtualConfiguration(it, projectId, type.name)
                }
            } ?: buildRepoConfiguration(this)
            // 创建仓库
            val repository = TRepository(
                name = name,
                type = type,
                category = category,
                public = public,
                description = description,
                configuration = repoConfiguration.toJsonString(),
                credentialsKey = credentialsKey,
                projectId = projectId,
                createdBy = operator,
                createdDate = LocalDateTime.now(),
                lastModifiedBy = operator,
                lastModifiedDate = LocalDateTime.now(),
                quota = quota,
                used = 0,
                coverStrategy = coverStrategy
            )
            return try {
                if (repoConfiguration is CompositeConfiguration) {
                    val old = queryCompositeConfiguration(projectId, name, type)
                    updateCompositeConfiguration(repoConfiguration, old, repository, operator)
                }
                repository.configuration = cryptoConfigurationPwd(repoConfiguration, false).toJsonString()

                // 查找是否存在假删除的仓库，如果存在则删除旧仓库再插入新数据
                val criteria = where(TRepository::projectId).isEqualTo(projectId)
                    .and(TRepository::name).isEqualTo(name)
                    .and(TRepository::deleted).ne(null)
                if (repositoryDao.remove(Query(criteria)).deletedCount != 0L) {
                    logger.info("Retrieved deleted record of Repository[$projectId/$name] before creating")
                }

                repositoryDao.insert(repository)
                val event = buildCreatedEvent(repoCreateRequest)
                publishEvent(event)
                eventSupplier.delegateToSupplier(
                    event = event,
                    topic = event.topic,
                    key = event.getFullResourceKey()
                )
                logger.info("Create repository [$repoCreateRequest] success.")
                convertToDetail(repository, storageCredential)!!
            } catch (exception: DuplicateKeyException) {
                logger.warn("Insert repository[$projectId/$name] error: [${exception.message}]")
                getRepoDetail(projectId, name, type.name)!!
            }
        }
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun updateRepo(repoUpdateRequest: RepoUpdateRequest) {
        val repository = checkRepository(repoUpdateRequest.projectId, repoUpdateRequest.name)
        repoUpdateRequest.apply {
            Preconditions.checkArgument((description?.length ?: 0) <= REPO_DESC_MAX_LENGTH, this::description.name)
            Preconditions.checkArgument(checkInterceptorConfig(configuration), this::description.name)
            quota?.let {
                Preconditions.checkArgument(it >= (repository.used ?: 0), this::quota.name)
                repository.quota = it
            }
            val oldConfiguration = repository.configuration.readJsonString<RepositoryConfiguration>()
            repository.public = public ?: repository.public
            repository.description = description ?: repository.description
            repository.lastModifiedBy = operator
            repository.lastModifiedDate = LocalDateTime.now()
            configuration?.let {
                updateRepoConfiguration(it, cryptoConfigurationPwd(oldConfiguration), repository, operator)
                repository.configuration = cryptoConfigurationPwd(it, false).toJsonString()
            }
            coverStrategy?.let { repository.coverStrategy = it }
            repositoryDao.save(repository)
        }
        val event = buildUpdatedEvent(repoUpdateRequest, repository.type)
        publishEvent(event)
        eventSupplier.delegateToSupplier(
            event = event,
            topic = event.topic,
            key = event.getFullResourceKey()
        )
        logger.info("Update repository[$repoUpdateRequest] success.")
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun deleteRepo(repoDeleteRequest: RepoDeleteRequest) {
        val repository = checkRepository(repoDeleteRequest.projectId, repoDeleteRequest.name)
        repoDeleteRequest.apply {
            // 当仓库为依赖源仓库时，如果仓库下没有包则删除仓库下所有节点
            if (repoDeleteRequest.forced ||
                (repository.type != RepositoryType.GENERIC
                        && packageDao.count(
                    Query(
                        Criteria.where(TPackage::projectId.name).`is`(projectId)
                            .and(TPackage::repoName.name).`is`(name)
                    )
                ) == 0L)
            ) {
                nodeService.deleteByPath(projectId, name, ROOT, operator)
            } else {
                val artifactInfo = DefaultArtifactInfo(projectId, name, ROOT)
                nodeService.countFileNode(artifactInfo).takeIf { it == 0L } ?: throw ErrorCodeException(
                    ArtifactMessageCode.REPOSITORY_CONTAINS_FILE
                )
                nodeService.deleteByPath(projectId, name, ROOT, operator)
            }

            // 为避免仓库被删除后节点无法被自动清理的问题，对仓库实行假删除
            if (!repository.id.isNullOrBlank()) {
                repositoryDao.updateFirst(
                    Query(Criteria.where(ID).isEqualTo(repository.id)),
                    Update().set(TRepository::deleted.name, LocalDateTime.now())
                )
            }

            // 删除关联的库
            if (repository.category == RepositoryCategory.COMPOSITE) {
                val configuration = repository.configuration.readJsonString<CompositeConfiguration>()
                configuration.proxy.channelList.forEach {
                    deleteProxyRepo(repository, it)
                }
            }
            if (repository.category == RepositoryCategory.LOCAL || repository.category == RepositoryCategory.REMOTE) {
                updateAssociatedVirtualRepos(projectId, name, repository.type.name, operator)
            }
        }
        publishEvent(buildDeletedEvent(repoDeleteRequest, repository.type))
        logger.info("Delete repository [$repoDeleteRequest] success.")
    }

    override fun allRepos(projectId: String?, repoName: String?, repoType: RepositoryType?): List<RepositoryInfo?> {
        val criteria = where(TRepository::deleted).isEqualTo(null)
        projectId?.let { criteria.and(TRepository::projectId.name).`is`(projectId) }
        repoName?.let { criteria.and(TRepository::name.name).`is`(repoName) }
        repoType?.let { criteria.and(TRepository::type.name).`is`(repoType) }
        val result = repositoryDao.find(Query(criteria))
        return result.map { convertToInfo(it) }
    }

    @Suppress("LoopWithTooManyJumpStatements")
    override fun migrateCleanStrategy(): List<String> {
        // 1. 查询所有仓库
        val allRepos = repositoryDao.find(
            Query(where(TRepository::category).`in`(listOf(RepositoryCategory.COMPOSITE, RepositoryCategory.LOCAL))
                .and(TRepository::type).isEqualTo(RepositoryType.GENERIC))
        ).mapNotNull { convertToInfo(it) }
        val migratedRepos = mutableListOf<String>()
        // 遍历仓库
        repoLoop@ for (repoInfo in allRepos) {
            logger.info("MigrateCleanStrategy: [${repoInfo.projectId}:${repoInfo.name}]")
            val configuration = when (repoInfo.category) {
                RepositoryCategory.LOCAL -> repoInfo.configuration as LocalConfiguration
                RepositoryCategory.COMPOSITE -> repoInfo.configuration as CompositeConfiguration
                else -> {
                    logger.warn("Unknown repo category: $repoInfo")
                    continue@repoLoop
                }
            }
            val cleanStrategy = configuration.cleanStrategy ?: continue@repoLoop
            val reserveDays = cleanStrategy.reserveDays
            // 提取规则
            var rule = RepoCleanRuleUtils.extractRule(cleanStrategy)
            if (rule == null) {
                configuration.cleanStrategy = RepositoryCleanStrategy(
                    status = cleanStrategy.status,
                    autoClean = cleanStrategy.autoClean,
                    reserveVersions = cleanStrategy.reserveVersions,
                    reserveDays = cleanStrategy.reserveDays,
                    rule = defaultRootClean(repoInfo, reserveDays)
                )
                updateRepoConfiguration(repoInfo, configuration)
                migratedRepos.add("${repoInfo.projectId}/${repoInfo.name}:${repoInfo.type}")
            } else {
                // 先合并path 的条件规则
                val targetPathNestedRules = mutableListOf<Rule.NestedRule>()
                pathNestedLoop@for (pathNestedRule in rule.rules) {
                    if (pathNestedRule is Rule.NestedRule) {
                        val queryRules = pathNestedRule.rules
                        val pathQueryRule = queryRules.filterIsInstance<Rule.QueryRule>().find { it.field == "path" }
                            ?: continue@pathNestedLoop
                        // 在合并后后规则集中寻找是否存在重复路径
                        val existPathNestedRule = targetPathNestedRules.find {
                            it.rules.filterIsInstance<Rule.QueryRule>()
                                .find { path -> path.value == pathQueryRule.value } != null
                        }
                        // 过滤掉 path及reverseDays 条件
                        val filterRules = queryRules.filterIsInstance<Rule.QueryRule>()
                            .filter { it.field != "path" && !it.field.startsWith("reverseDays") }

                        if (existPathNestedRule == null) {
                            if (filterRules.isNullOrEmpty()) {
                                pathNestedRule.rules.add(
                                    Rule.QueryRule("id", "null", OperationType.NE)
                                )
                            }
                            targetPathNestedRules.add(pathNestedRule)
                        } else {
                            // 这里意味着 path 对应的规则为全部
                            if (filterRules.isNullOrEmpty() &&
                                existPathNestedRule.rules
                                    .filterIsInstance<Rule.QueryRule>().find { it.field == "id" } == null
                            ) {
                                existPathNestedRule.rules.add(Rule.QueryRule("id", "null", OperationType.NE))
                            } else {
                                existPathNestedRule.rules.addAll(filterRules)
                            }
                        }
                    }
                }
                rule = Rule.NestedRule(targetPathNestedRules as MutableList<Rule>, Rule.NestedRule.RelationType.OR)

                var rootPathExist = false
                // 遍历条件规则
                pathNestedLoop@for (pathNestedRule in rule.rules) {
                    if (pathNestedRule is Rule.NestedRule) {
                        val rules = pathNestedRule.rules
                        val matchRules = mutableListOf<Rule.QueryRule>()
                        val oldMatchRules = rules.filterIsInstance<Rule.QueryRule>()
                        if (oldMatchRules.isNullOrEmpty()) {
                            // 无效规则
                            rule.rules.remove(pathNestedRule)
                        } else {
                            oldMatchRules.forEach { eachRule ->
                                if (eachRule.field == "path" && eachRule.value == "/") {
                                    rootPathExist = true
                                }
                                if (eachRule.field == "name" ||
                                    eachRule.field.startsWith("metadata.") ||
                                    eachRule.field == "id") {
                                    matchRules.add(eachRule)
                                }
                            }
                            // 空的匹配规则，在旧版中空表示全部数据
                            if (matchRules.isEmpty() && rules.filterIsInstance<Rule.NestedRule>().isEmpty()) {
                                matchRules.add(Rule.QueryRule("id", "null", OperationType.NE))
                            }
                        }
                        rules.removeIf { it is Rule.QueryRule &&
                            (it.field == "name" || it.field.startsWith("metadata.") || it.field == "id") }
                        rules.add(Rule.NestedRule(matchRules as MutableList<Rule>, Rule.NestedRule.RelationType.OR))
                        val queryFields = rules.filterIsInstance<Rule.QueryRule>().map { it.field }
                        if (!queryFields.contains("reserveDays")) {
                            rules.add(Rule.QueryRule("reserveDays", reserveDays))
                        }
                    }
                }
                if (!rootPathExist) {
                    rule.rules.add(0, Rule.NestedRule(
                        mutableListOf(
                            Rule.QueryRule("path", "/", OperationType.REGEX),
                            Rule.QueryRule("reserveDays", reserveDays, OperationType.LTE),
                            Rule.NestedRule(mutableListOf(), Rule.NestedRule.RelationType.OR)
                        ),
                        Rule.NestedRule.RelationType.AND))
                }
                val tRepo = repositoryDao.findByNameAndType(repoInfo.projectId, repoInfo.name)
                configuration.cleanStrategy = RepoCleanRuleUtils.replaceRule(cleanStrategy, rule)
                if (tRepo != null) {
                    try {
                        updateRepo(
                            RepoUpdateRequest(
                                projectId = repoInfo.projectId,
                                name = repoInfo.name,
                                configuration = configuration,
                                coverStrategy = repoInfo.coverStrategy,
                                operator = "system"
                            )
                        )
                        migratedRepos.add("${repoInfo.projectId}/${repoInfo.name}:${repoInfo.type}")
                    } catch(e: Exception) {
                        logger.warn("Migrate clean strategy failed: $repoInfo", e)
                    }
                } else {
                    logger.warn("Failed to find repo[$repoInfo], maybe it has been deleted.")
                }
            }
        }
        return migratedRepos
    }

    private fun defaultRootClean(repo: RepositoryInfo, reserveDays: Long): Rule.NestedRule {
        return Rule.NestedRule(
            rules = mutableListOf(
                Rule.QueryRule(field = "projectId", value = repo.projectId, operation = OperationType.EQ),
                Rule.QueryRule(field = "repoName", value = repo.name, operation = OperationType.EQ),
                Rule.NestedRule(
                    rules = mutableListOf(
                        Rule.NestedRule(
                            rules = mutableListOf(
                                Rule.QueryRule("path", "/", OperationType.REGEX),
                                Rule.QueryRule("reserveDays", reserveDays, OperationType.LTE),
                                Rule.NestedRule(
                                    rules = mutableListOf(
                                        Rule.QueryRule("id", "null", OperationType.NE)
                                    ),
                                    relation = Rule.NestedRule.RelationType.OR
                                )
                            ),
                            relation = Rule.NestedRule.RelationType.AND
                        )
                    ),
                    relation = Rule.NestedRule.RelationType.OR
                )
            ),
            relation = Rule.NestedRule.RelationType.AND
        )
    }

    private fun updateRepoConfiguration(repo: RepositoryInfo, configuration: RepositoryConfiguration) {
        try {
            updateRepo(
                RepoUpdateRequest(
                    projectId = repo.projectId,
                    name = repo.name,
                    configuration = configuration,
                    operator = "system"
                )
            )
        } catch(e: Exception) {
            logger.warn("Migrate clean strategy failed: $repo", e)
        }
    }

    override fun getRepoCleanStrategy(
        projectId: String,
        repoName: String
    ): RepositoryCleanStrategy? {
        val configuration = getRepoInfo(projectId, repoName)?.configuration
        requireNotNull(configuration) { "configuration is null at repository: [$repoName] " }
        if (configuration is LocalConfiguration) {
            configuration.cleanStrategy?.let {
                return it
            }
        }
        return null
    }

    override fun updateCleanStatusRunning(
        projectId: String,
        repoName: String
    ) {
        val repository = checkRepository(projectId, repoName)
        val configuration = repository.configuration.readJsonString<RepositoryConfiguration>()
        if (configuration is LocalConfiguration) {
            val repoCleanStrategy = getRepoCleanStrategy(projectId, repoName)
            repoCleanStrategy?.let {
                require(it.status == CleanStatus.WAITING) {
                    "projectId:[$projectId] repoName:[$repoName] " +
                            "update status to running fail original status is [${it.status}]"
                }
                it.status = CleanStatus.RUNNING
                configuration.cleanStrategy = it
                repository.configuration = configuration.toJsonString()
                repositoryDao.save(repository)
                logger.info(
                    "projectId:[$projectId] repoName:[$repoName] " +
                            "update clean strategy status to [RUNNING] success"
                )
            }
        }
    }

    override fun updateCleanStatusWaiting(
        projectId: String,
        repoName: String
    ) {
        val repository = checkRepository(projectId, repoName)
        val configuration = repository.configuration.readJsonString<RepositoryConfiguration>()
        if (configuration is LocalConfiguration) {
            val repoCleanStrategy = getRepoCleanStrategy(projectId, repoName)
            repoCleanStrategy?.let {
                it.status = CleanStatus.WAITING
                configuration.cleanStrategy = it
                repository.configuration = configuration.toJsonString()
                repositoryDao.save(repository)
                logger.info(
                    "projectId:[$projectId] repoName:[$repoName] " +
                            "update clean strategy status to [WAITING] success"
                )
            }
        }
    }

    override fun allRepoPage(skip: Long): List<TRepository> {
        val query = Query.query(
            Criteria.where(TRepository::category.name)
                .`in`(RepositoryCategory.COMPOSITE, RepositoryCategory.LOCAL)
                .and(TRepository::deleted).isEqualTo(null)
        ).skip(skip).limit(DEFAULT_PAGE_SIZE)
        return repositoryDao.find(query, TRepository::class.java)
    }

    override fun testRemoteUrl(remoteUrlRequest: RemoteUrlRequest): ConnectionStatusInfo {
        val remoteRepository = ArtifactContextHolder.getRepository(RepositoryCategory.REMOTE) as RemoteRepository
        val remoteConfiguration = RemoteConfiguration(
            url = remoteUrlRequest.url,
            credentials = remoteUrlRequest.credentials,
            network = remoteUrlRequest.network
        )
        return try {
            val response = remoteRepository.getResponse(remoteConfiguration)
            val reason = HttpStatus.valueOf(response.code()).reasonPhrase
            ConnectionStatusInfo(response.code() < 400, "${response.code()} $reason")
        } catch (exception: SocketTimeoutException) {
            ConnectionStatusInfo(false, "${HttpStatus.REQUEST_TIMEOUT.value} ${HttpStatus.REQUEST_TIMEOUT.name}")
        } catch (exception: UnknownHostException) {
            ConnectionStatusInfo(false, ("Unknown Host" + exception.message?.let { ": $it" }))
        } catch (exception: Exception) {
            ConnectionStatusInfo(false, exception.message ?: exception.javaClass.simpleName)
        }
    }

    /**
     * 获取仓库下的代理地址信息
     */
    private fun queryCompositeConfiguration(
        projectId: String,
        repoName: String,
        repoType: RepositoryType
    ): CompositeConfiguration? {
        val proxyList = proxyChannelService.listProxyChannel(projectId, repoName, repoType)
        if (proxyList.isEmpty()) return null
        val proxy = ProxyConfiguration(proxyList.map { convertProxyToProxyChannelSetting(it) })
        return CompositeConfiguration(proxy)
    }

    /**
     * 检查仓库是否存在，不存在则抛异常
     */
    private fun checkRepository(projectId: String, repoName: String, repoType: String? = null): TRepository {
        return repositoryDao.findByNameAndType(projectId, repoName, repoType)
            ?: throw ErrorCodeException(REPOSITORY_NOT_FOUND, repoName)
    }

    /**
     * 构造list查询条件
     */
    private fun buildListQuery(
        projectId: String,
        repoName: String? = null,
        repoType: String? = null,
        category: List<String>? = null
    ): Query {
        val criteria = where(TRepository::projectId).isEqualTo(projectId)
        criteria.and(TRepository::display).ne(false)
        criteria.and(TRepository::deleted).isEqualTo(null)
        repoName?.takeIf { it.isNotBlank() }?.apply { criteria.and(TRepository::name).regex("^$this") }
        repoType?.takeIf { it.isNotBlank() }?.apply { criteria.and(TRepository::type).isEqualTo(this.toUpperCase()) }
        category?.takeIf { it.isNotEmpty() }
            ?.apply { criteria.and(TRepository::category).inValues(this.map { it.toUpperCase() }) }
        return Query(criteria).with(Sort.by(Sort.Direction.DESC, TRepository::createdDate.name))
    }

    /**
     * 构造仓库初始化配置
     */
    private fun buildRepoConfiguration(request: RepoCreateRequest): RepositoryConfiguration {
        return when (request.category) {
            RepositoryCategory.LOCAL -> LocalConfiguration()
            RepositoryCategory.REMOTE -> RemoteConfiguration()
            RepositoryCategory.VIRTUAL -> VirtualConfiguration()
            RepositoryCategory.COMPOSITE -> CompositeConfiguration()
        }
    }

    /**
     * 更新仓库配置
     */
    private fun updateRepoConfiguration(
        new: RepositoryConfiguration,
        old: RepositoryConfiguration,
        repository: TRepository,
        operator: String
    ) {
        val newType = new::class.simpleName
        val oldType = old::class.simpleName
        if (newType != oldType) {
            throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "configuration type")
        }
        if (new is CompositeConfiguration && old is CompositeConfiguration) {
            updateCompositeConfiguration(new, old, repository, operator)
        }
        if (new is VirtualConfiguration) {
            checkVirtualConfiguration(new, repository.projectId, repository.type.name)
        }
        if (new is LocalConfiguration && old is LocalConfiguration) {
            if (logger.isDebugEnabled) {
                logger.debug(
                    "projectId:[${repository.projectId}] repoName:[${repository.name}] " +
                            "new clean strategy is [${new.cleanStrategy}], old is [${old.cleanStrategy}]"
                )
            }
            val newCleanStrategy = new.cleanStrategy
            val oldCleanStrategy = old.cleanStrategy
            if (newCleanStrategy != null) {
                if (oldCleanStrategy != null && oldCleanStrategy.status == CleanStatus.RUNNING) {
                    logger.warn(
                        "projectId:[${repository.projectId}] repoName:[${repository.name}], clean job:" +
                                "[${repository.id}] is running, the modification will take effect at the next execution"
                    )
                }
                Preconditions.checkArgument(newCleanStrategy.reserveVersions >= 0, "reserveVersions")
//                Preconditions.checkArgument(newCleanStrategy.reserveDays >= 0, "reserveDays")
                // 校验元数据保留规则中包含的正则表达式
                checkMetadataRuleWrapper(newCleanStrategy.rule, repository.projectId, repository.name)
            } else {
                logger.warn(
                    "projectId:[${repository.projectId}] repoName:[${repository.name}] new clean strategy is null"
                )
                new.cleanStrategy = old.cleanStrategy
            }
        }
    }

    /**
     * 检查元数据保留规则
     * 1.检查规则中的正则表达式是否符合语法规则
     * 2.检查规则中的目录是否存在
     */
    private fun checkMetadataRule(rule: Rule?, projectId: String, repoName: String, paths: MutableList<String>) {
        if (rule is Rule.NestedRule && rule.rules.isNotEmpty()) {
            rule.rules.forEach {
                when (it) {
                    is Rule.NestedRule -> checkMetadataRule(it, projectId, repoName, paths)
                    is Rule.QueryRule -> {
//                        checkPath(it, projectId, repoName)
                        if (it.field == "path") paths.add(it.value as String)
                        checkReserveDays(it)
                        RuleUtils.checkRuleRegex(it)
                    }
                    is Rule.FixedRule -> {
//                        checkPath(it.wrapperRule, projectId, repoName)
                        checkReserveDays(it.wrapperRule)
                        RuleUtils.checkRuleRegex(it.wrapperRule)
                    }
                }
            }
        }
    }

    private fun checkMetadataRuleWrapper(rule: Rule?, projectId: String, repoName: String) {
        val paths = mutableListOf<String>()
        checkMetadataRule(rule, projectId, repoName, paths)
        if (paths.isNotEmpty()) {
            val repeatPaths = mutableListOf<String>()
            paths.forEach {
                if (paths.count { path -> path == it } > 1) {
                    repeatPaths.add(it)
                }
            }
            if (repeatPaths.isNotEmpty()) {
                throw ErrorCodeException(
                    messageCode = CommonMessageCode.REPO_CLEAN_PATH_EXISTED,
                    params = arrayOf(repeatPaths.distinct().joinToString(";"))
                )
            }
        }
    }

    /**
     * 检验保留天数是否合法
     */
    private fun checkReserveDays(queryRule: Rule.QueryRule) {
        if (queryRule.field == "reserveDays") {
            val value = (queryRule.value)
            if (value is Int) {
                if (value < 0) {
                    throw ErrorCodeException(
                        messageCode = CommonMessageCode.PARAMETER_INVALID,
                        params = arrayOf("rule", "reserveDays must be greater than or equal to 0")
                    )
                }
            } else if (value is Long) {
                if (value < 0) {
                    throw ErrorCodeException(
                        messageCode = CommonMessageCode.PARAMETER_INVALID,
                        params = arrayOf("rule", "reserveDays must be greater than or equal to 0")
                    )
                }
            } else {
                logger.error("reserveDays value is $value")
                throw ErrorCodeException(
                    messageCode = CommonMessageCode.PARAMETER_INVALID,
                    params = arrayOf("rule", "reserveDays must be an integer")
                )
            }
        }
    }

    /**
     * 检查页面填写的【目录】 在当前 projectId  repoName 是否存在
     */
    @Deprecated("")
    private fun checkPath(queryRule: Rule.QueryRule, projectId: String, repoName: String) {
        if (queryRule.field == TNode::path.name) {
            val path = queryRule.value.toString()
            if (path == "/") return
            val projectIdRule = Rule.QueryRule(TNode::projectId.name, projectId)
            val repoNameRule = Rule.QueryRule(TNode::repoName.name, repoName)
            val folderRule = Rule.QueryRule(TNode::folder.name, true)
            val fullPathRule = Rule.QueryRule(TNode::fullPath.name, path)
            val queryFullPath = Rule.NestedRule(mutableListOf(projectIdRule, repoNameRule, folderRule, fullPathRule))
            val queryModel = QueryModel(
                page = PageLimit(1, DEFAULT_PAGE_SIZE),
                sort = null,
                select = null,
                rule = queryFullPath
            )
            val queryResult = nodeClient.search(queryModel).data?.records
            if (queryResult == null || queryResult.isEmpty())
                throw ErrorCodeException(CommonMessageCode.DIRECTORY_NOT_EXIST, path)
        }
    }

    /**
     * 更新Composite类型仓库配置
     */
    private fun updateCompositeConfiguration(
        new: CompositeConfiguration,
        old: CompositeConfiguration? = null,
        repository: TRepository,
        operator: String
    ) {
        // 校验
        new.proxy.channelList.forEach {
            Preconditions.checkNotBlank(it.name, "name")
            Preconditions.checkNotBlank(it.url, "url")
        }
        val newProxyProxyRepos = new.proxy.channelList
        val existProxyProxyRepos = old?.proxy?.channelList ?: emptyList()

        val newProxyRepoMap = newProxyProxyRepos.associateBy { it.name }
        val existProxyRepoMap = existProxyProxyRepos.associateBy { it.name }
        Preconditions.checkArgument(newProxyRepoMap.size == newProxyProxyRepos.size, "channelList")

        val toCreateList = mutableListOf<ProxyChannelSetting>()
        val toDeleteList = mutableListOf<ProxyChannelSetting>()
        val toUpdateList = mutableListOf<ProxyChannelSetting>()

        // 查找要添加的代理库
        newProxyRepoMap.forEach { (name, channel) ->
            existProxyRepoMap[name]?.let {
                // 查找要更新的代理库
                if (channel != it) {
                    toUpdateList.add(channel)
                }
            } ?: run { toCreateList.add(channel) }
        }
        // 查找要删除的代理库
        existProxyRepoMap.forEach { (name, channel) ->
            if (!newProxyRepoMap.containsKey(name)) {
                toDeleteList.add(channel)
            }
        }
        // 创建新的代理库
        toCreateList.forEach {
            try {
                createProxyRepo(repository, it, operator)
            } catch (e: DuplicateKeyException) {
                logger.warn("[${it.name}] exist in project[${repository.projectId}], skip creating proxy repo.")
            }
        }
        // 删除旧的代理库
        toDeleteList.forEach {
            deleteProxyRepo(repository, it)
        }
        // 更新旧的代理库
        toUpdateList.forEach {
            updateProxyRepo(repository, it, operator)
        }
    }

    /**
     * 删除关联的代理仓库
     */
    private fun deleteProxyRepo(repository: TRepository, proxy: ProxyChannelSetting) {
        val proxyRepository = ProxyChannelDeleteRequest(
            repoType = repository.type,
            projectId = repository.projectId,
            repoName = repository.name,
            name = proxy.name
        )
        proxyChannelService.deleteProxy(proxyRepository)
        logger.info(
            "Success to delete private proxy channel [${proxy.name}]" +
                " in repo[${repository.projectId}|${repository.name}]"
        )
    }

    private fun createProxyRepo(repository: TRepository, proxy: ProxyChannelSetting, operator: String) {
        // 创建代理仓库
        val proxyRepository = ProxyChannelCreateRequest(
            repoType = repository.type,
            projectId = repository.projectId,
            repoName = repository.name,
            url = proxy.url,
            name = proxy.name,
            username = proxy.username,
            password = proxy.password,
            public = proxy.public,
            credentialKey = proxy.credentialKey,
            networkProxy = proxy.networkProxy,
            connectTimeout = proxy.connectTimeout,
            readTimeout = proxy.readTimeout
        )
        proxyChannelService.createProxy(operator, proxyRepository)
        logger.info("Success to create private proxy repository[$proxyRepository]")
    }

    private fun updateProxyRepo(repository: TRepository, proxy: ProxyChannelSetting, operator: String) {
        // 更新代理仓库
        val proxyRepository = ProxyChannelUpdateRequest(
            repoType = repository.type,
            projectId = repository.projectId,
            repoName = repository.name,
            url = proxy.url,
            name = proxy.name,
            username = proxy.username,
            password = proxy.password,
            public = proxy.public,
            credentialKey = proxy.credentialKey,
            networkProxy = proxy.networkProxy,
            connectTimeout = proxy.connectTimeout,
            readTimeout = proxy.readTimeout
        )
        proxyChannelService.updateProxy(operator, proxyRepository)
        logger.info("Success to update private proxy repository[$proxyRepository]")
    }

    override fun listRepoPageByType(type: String, pageNumber: Int, pageSize: Int): Page<RepositoryDetail> {
        val query = Query(TRepository::type.isEqualTo(type))
            .addCriteria(TRepository::deleted.isEqualTo(null))
            .with(Sort.by(TRepository::name.name))
        val count = repositoryDao.count(query)
        val pageQuery = query.with(PageRequest.of(pageNumber, pageSize))
        val data = repositoryDao.find(pageQuery).map {
            val storageCredentials = it.credentialsKey?.let { key -> storageCredentialService.findByKey(key) }
            convertToDetail(it, storageCredentials)!!
        }

        return Page(pageNumber, pageSize, count, data)
    }

    /**
     * 解析存储凭证key
     * 规则：
     * 1. 如果请求指定了storageCredentialsKey，则使用指定的
     * 2. 如果没有指定，则根据仓库名称进行匹配storageCredentialsKey
     * 3. 如果配有匹配到，则根据仓库类型进行匹配storageCredentialsKey
     * 3. 如果以上都没匹配，则使用全局默认storageCredentialsKey
     */
    private fun determineStorageKey(request: RepoCreateRequest): String? {
        with(repositoryProperties) {
            return if (!request.storageCredentialsKey.isNullOrBlank()) {
                request.storageCredentialsKey
            } else if (repoStorageMapping.names.containsKey(request.name)) {
                repoStorageMapping.names[request.name]
            } else if (repoStorageMapping.types.containsKey(request.type)) {
                repoStorageMapping.types[request.type]
            } else {
                defaultStorageCredentialsKey
            }
        }
    }

    /**
     * 检查下载拦截器配置
     * 规则：
     *  filename不为空字符串
     *  metadata是键值对形式
     */
    @Suppress("UNCHECKED_CAST")
    private fun checkInterceptorConfig(configuration: RepositoryConfiguration?): Boolean {
        val config = configuration?.getSetting<List<Map<String, Any>>>(INTERCEPTORS)
        config?.forEach {
            val rules = it[RULES] as Map<String, String>
            val filename = rules[FILENAME]
            if (filename != null && filename.isBlank()) {
                return false
            }
            val metadata = rules[METADATA]
            if (metadata != null && metadata.split(StringPool.COLON).size != 2) {
                return false
            }
        }
        return true
    }

    private fun checkConfigurationType(category: RepositoryCategory, configuration: RepositoryConfiguration?): Boolean {
        return configuration?.let {
            when (category) {
                RepositoryCategory.COMPOSITE -> it is CompositeConfiguration
                RepositoryCategory.LOCAL -> it is LocalConfiguration && it !is CompositeConfiguration
                RepositoryCategory.REMOTE -> it is RemoteConfiguration
                RepositoryCategory.VIRTUAL -> it is VirtualConfiguration
            }
        } ?: true
    }

    private fun checkVirtualConfiguration(
        configuration: VirtualConfiguration,
        projectId: String,
        type: String
    ) {
        with (configuration) {
            repositoryList = repositoryList.distinctBy { it.name }
            // 校验虚拟仓库配置中的仓库列表
            repositoryList.forEach {
                val repoCategory = checkRepository(projectId, it.name, type).category
                if (it.category == null) {
                    it.category = repoCategory
                }
                Preconditions.checkArgument(
                    repoCategory == it.category &&
                            (repoCategory == RepositoryCategory.LOCAL || repoCategory == RepositoryCategory.REMOTE),
                    this::repositoryList.name
                )
            }
            // 校验部署仓库
            deploymentRepo.takeUnless { it.isNullOrBlank() }?.run {
                Preconditions.checkArgument(
                    VirtualRepositoryMember(this, RepositoryCategory.LOCAL) in repositoryList,
                    configuration::deploymentRepo.name
                )
            }
        }
    }

    private fun updateAssociatedVirtualRepos(
        projectId: String,
        repoName: String,
        type: String,
        operator: String
    ) {
        listRepo(
            projectId = projectId,
            type = type,
            category = listOf(RepositoryCategory.VIRTUAL.name)
        ).forEach {
            val virtualConfiguration = it.configuration as VirtualConfiguration
            val newRepoList = virtualConfiguration.repositoryList.toMutableList()
            virtualConfiguration.repositoryList = newRepoList.apply {
                removeIf { member -> member.name == repoName }
            }
            if (virtualConfiguration.deploymentRepo == repoName) {
                virtualConfiguration.deploymentRepo = null
            }
            val tRepository = checkRepository(projectId, it.name, type).apply {
                configuration = virtualConfiguration.toJsonString()
                lastModifiedBy = operator
                lastModifiedDate = LocalDateTime.now()
            }
            repositoryDao.save(tRepository)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryServiceImpl::class.java)
        private const val REPO_NAME_PATTERN = "[a-zA-Z_][a-zA-Z0-9\\.\\-_]{1,63}"
        private const val REPO_DESC_MAX_LENGTH = 200
        private const val INTERCEPTORS = "interceptors"
        private const val RULES = "rules"
        private const val FILENAME = "filename"
        private const val METADATA = "metadata"

        private fun convertToDetail(
            tRepository: TRepository?,
            storageCredentials: StorageCredentials? = null
        ): RepositoryDetail? {
            return tRepository?.let {
                RepositoryDetail(
                    name = it.name,
                    type = it.type,
                    category = it.category,
                    public = it.public,
                    description = it.description,
                    configuration = cryptoConfigurationPwd(it.configuration.readJsonString()),
                    storageCredentials = storageCredentials,
                    projectId = it.projectId,
                    createdBy = it.createdBy,
                    createdDate = it.createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    lastModifiedBy = it.lastModifiedBy,
                    lastModifiedDate = it.lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    quota = it.quota,
                    coverStrategy = it.coverStrategy,
                    used = it.used,
                    oldCredentialsKey = it.oldCredentialsKey
                )
            }
        }

        private fun convertToInfo(tRepository: TRepository?): RepositoryInfo? {
            return tRepository?.let {
                RepositoryInfo(
                    name = it.name,
                    type = it.type,
                    category = it.category,
                    public = it.public,
                    description = it.description,
                    configuration = cryptoConfigurationPwd(it.configuration.readJsonString()),
                    storageCredentialsKey = it.credentialsKey,
                    projectId = it.projectId,
                    createdBy = it.createdBy,
                    createdDate = it.createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    lastModifiedBy = it.lastModifiedBy,
                    lastModifiedDate = it.lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    quota = it.quota,
                    used = it.used,
                    coverStrategy = it.coverStrategy
                )
            }
        }

        private fun convertProxyToProxyChannelSetting(proxy: ProxyChannelInfo): ProxyChannelSetting {
            with(proxy) {
                return ProxyChannelSetting(
                    public = public,
                    name = name,
                    url = url,
                    credentialKey = credentialKey,
                    username = username,
                    password = password
                )
            }
        }

        /**
         * 加/解密密码
         */
        fun cryptoConfigurationPwd(
            repoConfiguration: RepositoryConfiguration,
            decrypt: Boolean = true
        ): RepositoryConfiguration {
            if (repoConfiguration is CompositeConfiguration) {
                repoConfiguration.proxy.channelList.forEach {
                    it.password?.let { pw ->
                        it.password = crypto(pw, decrypt)
                    }
                }
            }
            if (repoConfiguration is RemoteConfiguration) {
                repoConfiguration.credentials.password?.let {
                    repoConfiguration.credentials.password = crypto(it, decrypt)
                }
            }
            return repoConfiguration
        }

        private fun crypto(pw: String, decrypt: Boolean): String {
            return if (!decrypt) {
                RsaUtils.encrypt(pw)
            } else {
                try {
                    RsaUtils.decrypt(pw)
                } catch (e: Exception) {
                    pw
                }
            }
        }
    }
}
