package com.tencent.bkrepo.repository.service.repo.impl

import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_SIZE
import com.tencent.bkrepo.common.artifact.pojo.configuration.clean.CleanStatus
import com.tencent.bkrepo.common.artifact.pojo.configuration.clean.RepositoryCleanStrategy
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.repository.dao.RepositoryDao
import com.tencent.bkrepo.repository.job.clean.CleanRepoTaskScheduler
import com.tencent.bkrepo.repository.pojo.packages.*
import com.tencent.bkrepo.repository.service.packages.PackageService
import com.tencent.bkrepo.repository.service.repo.RepositoryCleanService
import com.tencent.bkrepo.repository.service.repo.RepositoryService
import com.tencent.bkrepo.repository.util.ArtifactClientServiceFactory
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
    private val packageService: PackageService
) : RepositoryCleanService {

    override fun cleanRepo(repoId: String) {
        val tRepository = repositoryDao.findById(repoId)
        tRepository?.let { repo ->
            val cleanStrategy = repositoryService.getRepoCleanStrategy(repo.projectId, repo.name)
            logger.info("projectId:[${repo.projectId}] repoName:[${repo.name}] clean strategy: [$cleanStrategy] excute")
            cleanStrategy?.let {
                //自动清理关闭，状态为 WAITING，删除job
                if (!it.autoClean && it.status == CleanStatus.WAITING) {
                    taskScheduler.deleteJob(repoId)
                    return
                }
                // 更新【清理策略】状态为 【RUNNING】
                try {
                    repositoryService.updateCleanStatusRunning(repo.projectId, repo.name)
                } catch (ex: IllegalArgumentException) {
                    logger.error("projectId:[${repo.projectId}] repoName:[${repo.name}] " +
                            "update clean strategy illegal argument exception:[$ex],clean repo fail")
                    return
                }
                var pageNumber = 1
                var packageOption = PackageListOption(
                    pageNumber,
                    DEFAULT_PAGE_SIZE
                )
                var packagePage = packageService.listPackagePage(repo.projectId, repo.name, packageOption)
                while (packagePage.records.isNotEmpty()) {
                    // 执行规则过滤，并删除
                    executeClean(packagePage.records, it)
                    pageNumber += 1
                    packageOption = PackageListOption(
                        pageNumber,
                        DEFAULT_PAGE_SIZE
                    )
                    packagePage = packageService.listPackagePage(repo.projectId, repo.name, option = packageOption)
                }
                // 更新【清理策略】状态为 【WAITING】
                try {
                    repositoryService.updateCleanStatusWaiting(repo.projectId, repo.name)
                } catch (ex: IllegalArgumentException) {
                    logger.warn("projectId:[${repo.projectId}] repoName:[${repo.name}] " +
                            "update clean strategy illegal argument exception:[$ex]")
                }
            } ?: logger.warn("projectId:[${repo.projectId}] repoName:[${repo.name}] clean strategy is null")
        } ?: logger.error("clean repo illegal argument exception tRepository is null, tRepository:[$tRepository]")
    }

    /**
     * 执行规则过滤，并删除
     */
    private fun executeClean(packageList: List<PackageSummary>, cleanStrategy: RepositoryCleanStrategy) {
        var deleteVersions: MutableList<PackageVersion>
        var ruleQueryList = mutableListOf<PackageVersion>()
        packageList.forEach {
            try {
                requireNotNull(it.id)
            } catch (ex: IllegalArgumentException) {
                logger.error(
                    "clean repo execute error projectId:[${it.projectId}] " +
                            "repoName:[${it.repoName}] [packageName:${it.name} package id is null"
                )
            }
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
                deleteVersions = listVersion.toMutableList()
                deleteVersions.removeAll(ruleQueryList)
                deleteVersions = reserveVersionsAndDaysFilter(
                    deleteVersions,
                    reserveVersions,
                    reserveDays
                ).toMutableList()
            }
            logger.info(
                "projectId:[${it.projectId}] repoName:[${it.repoName}] clean [packageName:${it.name}] " +
                        "delete version collection: $deleteVersions"
            )
            // 删除版本集合
            if (deleteVersions.isNotEmpty()) {
                logger.info("delete version begin....")
                deleteVersion(deleteVersions, it.key, it.type, it.projectId, it.repoName)
            }
        }
    }

    /**
     * 元数据规则查询
     */
    private fun metadataRuleQuery(rule: Rule, packageId: String): List<PackageVersion> {
        val versionList = mutableListOf<PackageVersion>()
        // rules is null
        if (rule is Rule.NestedRule && rule.rules.isEmpty()){
            return versionList
        }
        var pageNumber = 1
        val packageIdRule = Rule.QueryRule("packageId", packageId)
        val queryRule = Rule.NestedRule(mutableListOf(packageIdRule, rule), Rule.NestedRule.RelationType.AND)
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
                logger.info(
                    "delete package version begin, projectId:[$projectId] repoName:[$repoName] packageKey:[$packageKey]"
                )
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
