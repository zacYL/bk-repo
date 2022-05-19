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
import com.tencent.bkrepo.repository.model.TPackageVersion
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
import java.util.*

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
            cleanStrategy?.let {
                logger.info(
                    "projectId:[${repo.projectId}] repoName:[${repo.name}] " +
                            "clean strategy autoClean:[${it.autoClean}] status:[${it.status}]"
                )
                // 自动清理关闭，状态为 WAITING，删除job
                if (!it.autoClean && it.status == CleanStatus.WAITING) {
                    taskScheduler.deleteJob(repoId)
                    return
                }
                try {
                    repositoryService.updateCleanStatusRunning(repo.projectId, repo.name)
                    if (repo.type == RepositoryType.GENERIC) {
                        executeNodeClean(repo.projectId, repo.name, it)
                    } else {
                        executeClean(repo.projectId, repo.name, it)
                    }
                    repositoryService.updateCleanStatusWaiting(repo.projectId, repo.name)
                } catch (ex: IllegalArgumentException) {
                    logger.error("repo clean fail exception:[$ex]")
                } catch (ex: Exception) {
                    repositoryService.updateCleanStatusWaiting(repo.projectId, repo.name)
                    logger.error("projectId:[${repo.projectId}] repoName:[${repo.name}] clean error [$ex]")
                }
            } ?: logger.warn("projectId:[${repo.projectId}] repoName:[${repo.name}] clean strategy is null")
        } ?: logger.error("argument exception tRepository is null, tRepository:[$tRepository]")
    }

    /**
     * 执行规则过滤，并删除
     * TODO 日志恢复debug
     */
    private fun executeClean(
        projectId: String,
        repoName: String,
        cleanStrategy: RepositoryCleanStrategy
    ) {
        // 记录没有被删除的 package 数量，并在分页查询时跳过
        var skip: Long = 0
        var packageList = packageService.listPackagePage(projectId, repoName, DEFAULT_PAGE_SIZE, skip)
        while (packageList.isNotEmpty()) {
            packageList.forEach {
                val deleteVersions: MutableList<PackageVersion>
                var ruleQueryList = mutableListOf<PackageVersion>()
                requireNotNull(it.id)
                //包的版本数 < 保留版本数，直接跳过
                if (it.versions < cleanStrategy.reserveVersions) return@forEach
                val listVersion = packageService.listAllVersion(
                    it.projectId,
                    it.repoName,
                    it.key,
                    VersionListOption()
                ).toMutableList()
                cleanStrategy.rule?.let { rule ->
                    ruleQueryList = metadataRuleQuery(rule, it.id!!).toMutableList()
                }
//                if (logger.isDebugEnabled){
//                    logger.debug(
//                        "projectId:[${it.projectId}] repoName:[${it.repoName}] " +
//                                "packageName:[${it.name}] rule query result:[$ruleQueryList]"
//                    )
//                }
                logger.info(
                    "projectId:[${it.projectId}] repoName:[${it.repoName}] " +
                            "packageName:[${it.name}] rule query result:[$ruleQueryList]"
                )
                listVersion.removeAll(ruleQueryList)
                deleteVersions = reserveVersionsAndDaysFilter(
                    listVersion,
                    cleanStrategy.reserveVersions,
                    cleanStrategy.reserveDays
                ).toMutableList()
//                if (logger.isDebugEnabled){
//                    logger.debug(
//                        "projectId:[${it.projectId}] repoName:[${it.repoName}] clean [packageName:${it.name}] " +
//                                "delete version collection: $deleteVersions"
//                    )
//                }
                logger.info(
                    "projectId:[${it.projectId}] repoName:[${it.repoName}] clean [packageName:${it.name}] " +
                            "delete version collection: $deleteVersions"
                )
                if (deleteVersions.isNotEmpty()) {
                    if (!deleteVersions.containsAll(listVersion)) skip++
                    deleteVersion(deleteVersions, it.key, it.type, it.projectId, it.repoName)
                } else {
                    skip++
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
        val packageIdRule = Rule.QueryRule(TPackageVersion::packageId.name, packageId)
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
     * TODO 日志恢复debug
     */
    private fun executeNodeClean(
        projectId: String,
        repoName: String,
        cleanStrategy: RepositoryCleanStrategy
    ) {
        val projectIdRule = Rule.QueryRule("projectId", projectId)
        val repoNameRule = Rule.QueryRule("repoName", repoName)
        val allNodeQueryRule = Rule.NestedRule(mutableListOf(projectIdRule, repoNameRule))
        val allNodeList = nodeRuleQuery(allNodeQueryRule)
        var reserveNodeList = mutableListOf<NodeDelete>()
        var deleteNodeList: MutableList<NodeDelete>
        with(cleanStrategy) {
            rule?.let {
                reserveNodeList = nodeRuleQuery(it)
            }
//            if (logger.isDebugEnabled) {
//                logger.debug("project:[$projectId] repoName[$repoName] reverseNodeList:[$reserveNodeList]")
//            }
            logger.info("project:[$projectId] repoName[$repoName] reverseNodeList:[$reserveNodeList]")
            // 取【所有节点集合】 与 【保留规则集合】 的差集
            allNodeList.removeAll(reserveNodeList)
            // 保留天数过滤
            deleteNodeList = nodeReserveDaysFilter(allNodeList, reserveDays)
        }
//        if (logger.isDebugEnabled) {
//            logger.debug("projectId:[$projectId] repoName:[$repoName] delete list [$deleteNodeList]")
//        }
        logger.info("projectId:[$projectId] repoName:[$repoName] delete list [$deleteNodeList]")
        deleteNodeList.forEach {
            // 判断文件夹下是否有文件
            if (it.folder) {
                val countFileNode =
                    nodeStatsOperation.countFileNode(ArtifactInfo(it.projectId, it.repoName, it.fullPath))
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
        val newRule = RuleUtils.ruleFullPathToRegex(rule)
        var pageNumber = 1
        val queryModel = QueryModel(
            page = PageLimit(pageNumber, DEFAULT_PAGE_SIZE),
            sort = null,
            select = null,
            rule = newRule
        )
        var nodePage = nodeSearchService.search(queryModel)
        while (nodePage.records.isNotEmpty()) {
            nodePage.records.map {
                val projectId = it[TNode::projectId.name] as String
                val repoName = it[TNode::repoName.name] as String
                val fullPath = it[TNode::fullPath.name] as String
                val folder = it[TNode::folder.name] as Boolean
                val recentlyUseDate = it[TNode::recentlyUseDate.name] as? LocalDateTime
                val node = NodeDelete(projectId, repoName, folder, fullPath, recentlyUseDate)
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
        val nodeSortList = nodeList.sortedByDescending { it.recentlyUseDate }
        val nowDate = LocalDateTime.now()
        val size = nodeSortList.size
        for (i in 0 until (size)) {
            val recentlyUseDate = nodeSortList[i].recentlyUseDate
            if (recentlyUseDate == null) {
                deleteNodeList.addAll(nodeSortList.subList(i, size))
                return deleteNodeList
            } else {
                val days = Duration.between(recentlyUseDate, nowDate).toDays()
                if (days > reserveDays) {
                    deleteNodeList.addAll(nodeSortList.subList(i, size))
                    return deleteNodeList
                }
            }

        }
        return deleteNodeList
    }

    /**
     * 保留版本数，保留天数过滤
     * TODO 日志恢复debug
     */
    private fun reserveVersionsAndDaysFilter(
        versions: List<PackageVersion>,
        reserveVersions: Long,
        reserveDays: Long
    ): List<PackageVersion> {
        val filterVersions: MutableList<PackageVersion> = mutableListOf()
        //根据 【版本序列号】 降序排序
        val sortedByDesc = versions.sortedByDescending { it.ordinal }
        //截取超过【保留版本数】的版本，进行保留天数过滤
        val reserveDaysFilter =
            sortedByDesc.subList(reserveVersions.toInt(), versions.size).sortedByDescending { it.recentlyUseDate }
//        if (logger.isDebugEnabled) {
//            logger.info("reverse version number filter result:[$reserveDaysFilter]")
//        }
        logger.info("reverse version number filter result:[$reserveDaysFilter]")
        val nowDate = LocalDateTime.now()
        val size = reserveDaysFilter.size
        for (i in 0 until (size)) {
            val recentlyUseDate = reserveDaysFilter[i].recentlyUseDate
            if (recentlyUseDate == null) {
                filterVersions.addAll(reserveDaysFilter.subList(i, size))
                return filterVersions
            } else {
                val days = Duration.between(reserveDaysFilter[i].recentlyUseDate, nowDate).toDays()
                if (days > reserveDays) {
                    filterVersions.addAll(reserveDaysFilter.subList(i, size))
                    return filterVersions
                }
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
                            "repoName:[$repoName] packageKey:[$packageKey] version:[$it] "
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryCleanServiceImpl::class.java)
    }
}
