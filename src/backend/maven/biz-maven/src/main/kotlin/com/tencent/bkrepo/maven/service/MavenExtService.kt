package com.tencent.bkrepo.maven.service

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.model.Sort
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.maven.exception.MavenBadRequestException
import com.tencent.bkrepo.maven.pojo.MavenDependency
import com.tencent.bkrepo.maven.pojo.MavenPlugin
import com.tencent.bkrepo.maven.pojo.response.MavenGAVCResponse
import com.tencent.bkrepo.maven.util.DependencyUtils
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageVersionDependentsClient
import com.tencent.bkrepo.repository.pojo.dependent.PackageVersionDependentsRelation
import com.tencent.bkrepo.repository.pojo.dependent.PackageVersionDependentsRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class MavenExtService(
    private val nodeClient: NodeClient,
    private val packageVersionDependentsClient: PackageVersionDependentsClient
) {

    @Value("\${maven.domain:http://127.0.0.1:25803}")
    val mavenDomain = ""

    fun gavc(
        projectId: String,
        pageNumber: Int,
        pageSize: Int,
        g: String?,
        a: String?,
        v: String?,
        c: String?,
        repos: String?
    ): Response<Page<MavenGAVCResponse.UriResult>> {
        gavcCheck(g, a, v, c)
        val result = buildGavcQuery(projectId, pageNumber, pageSize, g, a, v, c, repos)
        val list = result.data?.records?.map {
            MavenGAVCResponse.UriResult("$mavenDomain/${it["projectId"]}/${it["repoName"]}${it["fullPath"]}")
        }
        val page = Page(
            pageNumber = result.data!!.pageNumber,
            pageSize = result.data!!.pageSize,
            totalRecords = result.data!!.totalRecords,
            totalPages = result.data!!.totalPages,
            records = list!!
        )
        return ResponseBuilder.success(page)
    }

    private fun gavcCheck(g: String?, a: String?, v: String?, c: String?) {
        var result = g.isNullOrBlank()
        listOf(a, v, c).map {
            result = it.isNullOrBlank() && result
        }
        if (result) throw MavenBadRequestException()
    }

    private fun buildGavcQuery(
        projectId: String,
        pageNumber: Int,
        pageSize: Int,
        g: String?,
        a: String?,
        v: String?,
        c: String?,
        repos: String?
    ): Response<Page<Map<String, Any?>>> {
        val rules = mutableListOf<Rule>()
        val repoRules = mutableListOf<Rule>()
        val metadataRules = mutableListOf<Rule>()
        val projectRule = Rule.QueryRule("projectId", projectId)
        g?.let { metadataRules.add(Rule.QueryRule("metadata.groupId", g)) }
        a?.let { metadataRules.add(Rule.QueryRule("metadata.artifactId", a)) }
        v?.let { metadataRules.add(Rule.QueryRule("metadata.version", v)) }
        c?.let { metadataRules.add(Rule.QueryRule("metadata.classifier", c)) }
        repos?.let {
            val repoList = repos.trim(',').split(",")
            for (repo in repoList) {
                if (repo.isNotBlank()) repoRules.add(Rule.QueryRule("repoName", repo))
            }
        }
        rules.add(projectRule)
        rules.add(Rule.QueryRule("folder", false))
        if (repoRules.isNotEmpty()) rules.add(Rule.NestedRule(repoRules, Rule.NestedRule.RelationType.OR))
        rules.add(Rule.NestedRule(metadataRules, Rule.NestedRule.RelationType.AND))

        val rule = Rule.NestedRule(
            rules,
            Rule.NestedRule.RelationType.AND
        )
        val queryModel = QueryModel(
            page = PageLimit(pageNumber = pageNumber, pageSize = pageSize),
            sort = Sort(properties = listOf("lastModifiedDate"), direction = Sort.Direction.ASC),
            select = listOf("projectId", "repoName", "fullPath"),
            rule = rule
        )
        return nodeClient.search(queryModel)
    }

    fun dependencies(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String,
        pageNumber: Int,
        pageSize: Int
    ): Response<Page<MavenDependency>> {
        val (dependencies, _) = dependents(projectId, repoName, packageKey, version)
        return page(dependencies, pageNumber, pageSize)
    }

    fun dependenciesReverse(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String,
        pageNumber: Int,
        pageSize: Int
    ): Response<Page<PackageVersionDependentsRelation>> {
        val searchStr = "dependency:" + PackageKeys.resolveGav(packageKey) + ":$version"
        return packageVersionDependentsClient.dependenciesReverse(
            searchStr = searchStr,
            projectId = projectId,
            repoName = repoName,
            pageNumber = pageNumber,
            pageSize = pageSize
        )
    }

    private fun <T> page(
        set: Set<T>,
        pageNumber: Int,
        pageSize: Int
    ): Response<Page<T>> {
        val totalRecords = set.size.toLong()
        val start = (pageNumber - 1) * pageSize
        val end = pageNumber * pageSize
        if (start > totalRecords) {
            return ResponseBuilder.success(Page(pageNumber, pageSize, totalRecords, emptyList()))
        }
        val page = Page(
            pageNumber = pageNumber,
            pageSize = pageSize,
            totalRecords = totalRecords,
            records = set.toList().subList(start, end.coerceAtMost(set.size))
        )
        return ResponseBuilder.success(page)
    }

    fun plugins(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String,
        pageNumber: Int,
        pageSize: Int
    ): Response<Page<MavenPlugin>> {
        val (_, plugins) = dependents(projectId, repoName, packageKey, version)
        return page(plugins, pageNumber, pageSize)
    }

    private fun dependents(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String,
    ): Pair<Set<MavenDependency>, Set<MavenPlugin>> {
        val result = packageVersionDependentsClient.get(
            PackageVersionDependentsRequest(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageKey,
                version = version,
            )
        ).data
        val dependencies = mutableSetOf<MavenDependency>()
        val plugins = mutableSetOf<MavenPlugin>()
        result?.let {
            it.forEach { dependents ->
                if (dependents.startsWith("dependency")) {
                    dependencies.add(DependencyUtils.toMavenDependency(dependents))
                } else if (dependents.startsWith("plugin")) {
                    plugins.add(DependencyUtils.toMavenPlugin(dependents))
                }
            }
        }
        return Pair(dependencies, plugins)
    }
}
