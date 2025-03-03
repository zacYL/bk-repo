package com.tencent.bkrepo.maven.service

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.common.api.exception.ParameterInvalidException
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.model.Sort
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
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
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.api.VersionDependentsClient
import com.tencent.bkrepo.repository.pojo.dependent.VersionDependentsRelation
import com.tencent.bkrepo.repository.pojo.dependent.VersionDependentsRequest
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.repo.RepoListOption
import org.apache.maven.model.Dependency
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service
class MavenExtService(
    private val nodeClient: NodeClient,
    private val packageClient: PackageClient,
    private val repositoryClient: RepositoryClient,
    private val versionDependentsClient: VersionDependentsClient,
    private val storageManager: StorageManager
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
        val accessibleRepos = repositoryClient.listPermissionRepo(userId, projectId, repoListOption).data!!
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
    ): Response<Page<MavenVersionDependentsRelation>> {
        // 先找到制品包信息
        val packageVersion = packageClient.findVersionByName(projectId, repoName, packageKey, version).data
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
        val response = versionDependentsClient.dependenciesReverse(
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
        return ResponseBuilder.success(
            Page(
                pageNumber = response.data!!.pageNumber,
                pageSize = response.data!!.pageSize,
                totalRecords = response.data!!.totalRecords,
                totalPages = response.data!!.totalPages,
                records = relations!!
            )
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
        logger.info("start query the info of dependents")
        val result = versionDependentsClient.get(
            VersionDependentsRequest(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageKey,
                version = version,
            )
        ).data
        var dependencies = mutableSetOf<MavenDependency>()
        val plugins = mutableSetOf<MavenPlugin>()
        // 先处理result
        result?.let {
            it.forEach { dependents ->
                if (dependents.startsWith("dependency")) {
                    dependencies.add(DependencyUtils.toMavenDependency(dependents))
                } else if (dependents.startsWith("plugin")) {
                    plugins.add(DependencyUtils.toMavenPlugin(dependents))
                }
            }
        }

        // 更新旧的数据
        if (dependencies.any { it.version == "null" }) {
            val (newDependencies, updateFlag) = transferDependencies(
                dependencies, projectId, repoName, packageKey, version
            )
            dependencies = newDependencies
            if (updateFlag) {
                versionDependentsClient.insert(
                    VersionDependentsRelation(
                        projectId = projectId,
                        repoName = repoName,
                        packageKey = packageKey,
                        version = version,
                        ext = null,
                        dependencies = mutableSetOf<String>().apply {
                            addAll(
                                dependencies.map {
                                    it.toSearchString()
                                }
                            )
                            addAll(
                                plugins.map {
                                    it.toSearchString()
                                }
                            )
                        }
                    )
                )
            }
        }
        return Pair(dependencies, plugins)
    }

    private fun transferDependencies(
        dependencies: MutableSet<MavenDependency>,
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String
    ): Pair<MutableSet<MavenDependency>, Boolean> {
        // 二次处理的依赖信息(从pom文件重新读取)
        val dependenciesFromPom = mutableSetOf<MavenDependency>()
        var model: Model? = null

        // 当前的pom文件
        val packageVersion = packageClient.findVersionByName(projectId, repoName, packageKey, version).data
        packageVersion?.run {
            // 判断封装类型
            model = getPom(this, projectId, repoName)
        }

        // 处理父pom问题(存在空版本问题),总共要找三层
        var deep: Int = 1
        // 子pom
        var childModel: Model? = model
        // 更新标志
        var updateFlag = false

        // map存放了处理后的dependency以及对应处理前的信息
        var handledDependencyMap = mutableMapOf<Dependency, MavenDependency>()
        // model对应就是当前处理的jar包依赖，这块是保持不变，childModel和parentModel只是辅助
        // 筛选出version为null的dependency
        val preHandleDependencies = model?.dependencies.orEmpty().filter {
            it.version == null
        }
        while (childModel != null && preHandleDependencies.isNotEmpty()) {
            logger.info("resolve parent pom")

            if (deep > 3) break
            // 无论如何都要更新层数
            logger.info("the depth: $deep")
            deep++
            // 父model
            val parentPom: Model? = getParentPom(projectId, repoName, childModel)

            parentPom?.run {
                logger.info("start assign value for version")
                // val newPlugins = model!!.build.plugins
                // 单独处理dependencies
                handledDependencyMap = getHandledMap(preHandleDependencies, handledDependencyMap, model!!, parentPom)
                if (handledDependencyMap.isNotEmpty()) {
                    updateFlag = true
                }
            }
            // 将当前的parentPom赋值给model继续下一个循环
            // 若parent不存在刚好就赋值为null，退出循环
            childModel = parentPom
        }
        // 更新dependenciesFromPom
        // 未处理的依赖(包括version不为空的，以及不出在已处理map中的)
        val versionDependencies = dependencies.filter {
            it.version != "null"
        }
        // 从version为空的依赖中筛选出处理后仍然为空的version
        val nonHandledDependencies = preHandleDependencies.filter {
            !handledDependencyMap.containsKey(it)
        }
        // 获取对应的mavenDependency
        val nonMavenDependencies = getNonHandledMavenDependency(nonHandledDependencies, dependencies)

        dependenciesFromPom.run {
            addAll(
                // 经过处理后的依赖
                handledDependencyMap.values
            )
            addAll(
                // 不需要处理的依赖（即version不为空）
                versionDependencies
            )
            addAll(
                // 未处理的依赖
                nonMavenDependencies
            )
        }
        // 如果刚好version全部都加上了，还要判断一次,同样需要更新
        if (!dependenciesFromPom.any { it.version == "null" } ||
            handledDependencyMap.isNotEmpty()
        ) {
            updateFlag = true
        }
        return Pair(dependenciesFromPom, updateFlag)
    }

    private fun getNonHandledMavenDependency(
        nonHandleDependencies: List<Dependency>,
        dependencies: MutableSet<MavenDependency>
    ): List<MavenDependency> {
        val result = mutableListOf<MavenDependency>()
        dependencies.filter {
            it.version == "null"
        }.forEach {
            var flag = false
            nonHandleDependencies.forEach { dependency ->
                if (dependency.groupId == it.groupId &&
                    dependency.artifactId == it.artifactId
                ) {
                    flag = true
                }
            }
            if (flag) {
                result.add(it)
            }
        }
        return result
    }

    private fun getHandledMap(
        newDependencies: List<Dependency>,
        map: MutableMap<Dependency, MavenDependency>,
        model: Model,
        parentPom: Model
    ): MutableMap<Dependency, MavenDependency> {
        newDependencies.filter {
            !map.containsKey(it)
        }.forEach { dependency ->
            val parseDependency = DependencyUtils.parseDependency(dependency, model, parentPom)
            if (parseDependency.version != "null" && parseDependency.version != null) {
                map.set(dependency, parseDependency)
            }
        }
        return map
    }

    private fun getPom(
        packageVersion: PackageVersion,
        projectId: String,
        repoName: String,
    ): Model? {
        val packaging = packageVersion.metadata["packaging"] as String? ?: run {
            val matcher = Pattern.compile(PACKAGE_SUFFIX_REGEX).matcher(packageVersion.contentPath!!)
            require(matcher.matches()) {
                "Invalid artifact file format [${packageVersion.contentPath}] in $projectId/$repoName"
            }
            matcher.group(2)
        }

        // 存储信息获取
        val node = nodeClient.getNodeDetail(projectId, repoName, packageVersion.contentPath!!).data
        val storageCredentials = repositoryClient.getRepoDetail(
            projectId, repoName
        ).data?.storageCredentials
        val inputStream = storageManager.loadArtifactInputStream(node, storageCredentials)

        return if (packaging != "pom") {
            // 查询同一目录下的所有节点
            getPomFromNodeList(node, projectId, repoName, storageCredentials)
        } else {
            inputStream?.use { MavenXpp3Reader().read(it) }
        }
    }

    // 从同一目录下获取Pom
    private fun getPomFromNodeList(
        node: NodeDetail?,
        projectId: String,
        repoName: String,
        storageCredentials: StorageCredentials?
    ): Model? {
        node?.let {
            val nodeList = nodeClient.listNodePage(projectId, repoName, node.path).data?.records
            val fullPath = nodeList?.find { nodeInfo -> nodeInfo.fullPath.endsWith(".pom") }?.fullPath
            fullPath?.run {
                val pomNode = nodeClient.getNodeDetail(projectId, repoName, fullPath).data
                val inputStreamPom = storageManager.loadArtifactInputStream(pomNode, storageCredentials)
                // 当前的封装类型
                return inputStreamPom?.use { MavenXpp3Reader().read(it) }
            }
        }
        return null
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
        // 处理父pom问题
        val parentPom = getParentPom(projectId, repoName, model)
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
                            DependencyUtils.parseDependency(it, model, parentPom).toSearchString()
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

    // 获取父pom
    private fun getParentPom(projectId: String, repoName: String, model: Model): Model? {
        val parent = model.parent ?: null
        var parentPom: Model? = null
        // 不存在parent标签
        if (parent == null) {
            return null
        }
        with(parent) {
            logger.info("正在处理父pom")
            // 父pom肯定在同个仓库里
            if (groupId == null || artifactId == null) {
                // 不存在parent信息
                return null
            }
            val packageKey = PackageKeys.ofGav(groupId, artifactId)
            val packageVersion = packageClient.findVersionByName(
                projectId,
                repoName,
                packageKey,
                version
            ).data
            packageVersion?.run {
                // 获取仓库的存储凭证
                val storageCredentials = repositoryClient.getRepoDetail(
                    projectId,
                    repoName
                ).data?.storageCredentials
                // 获取父pom的node信息
                val node = nodeClient.getNodeDetail(
                    projectId,
                    repoName,
                    contentPath!!
                ).data
                val inputStream = storageManager.loadArtifactInputStream(node, storageCredentials)
                parentPom = inputStream.use { MavenXpp3Reader().read(it) }
            }
        }
        return parentPom
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MavenExtService::class.java)
    }
}
