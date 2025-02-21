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

package com.tencent.bkrepo.common.metadata.service.repo.impl

import com.tencent.bkrepo.auth.api.ServicePermissionClient
import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_SIZE
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode.REPOSITORY_NOT_FOUND
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryIdentify
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
import com.tencent.bkrepo.common.metadata.condition.SyncCondition
import com.tencent.bkrepo.common.metadata.dao.repo.RepositoryDao
import com.tencent.bkrepo.common.metadata.model.TRepository
import com.tencent.bkrepo.common.metadata.service.project.ProjectService
import com.tencent.bkrepo.common.metadata.service.recycle.RecycleBinService
import com.tencent.bkrepo.common.metadata.service.repo.ProxyChannelService
import com.tencent.bkrepo.common.metadata.service.repo.RepositoryService
import com.tencent.bkrepo.common.metadata.service.repo.ResourceClearService
import com.tencent.bkrepo.common.metadata.service.repo.StorageCredentialService
import com.tencent.bkrepo.common.metadata.util.RepoEventFactory.buildCreatedEvent
import com.tencent.bkrepo.common.metadata.util.RepoEventFactory.buildDeletedEvent
import com.tencent.bkrepo.common.metadata.util.RepoEventFactory.buildUpdatedEvent
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper.Companion.REPO_DESC_MAX_LENGTH
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper.Companion.REPO_NAME_PATTERN
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper.Companion.buildChangeList
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper.Companion.buildListPermissionRepoQuery
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper.Companion.buildListQuery
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper.Companion.buildProxyChannelCreateRequest
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper.Companion.buildProxyChannelDeleteRequest
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper.Companion.buildProxyChannelUpdateRequest
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper.Companion.buildRangeQuery
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper.Companion.buildRepoConfiguration
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper.Companion.buildSingleQuery
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper.Companion.buildTypeQuery
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper.Companion.checkCategory
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper.Companion.checkConfigType
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper.Companion.checkInterceptorConfig
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper.Companion.convertProxyToProxyChannelSetting
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper.Companion.convertToDetail
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper.Companion.convertToInfo
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper.Companion.cryptoConfigurationPwd
import com.tencent.bkrepo.common.metadata.util.RepositoryServiceHelper.Companion.determineStorageKey
import com.tencent.bkrepo.common.mongo.dao.AbstractMongoDao.Companion.ID
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.cluster.condition.DefaultCondition
import com.tencent.bkrepo.common.service.util.SpringContextUtils.Companion.publishEvent
import com.tencent.bkrepo.common.stream.event.supplier.MessageSupplier
import com.tencent.bkrepo.repository.constant.SYSTEM_REPO
import com.tencent.bkrepo.repository.pojo.node.NodeSizeInfo
import com.tencent.bkrepo.repository.pojo.project.RepoRangeQueryRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoListOption
import com.tencent.bkrepo.repository.pojo.repo.RepoUpdateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import com.tencent.bkrepo.repository.util.RepoCleanRuleUtils
import com.tencent.bkrepo.repository.util.RuleUtils
import java.time.LocalDateTime
import java.util.Locale
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Conditional
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 仓库服务实现类
 */
