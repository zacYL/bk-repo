package com.tencent.bkrepo.repository.service.packages.impl

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.artifact.constant.PUBLIC_PROXY_PROJECT
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.util.MongoEscapeUtils
import com.tencent.bkrepo.repository.dao.PackageDao
import com.tencent.bkrepo.repository.model.TPackage
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import com.tencent.bkrepo.repository.pojo.software.CpackGlobalSearchPojo
import com.tencent.bkrepo.repository.pojo.software.ProjectPackageOverview
import com.tencent.bkrepo.repository.search.cpack.packages.CpackPackageSearchInterpreter
import com.tencent.bkrepo.repository.service.packages.CpackPackageService
import com.tencent.bkrepo.repository.service.repo.CpackRepositoryService
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service

@Service
class CpackPackageServiceImpl(
    private val packageDao: PackageDao,
    private val cpackRepositoryService: CpackRepositoryService,
    private val cpackPackageSearchInterpreter: CpackPackageSearchInterpreter
) : CpackPackageService {

    override fun packageOverview(
        repoType: RepositoryType,
        projectId: String?,
        repoName: String?,
        packageName: String?
    ): List<ProjectPackageOverview> {
        val criteria = Criteria.where(TPackage::type.name).`is`(repoType)
        if (projectId != null && projectId.isNotBlank()) {
            criteria.and(TPackage::projectId.name).`is`(projectId)
            if (repoName != null) {
                criteria.and(TPackage::repoName.name).`is`(repoName)
            } else {
                val allSoftRepo = cpackRepositoryService.listRepo(projectId, includeGeneric = false)
                val repoNames = allSoftRepo.map { it.name }
                criteria.and(TPackage::repoName.name).`in`(repoNames)
            }
        } else {
            transCri(criteria, repoType)
        }
        packageName?.let {
            val escapedValue = MongoEscapeUtils.escapeRegexExceptWildcard(packageName)
            val regexPattern = escapedValue.replace("*", ".*")
            criteria.and(TPackage::name.name).regex("^$regexPattern$")
        }
        val aggregation = Aggregation.newAggregation(
            TPackage::class.java,
            Aggregation.match(criteria),
            Aggregation.group("\$projectId", "\$repoName").sum("\$versions").`as`("count")
        )
        val result = packageDao.aggregate(aggregation, CpackGlobalSearchPojo::class.java).mappedResults

        return transTree(result)
    }

    private fun transCri(criteria: Criteria, repoType: RepositoryType) {
        val allSoftRepo = cpackRepositoryService.listRepo(type = repoType, includeGeneric = false)
        val projectMap = transRepoTree(allSoftRepo)
        val criteriaSet = mutableSetOf<Criteria>()
        projectMap.map {
            val singleCri = Criteria.where(TPackage::projectId.name).`is`(it.key)
                .and(TPackage::repoName.name).`in`(it.value)
            criteriaSet.add(singleCri)
        }
        criteria.orOperator(*criteriaSet.toTypedArray())
    }

    private fun transRepoTree(repos: List<RepositoryInfo>): Map<String, List<String>> {
        val projectMap = mutableMapOf<String, MutableList<String>>()
        repos.filter { it.projectId != PUBLIC_PROXY_PROJECT }.map {
            if (projectMap.containsKey(it.projectId)) {
                projectMap[it.projectId]?.add(it.name)
            } else {
                projectMap[it.projectId] = mutableListOf(it.name)
            }
        }
        return projectMap
    }

    private fun transTree(list: List<CpackGlobalSearchPojo>): List<ProjectPackageOverview> {
        val projectSet = mutableSetOf<ProjectPackageOverview>()
        list.map { pojo ->
            val repoOverview = ProjectPackageOverview.RepoPackageOverview(
                repoName = pojo.id.repoName,
                packages = pojo.count
            )
            projectSet.filter { it.projectId == pojo.id.projectId }.apply {
                if (this.isEmpty()) {
                    projectSet.add(
                        ProjectPackageOverview(
                            projectId = pojo.id.projectId,
                            repos = mutableSetOf(repoOverview),
                            sum = pojo.count
                        )
                    )
                } else {
                    this.first().repos.apply { this.add(repoOverview) }
                    this.first().sum += pojo.count
                }
            }
        }
        return projectSet.toList()
    }

    override fun searchPackage(queryModel: QueryModel): Page<MutableMap<*, *>> {
        val rules = (queryModel.rule as Rule.NestedRule).rules
        var repoName: String? = null
        var projectId: String? = null
        var repoType: RepositoryType? = null
        val projectSubRule = mutableListOf<Rule>()
        for (rule in rules) {
            val queryRule = rule as Rule.QueryRule
            when (queryRule.field) {
                "repoName" -> repoName = queryRule.value as String
                "projectId" -> projectId = queryRule.value as String
                "repoType" -> repoType = RepositoryType.valueOf(queryRule.value as String)
                else -> projectSubRule.add(queryRule)
            }
        }
        if (projectId == null) {
            val projectsRule = mutableListOf<Rule>()
            val allSoftRepo = cpackRepositoryService.listRepo(type = repoType, includeGeneric = false)
            val projectMap = transRepoTree(allSoftRepo)
            projectMap.map { project ->
                val projectRule = Rule.QueryRule(field = "projectId", value = project.key)
                val reposRule = Rule.QueryRule(field = "repoName", value = project.value, operation = OperationType.IN)
                val rule = Rule.NestedRule(
                    mutableListOf(
                        projectRule,
                        reposRule,
                        rules.first { (it as Rule.QueryRule).field == "repoType" }
                    ).apply { this.addAll(projectSubRule) },
                    Rule.NestedRule.RelationType.AND
                )
                projectsRule.add(rule)
            }
            rules.apply {
                this.clear()
                this.addAll(projectsRule)
            }
            queryModel.rule = Rule.NestedRule(rules, Rule.NestedRule.RelationType.OR)
        }
        if (projectId != null && repoName == null) {
            val genericRepos =
                cpackRepositoryService.listRepo(projectId, type = repoType, includeGeneric = false).map { it.name }
            rules.add(Rule.QueryRule(field = "repoName", value = genericRepos, operation = OperationType.IN))
        }
        val context = cpackPackageSearchInterpreter.interpret(queryModel)
        val query = context.mongoQuery
        val countQuery = Query.of(query).limit(0).skip(0)
        val totalRecords = packageDao.count(countQuery)
        val packageList = packageDao.find(query, MutableMap::class.java)
        val pageNumber = if (query.limit == 0) 0 else (query.skip / query.limit).toInt()
        return Page(pageNumber + 1, query.limit, totalRecords, packageList)
    }
}
