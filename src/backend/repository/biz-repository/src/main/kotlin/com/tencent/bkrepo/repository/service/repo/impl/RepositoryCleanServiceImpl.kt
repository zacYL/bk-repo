package com.tencent.bkrepo.repository.service.repo.impl

import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_SIZE
import com.tencent.bkrepo.common.artifact.pojo.configuration.clean.MetadataRule
import com.tencent.bkrepo.common.artifact.pojo.configuration.clean.OperationType
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.repository.dao.RepositoryDao
import com.tencent.bkrepo.repository.pojo.packages.PackageListOption
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.VersionListOption
import com.tencent.bkrepo.repository.service.packages.PackageService
import com.tencent.bkrepo.repository.service.repo.RepositoryCleanService
import com.tencent.bkrepo.repository.service.repo.RepositoryService
import com.tencent.bkrepo.repository.util.ArtifactRegistryServiceFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service
class RepositoryCleanServiceImpl(
    private val repositoryDao: RepositoryDao,
    private val repositoryService: RepositoryService,
    private val packageService: PackageService
) : RepositoryCleanService {

//    @Autowired
//    lateinit var dockerClient: DockerClient
//
//    @Autowired
//    lateinit var mavenClientXujian: MavenClientXujian
//
//    @Autowired
//    lateinit var pypiResource: PypiResource
//
//    @Autowired
//    lateinit var npmClient: NpmResource
//
//    @Autowired
//    lateinit var helmClient: HelmClient
//
//    @Autowired
//    lateinit var composerResource: ComposerResource
//
//    @Autowired
//    lateinit var rpmResource: RpmResource

    override fun cleanRepo(repoId: String) {
        logger.info("starting to clean repository, repoId:[$repoId]")
        val tRepository = repositoryDao.findById(repoId)
        require(tRepository != null)
        with(tRepository) {
            val cleanStrategy = repositoryService.getRepoCleanStrategy(projectId, name)
            logger.info("[$projectId $name] clean strategy: [$cleanStrategy]")
            if (cleanStrategy != null) {
                with(cleanStrategy) {
                    //1.获取仓库下所有包
                    var pageNumber = 1
                    var packageOption = PackageListOption(
                        pageNumber,
                        DEFAULT_PAGE_SIZE
                    )
                    var packagePage = packageService.listPackagePage(projectId, name, packageOption)
                    logger.info("clean repository:[${tRepository.name}] have package [${packagePage}]")
                    while (packagePage.records.isNotEmpty()) {
                        packagePage.records.forEach { pkg ->
                            //包的版本数 < 保留版本数，直接跳过
                            if (pkg.versions < reserveVersions) return@forEach
                            var deleteVersions: MutableList<PackageVersion>
                            val ruleQueryList: MutableList<PackageVersion>
                            // 所有版本
                            val listVersion = packageService.listAllVersion(
                                projectId,
                                name,
                                pkg.key,
                                VersionListOption()
                            )
                            // 元数据规则不为null，根据规则查询
                            if (rule != null) {
                                ruleQueryList = metadataRuleQuery(rule!!, pkg.id!!).toMutableList()
                                // 直接进入下一条规则的过滤
                                if (ruleQueryList.isEmpty()) {
                                    deleteVersions = reserveVersionsAndDaysFilter(
                                        listVersion,
                                        reserveVersions,
                                        reserveDays
                                    ).toMutableList()
                                } else {
                                    // 排除ruleQueryList，然后进入下一条规则过滤
                                    deleteVersions = listVersion.toMutableList()
                                    deleteVersions.removeAll(ruleQueryList)
                                    deleteVersions = reserveVersionsAndDaysFilter(
                                        deleteVersions,
                                        reserveVersions,
                                        reserveDays
                                    ).toMutableList()
                                }
                            } else {
                                // 直接进入下一条规则的过滤
                                deleteVersions = reserveVersionsAndDaysFilter(
                                    listVersion,
                                    reserveVersions,
                                    reserveDays
                                ).toMutableList()
                            }
                            // 删除
                            logger.info("[packageName:${pkg.name}] delete version collection: $deleteVersions")
                            if (deleteVersions.isNotEmpty()) {
                                deleteVersion(deleteVersions, pkg.key, pkg.type, projectId, name)
                            }
                            logger.info("repository [${tRepository.name}] package [${pkg.name}] clean up complete")
                        }
                        pageNumber += 1
                        packageOption = PackageListOption(
                            pageNumber,
                            DEFAULT_PAGE_SIZE
                        )
                        packagePage = packageService.listPackagePage(projectId, name, option = packageOption)
                    }
                }
            } else {
                logger.warn("repository [${tRepository.name}] clean strategy is null")
            }
        }
    }

    /**
     * 元数据规则查询
     */
    private fun metadataRuleQuery(rule: Rule, packageId: String): List<PackageVersion> {
        val versionList = mutableListOf<PackageVersion>()
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
        logger.info("delete package version ")
        val artifactRegistryService = ArtifactRegistryServiceFactory.getArtifactRegistryService(type)
        versions.forEach {
            artifactRegistryService.deleteVersion(projectId,repoName,packageKey,it.name)
        }
    }

    /**
     * 元数据规则匹配
     */
    private fun matchRule(rules: List<MetadataRule>, versionMetadata: Map<String, Any>): Boolean {
        var match = false
        rules.forEach {
            val ruleName = it.name
            val ruleValue = it.value
            val operationType = it.type
            //【制品包版本的元数据中的key集合】 包含 【当前规则的key】，则进行匹配
            if (versionMetadata.keys.contains(ruleName)) {
                //取出与【清理策略元数据规则的key】相同的【制品包版本元数据的value】
                val versionValue = versionMetadata[ruleName].toString()
                //根据操作类型，进行判断
                match = when (operationType) {
                    OperationType.EQ -> {
                        ruleValue == versionValue
                    }
                    OperationType.REGEX -> {
                        val regex = Regex(ruleValue)
                        regex.containsMatchIn(versionValue)
                    }
                    OperationType.IN -> {
                        versionValue.contains(ruleValue)
                    }
                }
                //匹配到一个，直接返回
                if (match) return match
            }
        }
        return match
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryCleanServiceImpl::class.java)
    }
}
