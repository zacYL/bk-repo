package com.tencent.bkrepo.repository.service.repo.impl

import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_SIZE
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.pojo.configuration.clean.CleanStatus
import com.tencent.bkrepo.common.artifact.pojo.configuration.clean.RepositoryCleanStrategy
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import com.tencent.bkrepo.repository.dao.RepositoryDao
import com.tencent.bkrepo.repository.job.clean.CleanRepoTaskScheduler
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.pojo.node.NodeDelete
import com.tencent.bkrepo.repository.pojo.packages.*
import com.tencent.bkrepo.repository.service.node.NodeDeleteOperation
import com.tencent.bkrepo.repository.service.node.NodeSearchService
import com.tencent.bkrepo.repository.service.node.NodeStatsOperation
import com.tencent.bkrepo.repository.service.packages.PackageService
import com.tencent.bkrepo.repository.service.repo.RepositoryCleanService
import com.tencent.bkrepo.repository.service.repo.RepositoryService
import com.tencent.bkrepo.repository.util.ArtifactClientServiceFactory
import com.tencent.bkrepo.repository.util.RuleUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException
import java.time.Duration
import java.time.LocalDateTime

@Service
class RepositoryCleanServiceImpl(
    private val repositoryDao: RepositoryDao,
    private val repositoryService: RepositoryService,
    private val taskScheduler: CleanRepoTaskScheduler,
    private val packageService: PackageService,
    private val nodeSearchService: NodeSearchService,
    private val nodeDeleteOperation: NodeDeleteOperation,
    private val nodeStatsOperation: NodeStatsOperation
) : RepositoryCleanService {

    override fun cleanRepo(repoId: String) {
        val tRepository = repositoryDao.findById(repoId)
        tRepository?.let { repo ->
            val cleanStrategy = repositoryService.getRepoCleanStrategy(repo.projectId, repo.name)
            logger.info("projectId:[${repo.projectId}] repoName:[${repo.name}] clean strategy:[$cleanStrategy] execute")
            cleanStrategy?.let {
                // 自动清理关闭，状态为 WAITING，删除job
                if (!it.autoClean && it.status == CleanStatus.WAITING) {
                    taskScheduler.deleteJob(repoId)
                    return
                }
                // 更新【清理策略】状态为 【RUNNING】
                try {
                    repositoryService.updateCleanStatusRunning(repo.projectId, repo.name)
                } catch (ex: IllegalArgumentException) {
                    logger.error(
                        "projectId:[${repo.projectId}] repoName:[${repo.name}] " +
                                "update clean strategy illegal argument exception:[$ex],clean repo fail"
                    )
                    return
                }
                if (repo.type == RepositoryType.GENERIC) {
                    executeNodeClean(repo.projectId, repo.name, it)
                } else {
                    executeClean(repo.projectId, repo.name, it)
                }
                // 更新【清理策略】状态为 【WAITING】
                try {
                    repositoryService.updateCleanStatusWaiting(repo.projectId, repo.name)
                } catch (ex: IllegalArgumentException) {
                    logger.warn(
                        "projectId:[${repo.projectId}] repoName:[${repo.name}] " +
                                "update clean strategy illegal argument exception:[$ex]"
                    )
                }
            } ?: logger.warn("projectId:[${repo.projectId}] repoName:[${repo.name}] clean strategy is null")
        } ?: logger.error("clean repo illegal argument exception tRepository is null, tRepository:[$tRepository]")
    }

    /**
     * 执行规则过滤，并删除
     */
    private fun executeClean(
        projectId: String,
        repoName: String,
        cleanStrategy: RepositoryCleanStrategy
    ) {
        var skip: Long = 0
        var packageList = packageService.listPackagePage(projectId, repoName, DEFAULT_PAGE_SIZE, skip)
        while (packageList.isNotEmpty()) {
            // 执行规则过滤，并删除
            var deleteVersions: MutableList<PackageVersion>
            var ruleQueryList = mutableListOf<PackageVersion>()
            packageList.forEach {
                requireNotNull(it.id)
                //包的版本数 < 保留版本数，直接跳过
                with(cleanStrategy) {
                    if (it.versions < reserveVersions) return@forEach
                    val listVersion = packageService.listAllVersion(
                        it.projectId,
                        it.repoName,
                        it.key,
                        VersionListOption()
                    )
                    // 元数据规则不为null，根据规则查询
                    rule?.let { rule ->
                        ruleQueryList = metadataRuleQuery(rule, it.id!!).toMutableList()
                    }
                    logger.info(
                        "projectId:[${it.projectId}] repoName:[${it.repoName}] " +
                                "packageName:[${it.name}] rule query result:[$ruleQueryList]"
                    )
                    deleteVersions = listVersion.toMutableList()
                    deleteVersions.removeAll(ruleQueryList)
                    deleteVersions = reserveVersionsAndDaysFilter(
                        deleteVersions,
                        reserveVersions,
                        reserveDays
                    ).toMutableList()

                    logger.info(
                        "projectId:[${it.projectId}] repoName:[${it.repoName}] clean [packageName:${it.name}] " +
                                "delete version collection: $deleteVersions"
                    )
                    // 删除版本集合
                    if (deleteVersions.isNotEmpty()) {
                        if (!deleteVersions.containsAll(listVersion)) skip++
                        deleteVersion(deleteVersions, it.key, it.type, it.projectId, it.repoName)
                    } else {
                        skip++
                    }
                }
            }
            packageList = packageService.listPackagePage(projectId, repoName, DEFAULT_PAGE_SIZE, skip)
        }
    }

    /**
     * 元数据规则查询
     */
    private fun metadataRuleQuery(
        rule: Rule,
        packageId: String
    ): List<PackageVersion> {
        val versionList = mutableListOf<PackageVersion>()
        if (rule is Rule.NestedRule && rule.rules.isEmpty()) {
            return versionList
        }
        var pageNumber = 1
        val packageIdRule = Rule.QueryRule("packageId", packageId)
        val queryRule = Rule.NestedRule(mutableListOf(packageIdRule, rule))
        val queryModel = QueryModel(
            page = PageLimit(pageNumber, DEFAULT_PAGE_SIZE),
            sort = null,
            select = null,
            rule = queryRule
        )
        var versionPage = packageService.searchVersion(queryModel)
        while (versionPage.records.isNotEmpty()) {
            versionList.addAll(versionPage.records)
            pageNumber += 1
            queryModel.page = PageLimit(pageNumber, DEFAULT_PAGE_SIZE)
            versionPage = packageService.searchVersion(queryModel)
        }
        return versionList
    }

    /**
     * 执行 Generic 仓库清理
     */
    private fun executeNodeClean(
        projectId: String,
        repoName: String,
        cleanStrategy: RepositoryCleanStrategy
    ) {
        val projectIdRule = Rule.QueryRule("projectId", projectId)
        val repoNameRule = Rule.QueryRule("repoName", repoName)
        val allNodeQueryRule = Rule.NestedRule(mutableListOf(projectIdRule, repoNameRule))
        // 仓库下所有节点集合
        val allNodeList = nodeRuleQuery(allNodeQueryRule)
        // 保留规则查询节点集合
        var reserveNodeList = mutableListOf<NodeDelete>()
        var deleteNodeList: MutableList<NodeDelete>
        with(cleanStrategy) {
            rule?.let {
                reserveNodeList = nodeRuleQuery(it)
            }
            if (logger.isDebugEnabled) {
                logger.debug("project:[$projectId] repoName[$repoName] reverseNodeList:[$reserveNodeList]")
            }
            // 取【所有节点集合】 与 【保留规则集合】 的差集
            allNodeList.removeAll(reserveNodeList)
            // 保留天数过滤
            deleteNodeList = nodeReserveDaysFilter(allNodeList, reserveDays)
            if (logger.isDebugEnabled) {
                logger.debug("project:[$projectId] repoName[$repoName] deleteNodeList:[$reserveNodeList]")
            }
        }
        if (logger.isDebugEnabled) {
            logger.debug("projectId:[$projectId] repoName:[$repoName] delete list [$deleteNodeList]");
        }
        deleteNodeList.forEach {
            // 判断文件夹下是否有文件
            if (it.folder) {
                val countFileNode =
                    nodeStatsOperation.countFileNode(ArtifactInfo(it.projectId, it.repoName, it.fullPath))//TODO 数量有差异
                if (countFileNode > 0) return@forEach
            }
            nodeDeleteOperation.deleteByPath(it.projectId, it.repoName, it.fullPath, SYSTEM_USER)
        }
    }

    /**
     * 根据规则查询节点
     * @return MutableList<TNode> 节点集合
     */
    private fun nodeRuleQuery(rule: Rule): MutableList<NodeDelete> {
        val result = mutableListOf<NodeDelete>()
        if (rule is Rule.NestedRule && rule.rules.isEmpty()) return result
        // 处理 fullPath 条件
        RuleUtils.ruleFullPathToRegex(rule)
        var pageNumber = 1
        val queryModel = QueryModel(
            page = PageLimit(pageNumber, DEFAULT_PAGE_SIZE),
            sort = null,
            select = null,
            rule = rule
        )
        var nodePage = nodeSearchService.search(queryModel)
        while (nodePage.records.isNotEmpty()) {
            nodePage.records.map {
                val projectId = it[TNode::projectId.name] as String
                val repoName = it[TNode::repoName.name] as String
                val fullPath = it[TNode::fullPath.name] as String
                val folder = it[TNode::folder.name] as Boolean
                val lastModifiedDate = it[TNode::lastModifiedDate.name] as LocalDateTime
                val node = NodeDelete(projectId, repoName, folder, fullPath, lastModifiedDate)
                result.add(node)
            }
            pageNumber += 1
            queryModel.page = PageLimit(pageNumber, DEFAULT_PAGE_SIZE)
            nodePage = nodeSearchService.search(queryModel)
        }
        return result
    }

    /**
     * 根据保留天数过滤节点
     * @return MutableList<TNode> ,返回大于【保留天数】的节点集合
     */
    private fun nodeReserveDaysFilter(nodeList: List<NodeDelete>, reserveDays: Long): MutableList<NodeDelete> {
        val deleteNodeList: MutableList<NodeDelete> = mutableListOf()
        // 根据【lastModifyDate】降序排序
        val nodeSortList = nodeList.sortedByDescending { it.lastModifiedDate }
        val nowDate = LocalDateTime.now()
        val size = nodeSortList.size
        for (i in 0 until (size)) {
            val days = Duration.between(nodeSortList[i].lastModifiedDate, nowDate).toDays()
            if (days > reserveDays) {
                deleteNodeList.addAll(nodeSortList.subList(i, size))
                break
            }
        }
        return deleteNodeList
    }

    /**
     * 保留版本数，保留天数过滤
     */
    private fun reserveVersionsAndDaysFilter(
        versions: List<PackageVersion>,
        reserveVersions: Long,
        reserveDays: Long
    ): List<PackageVersion> {
        val filterVersions: MutableList<PackageVersion> = mutableListOf()
        //根据 【版本序列号】 降序排序
        val sortedByDesc = versions.sortedByDescending { it.ordinal }
        //截取超过【保留版本数】的版本，接下来进行保留天数过滤
        val reserveDaysFilter =
            sortedByDesc.subList(reserveVersions.toInt(), versions.size).sortedByDescending { it.lastModifiedDate }
        logger.info("reverse version number filter result:[$reserveDaysFilter]")
        val nowDate = LocalDateTime.now()
        val size = reserveDaysFilter.size
        for (i in 0 until (size)) {
            val days = Duration.between(reserveDaysFilter[i].lastModifiedDate, nowDate).toDays()
            if (days > reserveDays) {
                filterVersions.addAll(reserveDaysFilter.subList(i, size))
                break
            }
        }
        return filterVersions
    }

    /**
     * 删除对应依赖源的包版本
     */
    private fun deleteVersion(
        versions: List<PackageVersion>,
        packageKey: String,
        type: PackageType,
        projectId: String,
        repoName: String
    ) {
        val artifactClientService = ArtifactClientServiceFactory.getArtifactClientService(type)
        versions.forEach {
            try {
                artifactClientService.deleteVersion(projectId, repoName, packageKey, it.name)
            } catch (ex: Exception) {
                logger.error(
                    "delete package version fail, exception is [$ex], projectId:[$projectId] " +
                            "repoName:[$repoName] packageKey:[$packageKey] version:[$versions] "
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryCleanServiceImpl::class.java)
    }
}