@Service
@Conditional(SyncCondition::class, DefaultCondition::class)
@Suppress("TooManyFunctions")
class RepositoryServiceImpl(
    val repositoryDao: RepositoryDao,
    private val projectService: ProjectService,
    private val recycleBinService: RecycleBinService,
    private val storageCredentialService: StorageCredentialService,
    private val proxyChannelService: ProxyChannelService,
    private val messageSupplier: MessageSupplier,
    private val servicePermissionClient: ServicePermissionClient,
    private val resourceClearService: ObjectProvider<ResourceClearService>
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

    override fun updateStorageCredentialsKey(projectId: String, repoName: String, storageCredentialsKey: String?) {
        val repo = checkRepository(projectId, repoName)
        if (repo.credentialsKey != storageCredentialsKey) {
            repo.oldCredentialsKey = repo.credentialsKey
            repo.credentialsKey = storageCredentialsKey
            repositoryDao.save(repo)
        }
    }

    override fun unsetOldStorageCredentialsKey(projectId: String, repoName: String) {
        repositoryDao.unsetOldCredentialsKey(projectId, repoName)
    }

    override fun listRepo(
        projectId: String,
        name: String?,
        type: String?,
        display: Boolean?,
        category: List<String>?,
    ): List<RepositoryInfo> {
        val query = buildListQuery(projectId, name, type, display, category)
        return repositoryDao.find(query).map { convertToInfo(it)!! }
    }

    override fun listRepoPage(
        projectId: String,
        pageNumber: Int,
        pageSize: Int,
        option: RepoListOption
    ): Page<RepositoryInfo> {
        val query = buildListQuery(projectId, option.name, option.type, null, option.category)
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
        option: RepoListOption,
    ): List<RepositoryInfo> {
        var names = servicePermissionClient.listPermissionRepo(
            projectId = projectId,
            userId = userId,
            appId = SecurityUtils.getPlatformId(),
            actions = option.actions,
            includePathAuthRepo = option.includePathAuthRepo
        ).data.orEmpty()
        if (!option.name.isNullOrBlank()) {
            names = names.filter { it.startsWith(option.name.orEmpty(), true) }
        }
        val query = buildListPermissionRepoQuery(projectId, names, option)
        val originResults = repositoryDao.find(query).map { convertToInfo(it)!! }
        val originNames = originResults.map { it.name }.toSet()
        var includeResults = emptyList<RepositoryInfo>()
        if (names.isNotEmpty() && option.include != null) {
            val inValues = names.intersect(setOf(option.include!!)).minus(originNames)
            val includeCriteria = where(TRepository::projectId).isEqualTo(projectId)
                .and(TRepository::name).inValues(inValues)
            includeResults = repositoryDao.find(Query(includeCriteria)).map { convertToInfo(it)!! }
        }
        return originResults + includeResults
    }

    override fun listPermissionRepoPage(
        userId: String,
        projectId: String,
        pageNumber: Int,
        pageSize: Int,
        option: RepoListOption,
    ): Page<RepositoryInfo> {
        val allRepos = listPermissionRepo(userId, projectId, option)
        return Pages.buildPage(allRepos, pageNumber, pageSize)
    }

    override fun listRepoByTypes(projectId: String, types: List<String>): List<RepositoryInfo> {
        val query = buildListQuery(projectId)
            .addCriteria(TRepository::type.inValues(types.map { it.uppercase(Locale.getDefault()) }))
        return repositoryDao.find(query).map { convertToInfo(it)!! }
    }

    override fun rangeQuery(request: RepoRangeQueryRequest): Page<RepositoryInfo?> {
        val limit = request.limit
        val skip = request.offset
        val query = buildRangeQuery(request)
        val totalCount = repositoryDao.count(query)
        val records = repositoryDao.find(query.limit(limit).skip(skip))
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
            Preconditions.checkArgument(checkCategory(category, configuration), this::configuration.name)
            Preconditions.checkArgument(checkInterceptorConfig(configuration), this::configuration.name)
            Preconditions.checkArgument(checkConfigurationType(category, configuration), this::configuration.name)
            // 确保项目一定存在
            val project = projectService.getProjectInfo(projectId)
                ?: throw ErrorCodeException(ArtifactMessageCode.PROJECT_NOT_FOUND, projectId)
            // 确保同名仓库不存在
            if (checkExist(projectId, name)) {
                throw ErrorCodeException(ArtifactMessageCode.REPOSITORY_EXISTED, name)
            }
            //若同时是匿名公开与系统内公开仓库，则调整为系统内公开仓库
            val settingSystem = (configuration?.settings?.get(SYSTEM_REPO) ?: false) as Boolean
            if (public && settingSystem) {
                this.public = false
            }
            // 解析存储凭证
            val credentialsKey = determineStorageKey(this, project.credentialsKey)
            // 确保存储凭证Key一定存在
            credentialsKey?.takeIf { it.isNotBlank() }?.let {
                storageCredentialService.findByKey(it) ?: throw ErrorCodeException(
                    CommonMessageCode.RESOURCE_NOT_FOUND,
                    it,
                )
            }
            // 校验或初始化仓库配置
            val repoConfiguration = configuration?.also {
                if (it is VirtualConfiguration) {
                    checkVirtualConfiguration(it, projectId, type.name)
                }
            } ?: buildRepoConfiguration(this)
            // 创建仓库
            val repository = buildTRepository(this, repoConfiguration, credentialsKey)
            return try {
                if (repoConfiguration is CompositeConfiguration) {
                    val old = queryCompositeConfiguration(projectId, name, type)
                    updateCompositeConfiguration(repoConfiguration, old, repository, operator)
                }
                repository.configuration = cryptoConfigurationPwd(repoConfiguration, false).toJsonString()
                checkAndRemoveDeletedRepo(projectId, name, credentialsKey)
                repositoryDao.insert(repository)
                val event = buildCreatedEvent(repoCreateRequest)
                publishEvent(event)
                messageSupplier.delegateToSupplier(
                    data = event,
                    topic = event.topic,
                    key = event.getFullResourceKey(),
                )
                logger.info("Create repository [$repoCreateRequest] success.")
                convertToDetail(repository)!!
            } catch (exception: DuplicateKeyException) {
                logger.warn("Insert repository[$projectId/$name] error: [${exception.message}]")
                getRepoDetail(projectId, name, type.name)!!
            }
        }
    }

    fun buildTRepository(
        request: RepoCreateRequest,
        repoConfiguration: RepositoryConfiguration,
        credentialsKey: String?,
    ): TRepository {
        return RepositoryServiceHelper.buildTRepository(request, repoConfiguration, credentialsKey)
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun updateRepo(repoUpdateRequest: RepoUpdateRequest) {
        repoUpdateRequest.apply {
            Preconditions.checkArgument((description?.length ?: 0) < REPO_DESC_MAX_LENGTH, this::description.name)
            Preconditions.checkArgument(checkInterceptorConfig(configuration), this::configuration.name)
            val repository = checkRepository(projectId, name)
            quota?.let {
                Preconditions.checkArgument(it >= (repository.used ?: 0), this::quota.name)
                repository.quota = it
                repository.used = repository.used ?: 0
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
            repository.display = display
            coverStrategy?.let { repository.coverStrategy = it }
            repositoryDao.save(repository)
            val event = buildUpdatedEvent(repoUpdateRequest, repository.type)
            publishEvent(event)
            messageSupplier.delegateToSupplier(
                data = event,
                topic = event.topic,
                key = event.getFullResourceKey(),
            )
        }
        logger.info("Update repository[$repoUpdateRequest] success.")
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun deleteRepo(repoDeleteRequest: RepoDeleteRequest) {
        repoDeleteRequest.apply {
            val repository = checkRepository(projectId, name)
            resourceClearService.ifAvailable?.clearRepo(repository, forced, operator)
            // 为避免仓库被删除后节点无法被自动清理的问题，对仓库实行假删除
            repositoryDao.updateFirst(
                Query(Criteria.where(ID).isEqualTo(repository.id!!)),
                Update().set(TRepository::deleted.name, LocalDateTime.now())
            )
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
            // 清理回收站数据
            recycleBinService.clean(projectId, name)
            //删除仓库权限中心数据
            try {
                servicePermissionClient.deletePermissionData(repoDeleteRequest.projectId, repoDeleteRequest.name)
            } catch (exception: Exception) {
                logger.error("Permission data cleaning failed: ${exception}")
            }
            publishEvent(buildDeletedEvent(repoDeleteRequest, repository.type))
        }
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
            Query(
                where(TRepository::category).`in`(listOf(RepositoryCategory.COMPOSITE, RepositoryCategory.LOCAL))
                    .and(TRepository::type).isEqualTo(RepositoryType.GENERIC)
            )
        ).mapNotNull { convertToInfo(it) }
        val migratedRepos = mutableListOf<String>()
        // 遍历仓库
        for (repoInfo in allRepos) {
            logger.info("MigrateCleanStrategy: [${repoInfo.projectId}:${repoInfo.name}]")
            val configuration = when (repoInfo.category) {
                RepositoryCategory.LOCAL -> repoInfo.configuration as LocalConfiguration
                RepositoryCategory.COMPOSITE -> repoInfo.configuration as CompositeConfiguration
                else -> {
                    logger.warn("Unknown repo category: $repoInfo")
                    continue
                }
            }
            val cleanStrategy = configuration.cleanStrategy ?: continue
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
                rule = Rule.NestedRule(findRules(rule.rules) as MutableList<Rule>, Rule.NestedRule.RelationType.OR)
                val rootPathExist = mergeRules(rule, reserveDays)
                if (!rootPathExist) {
                    rule.rules.add(
                        0, Rule.NestedRule(
                            mutableListOf(
                                Rule.QueryRule("path", "/", OperationType.REGEX),
                                Rule.QueryRule("reserveDays", reserveDays, OperationType.LTE),
                                Rule.NestedRule(mutableListOf(), Rule.NestedRule.RelationType.OR)
                            ),
                            Rule.NestedRule.RelationType.AND
                        )
                    )
                }
                val tRepo = repositoryDao.findByNameAndType(repoInfo.projectId, repoInfo.name)
                configuration.cleanStrategy = RepoCleanRuleUtils.replaceRule(cleanStrategy, rule)
                updateRepositoryConfiguration(
                    repoInfo = repoInfo,
                    tRepo = tRepo,
                    configuration = configuration,
                    migratedRepos = migratedRepos
                )
            }
        }
        return migratedRepos
    }


    private fun findRules(rule: MutableList<Rule>): MutableList<Rule.NestedRule> {
        val targetPathNestedRules = mutableListOf<Rule.NestedRule>()
        for (pathNestedRule in rule) {
            if (pathNestedRule !is Rule.NestedRule) continue
            val queryRules = pathNestedRule.rules
            val pathQueryRule = queryRules.filterIsInstance<Rule.QueryRule>().find { it.field == "path" }
                ?: continue
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
        return targetPathNestedRules
    }

    private fun mergeRules(rule: Rule.NestedRule, reserveDays: Long): Boolean {
        var rootPathExist = false
        // 遍历条件规则
        for (pathNestedRule in rule.rules) {
            if (pathNestedRule !is Rule.NestedRule) continue
            val rules = pathNestedRule.rules
            val matchRules = mutableListOf<Rule.QueryRule>()
            val oldMatchRules = rules.filterIsInstance<Rule.QueryRule>()
            if (oldMatchRules.isNullOrEmpty()) {
                // 无效规则
                rule.rules.remove(pathNestedRule)
            } else {
                val (exist, tempRules) = findMatchRules(oldMatchRules, rules)
                rootPathExist = exist
                matchRules.addAll(tempRules)
            }
            rules.removeIf {
                it is Rule.QueryRule &&
                        (it.field == "name" || it.field.startsWith("metadata.") || it.field == "id")
            }
            rules.add(Rule.NestedRule(matchRules as MutableList<Rule>, Rule.NestedRule.RelationType.OR))
            val queryFields = rules.filterIsInstance<Rule.QueryRule>().map { it.field }
            if (!queryFields.contains("reserveDays")) {
                rules.add(Rule.QueryRule("reserveDays", reserveDays))
            }
        }
        return rootPathExist
    }

    private fun findMatchRules(
        oldMatchRules: List<Rule.QueryRule>,
        rules: MutableList<Rule>
    ): Pair<Boolean, MutableList<Rule.QueryRule>> {
        val matchRules = mutableListOf<Rule.QueryRule>()
        var rootPathExist = false
        oldMatchRules.forEach { eachRule ->
            if (eachRule.field == "path" && eachRule.value == "/") {
                rootPathExist = true
            }
            if (eachRule.field == "name" ||
                eachRule.field.startsWith("metadata.") ||
                eachRule.field == "id"
            ) {
                matchRules.add(eachRule)
            }
        }
        // 空的匹配规则，在旧版中空表示全部数据
        if (matchRules.isEmpty() && rules.filterIsInstance<Rule.NestedRule>().isEmpty()) {
            matchRules.add(Rule.QueryRule("id", "null", OperationType.NE))
        }
        return Pair(rootPathExist, matchRules)
    }

    private fun updateRepositoryConfiguration(
        repoInfo: RepositoryInfo,
        tRepo: TRepository?,
        configuration: RepositoryConfiguration,
        migratedRepos: MutableList<String>
    ) {
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
            } catch (e: Exception) {
                logger.warn("Migrate clean strategy failed: $repoInfo", e)
            }
        } else {
            logger.warn("Failed to find repo[$repoInfo], maybe it has been deleted.")
        }
    }


    override fun statRepo(projectId: String, repoName: String): NodeSizeInfo {
        val projectMetrics = projectService.getProjectMetricsInfo(projectId)
        val repoMetrics = projectMetrics?.repoMetrics?.firstOrNull { it.repoName == repoName }
        return NodeSizeInfo(
            subNodeCount = repoMetrics?.num ?: 0,
            subNodeWithoutFolderCount = repoMetrics?.num ?: 0,
            size = repoMetrics?.size ?: 0,
        )
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
        } catch (e: Exception) {
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

    /**
     * 获取仓库下的代理地址信息
     */
    private fun queryCompositeConfiguration(
        projectId: String,
        repoName: String,
        repoType: RepositoryType,
    ): CompositeConfiguration? {
        val proxyList = proxyChannelService.listProxyChannel(projectId, repoName, repoType)
        if (proxyList.isEmpty()) return null
        val proxy = ProxyConfiguration(proxyList.map { convertProxyToProxyChannelSetting(it) })
        return CompositeConfiguration(proxy)
    }

    /**
     * 检查仓库是否存在，不存在则抛异常
     */
    fun checkRepository(projectId: String, repoName: String, repoType: String? = null): TRepository {
        return repositoryDao.findByNameAndType(projectId, repoName, repoType)
            ?: throw ErrorCodeException(REPOSITORY_NOT_FOUND, repoName)
    }

    /**
     * 更新仓库配置
     */
    private fun updateRepoConfiguration(
        new: RepositoryConfiguration,
        old: RepositoryConfiguration,
        repository: TRepository,
        operator: String,
    ) {
        checkConfigType(new, old)
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
        if (rule !is Rule.NestedRule || rule.rules.isEmpty()) return
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
     * 更新Composite类型仓库配置
     */
    private fun updateCompositeConfiguration(
        new: CompositeConfiguration,
        old: CompositeConfiguration? = null,
        repository: TRepository,
        operator: String,
    ) {
        val (toCreateList, toDeleteList, toUpdateList) = buildChangeList(new, old)
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
    fun deleteProxyRepo(repository: TRepository, proxy: ProxyChannelSetting) {
        val proxyRepository = buildProxyChannelDeleteRequest(repository, proxy)
        proxyChannelService.deleteProxy(proxyRepository)
        logger.info(
            "Success to delete private proxy channel [${proxy.name}]" +
                    " in repo[${repository.projectId}|${repository.name}]",
        )
    }

    private fun createProxyRepo(repository: TRepository, proxy: ProxyChannelSetting, operator: String) {
        // 创建代理仓库
        val proxyRepository = buildProxyChannelCreateRequest(repository, proxy)
        proxyChannelService.createProxy(operator, proxyRepository)
        logger.info("Success to create private proxy repository[$proxyRepository]")
    }

    private fun updateProxyRepo(repository: TRepository, proxy: ProxyChannelSetting, operator: String) {
        // 更新代理仓库
        val proxyRepository = buildProxyChannelUpdateRequest(repository, proxy)
        proxyChannelService.updateProxy(operator, proxyRepository)
        logger.info("Success to update private proxy repository[$proxyRepository]")
    }

    override fun listRepoPageByType(type: String, pageNumber: Int, pageSize: Int): Page<RepositoryDetail> {
        val query = buildTypeQuery(type)
        val count = repositoryDao.count(query)
        val pageQuery = query.with(PageRequest.of(pageNumber, pageSize))
        val data = repositoryDao.find(pageQuery).map {
            val storageCredentials = it.credentialsKey?.let { key -> storageCredentialService.findByKey(key) }
            convertToDetail(it, storageCredentials)!!
        }

        return Page(pageNumber, pageSize, count, data)
    }

    /**
     * 查找是否存在已被逻辑删除的仓库，如果存在且存储凭证相同，则删除旧仓库再插入新数据；如果存在且存储凭证不同，则禁止创建仓库
     */
    private fun checkAndRemoveDeletedRepo(projectId: String, repoName: String, credentialsKey: String?) {
        val query = buildSingleQuery(projectId, repoName)
        repositoryDao.findOne(query)?.let {
            if (credentialsKey == it.credentialsKey) {
                repositoryDao.remove(query)
                logger.info("Retrieved deleted record of Repository[$projectId/$repoName] before creating")
            } else {
                throw ErrorCodeException(ArtifactMessageCode.REPOSITORY_EXISTED, repoName)
            }
        }
    }

    private fun checkConfigurationType(category: RepositoryCategory, configuration: RepositoryConfiguration?): Boolean {
        return configuration?.let {
            when (category) {
                RepositoryCategory.COMPOSITE -> it is CompositeConfiguration
                RepositoryCategory.LOCAL -> it is LocalConfiguration && it !is CompositeConfiguration
                RepositoryCategory.REMOTE -> it is RemoteConfiguration
                RepositoryCategory.VIRTUAL -> it is VirtualConfiguration
                RepositoryCategory.PROXY -> {
                    it is com.tencent.bkrepo.common.artifact.pojo.configuration.proxy.ProxyConfiguration
                }
            }
        } ?: true
    }

    private fun checkVirtualConfiguration(
        configuration: VirtualConfiguration,
        projectId: String,
        type: String
    ) {
        with(configuration) {
            repositoryList = repositoryList.distinctBy { it.name }
            // 校验虚拟仓库配置中的仓库列表
            repositoryList.forEach {
                Preconditions.checkArgument(projectId == it.projectId, "projectId")
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
                    RepositoryIdentify(projectId, this, RepositoryCategory.LOCAL) in repositoryList,
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

    }
}
