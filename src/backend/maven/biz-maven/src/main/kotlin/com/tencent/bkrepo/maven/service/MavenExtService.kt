package com.tencent.bkrepo.maven.service

import com.tencent.bkrepo.common.api.exception.ParameterInvalidException
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.metadata.service.node.NodeSearchService
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.metadata.service.packages.PackageService
import com.tencent.bkrepo.common.metadata.service.repo.RepositoryService
import com.tencent.bkrepo.common.metadata.util.PackageKeys
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.model.Sort
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.maven.constants.PACKAGE_SUFFIX_REGEX
import com.tencent.bkrepo.maven.enum.MavenMessageCode
import com.tencent.bkrepo.maven.exception.MavenArtifactNotFoundException
import com.tencent.bkrepo.maven.pojo.MavenDependency
import com.tencent.bkrepo.maven.pojo.MavenGAVC
import com.tencent.bkrepo.maven.pojo.MavenPlugin
import com.tencent.bkrepo.maven.pojo.MavenVersionDependentsRelation
import com.tencent.bkrepo.maven.pojo.response.MavenGAVCResponse
import com.tencent.bkrepo.maven.util.DependencyUtils
import com.tencent.bkrepo.maven.util.DependencyUtils.toReverseSearchString
import com.tencent.bkrepo.maven.util.DependencyUtils.toSearchString
import com.tencent.bkrepo.repository.api.VersionDependentsClient
import com.tencent.bkrepo.repository.pojo.dependent.VersionDependentsRelation
import com.tencent.bkrepo.repository.pojo.dependent.VersionDependentsRequest
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.repo.RepoListOption
import org.apache.maven.model.Model
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service
class MavenExtService(
    private val nodeSearchService: NodeSearchService,
    private val packageService: PackageService,
    private val repositoryService: RepositoryService,
    private val versionDependentsClient: VersionDependentsClient
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
        val userId = SecurityUtils.getUserId()
        val repoListOption = RepoListOption(
            type = PackageType.MAVEN.name,
            category = listOf(
                RepositoryCategory.LOCAL.name,
                RepositoryCategory.REMOTE.name,
                RepositoryCategory.COMPOSITE.name
            ),
            actions = listOf(PermissionAction.READ)
        )
        val accessibleRepos = repositoryService.listPermissionRepo(userId, projectId, repoListOption)
            .map { it.name }.toSet()
        val inputRepos = repos?.split(",")?.map { it.trim() }
        val queryRepos = (inputRepos?.intersect(accessibleRepos) ?: accessibleRepos)
            .ifEmpty { throw PermissionException() }
        val result = buildGavcQuery(projectId, pageNumber, pageSize, g, a, v, c, queryRepos)
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
        if (result) throw ParameterInvalidException("$g|$a|$v|$c")
    }

    private fun buildGavcQuery(
        projectId: String,
        pageNumber: Int,
        pageSize: Int,
        g: String?,
        a: String?,
        v: String?,
        c: String?,
        repoList: Set<String>
    ): Response<Page<Map<String, Any?>>> {
        val rules = mutableListOf<Rule>()
        val repoRules = mutableListOf<Rule>()
        val metadataRules = mutableListOf<Rule>()
        val projectRule = Rule.QueryRule("projectId", projectId)
        g?.let { metadataRules.add(Rule.QueryRule("metadata.groupId", g)) }
        a?.let { metadataRules.add(Rule.QueryRule("metadata.artifactId", a)) }
        v?.let { metadataRules.add(Rule.QueryRule("metadata.version", v)) }
        c?.let { metadataRules.add(Rule.QueryRule("metadata.classifier", c)) }
        for (repo in repoList) {
            repoRules.add(Rule.QueryRule("repoName", repo))
        }
        rules.add(projectRule)
        rules.add(Rule.QueryRule("folder", false))
        if (repoRules.isNotEmpty()) rules.add(Rule.NestedRule(repoRules, Rule.NestedRule.RelationType.OR))
        rules.add(Rule.NestedRule(metadataRules, Rule.NestedRule.RelationType.AND))

        val rule = Rule.NestedRule(
            rules, Rule.NestedRule.RelationType.AND
        )
        val queryModel = QueryModel(
            page = PageLimit(pageNumber = pageNumber, pageSize = pageSize),
            sort = Sort(properties = listOf("lastModifiedDate"), direction = Sort.Direction.ASC),
            select = listOf("projectId", "repoName", "fullPath"),
            rule = rule
        )
        return ResponseBuilder.success(nodeSearchService.search(queryModel))
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
    ): Response<Page<MavenVersionDependentsRelation>> {
        // 先找到制品包信息
        val packageVersion = packageService.findVersionByName(projectId, repoName, packageKey, version)
            ?: throw MavenArtifactNotFoundException(MavenMessageCode.MAVEN_ARTIFACT_NOT_FOUND)
        val type = packageVersion.metadata["packaging"] as String? ?: run {
            val matcher = Pattern.compile(PACKAGE_SUFFIX_REGEX).matcher(packageVersion.contentPath!!)
            require(matcher.matches()) {
                "Invalid artifact file format [${packageVersion.contentPath}] in $projectId/$repoName"
            }
            matcher.group(2)
        }
        val mavenDependency = MavenDependency(
            groupId = packageVersion.metadata["groupId"] as String,
            artifactId = packageVersion.metadata["artifactId"] as String,
            version = packageVersion.metadata["version"] as String,
            type = type,
            classifier = packageVersion.metadata["classifier"] as? String,
            scope = null,
            optional = null
        )
        val searchStr = mavenDependency.toReverseSearchString()
        val response =  versionDependentsClient.dependenciesReverse(
            searchStr = searchStr,
            projectId = projectId,
            repoName = repoName,
            pageNumber = pageNumber,
            pageSize = pageSize
        )
        val relations = response.data?.records?.map { it ->
            val arr = PackageKeys.resolveGav(it.packageKey).split(":")
            val type = it.ext?.firstOrNull { it.key == "type" }?.value as? String ?: "jar"
            val classifier = it.ext?.firstOrNull { it.key == "classifier" }?.value as? String
            MavenVersionDependentsRelation(
                projectId = it.projectId,
                repoName = it.repoName,
                packageKey = it.packageKey,
                groupId = arr[0],
                    artifactId = arr[1],
                version = it.version,
                    type = type,
                    classifier = classifier,
                    dependencies = null
            )
        }
        return ResponseBuilder.success(Page(
            pageNumber = response.data!!.pageNumber,
            pageSize = response.data!!.pageSize,
            totalRecords = response.data!!.totalRecords,
            totalPages = response.data!!.totalPages,
            records = relations!!
        ))
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
        val result = versionDependentsClient.get(
            VersionDependentsRequest(
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

    fun addVersionDependents(mavenGavc: MavenGAVC, model: Model, projectId: String, repoName: String) {
        val ext = mutableListOf<MetadataModel>().apply {
            add(MetadataModel("type", mavenGavc.packaging))
            mavenGavc.classifier?.let { add(MetadataModel("classifier", it)) }
        }
        val dependencies = try {
            model.dependencies
        } catch (e: NullPointerException) {
            logger.warn("Failed to parse dependencies from pom.xml")
            listOf()
        }

        val plugins = try {
            model.build.plugins
        } catch (e: NullPointerException) {
            logger.warn("Failed to parse plugins from pom.xml")
            listOf()
        }
        versionDependentsClient.insert(
            VersionDependentsRelation(
                projectId = projectId,
                repoName = repoName,
                packageKey = PackageKeys.ofGav(mavenGavc.groupId, mavenGavc.artifactId),
                version = mavenGavc.version,
                ext = ext,
                dependencies = mutableSetOf<String>().apply {
                    addAll(
                        dependencies.map {
                            DependencyUtils.parseDependency(it, model).toSearchString()
                        }
                    )
                    addAll(
                        plugins.map {
                            DependencyUtils.parsePlugin(it).toSearchString()
                        }
                    )
                }
            )
        )
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MavenExtService::class.java)
    }
}
