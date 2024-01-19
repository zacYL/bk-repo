package com.tencent.bkrepo.repository.service.packages.impl

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.HumanReadable
import com.tencent.bkrepo.common.artifact.constant.SOURCE_TYPE
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.mongo.dao.AbstractMongoDao.Companion.ID
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.dao.PackageDao
import com.tencent.bkrepo.repository.dao.PackageVersionDao
import com.tencent.bkrepo.repository.model.TMetadata
import com.tencent.bkrepo.repository.dao.RepositoryDao
import com.tencent.bkrepo.repository.model.TPackage
import com.tencent.bkrepo.repository.model.TPackageVersion
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.model.TRepository
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveCopyRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageListOption
import com.tencent.bkrepo.repository.pojo.packages.VersionListOption
import com.tencent.bkrepo.repository.service.node.NodeService
import com.tencent.bkrepo.repository.service.packages.PackageRepairService
import com.tencent.bkrepo.repository.service.packages.PackageService
import com.tencent.bkrepo.repository.service.repo.RepositoryService
import com.tencent.bkrepo.repository.util.PackageQueryHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import kotlin.system.measureNanoTime

@Service
class PackageRepairServiceImpl(
    private val packageService: PackageService,
    private val repositoryService: RepositoryService,
    private val nodeService: NodeService,
    private val packageDao: PackageDao,
    private val packageVersionDao: PackageVersionDao,
    private val repositoryDao: RepositoryDao,
    private val nodeDao: NodeDao
) : PackageRepairService {
    override fun repairHistoryVersion() {
        // 查询仓库下面的所有package的包
        var successCount = 0L
        var failedCount = 0L
        var totalCount = 0L
        val startTime = LocalDateTime.now()

        // 查询所有的包
        logger.info("starting repair package history version.")
        // 分页查询包信息
        var page = 1
        val packagePage = queryPackage(page)
        var packageList = packagePage.records
        val total = packagePage.totalRecords
        if (packageList.isEmpty()) {
            logger.info("no package found, return.")
            return
        }
        while (packageList.isNotEmpty()) {
            packageList.forEach {
                logger.info(
                    "Retrieved $total packages to repair history version," +
                        " process: $totalCount/$total"
                )
                val projectId = it.projectId
                val repoName = it.repoName
                val key = it.key
                try {
                    // 添加包管理
                    doRepairPackageHistoryVersion(it)
                    logger.info("Success to repair history version for [$key] in repo [$projectId/$repoName].")
                    successCount += 1
                } catch (exception: RuntimeException) {
                    logger.error(
                        "Failed to repair history version for [$key] in repo [$projectId/$repoName]," +
                            " message: $exception"
                    )
                    failedCount += 1
                } finally {
                    totalCount += 1
                }
            }
            page += 1
            packageList = queryPackage(page).records
        }
        val durationSeconds = Duration.between(startTime, LocalDateTime.now()).seconds
        logger.info(
            "Repair package history version, " +
                "total: $totalCount, success: $successCount, failed: $failedCount," +
                " duration $durationSeconds s totally."
        )
    }

    override fun repairVersionCount() {
        var updated = 0L
        var failed = 0L
        var total = 0L
        var pageNumber = 1

        measureNanoTime {
            while (true) {
                val packageList = queryPackage(pageNumber++).records.takeIf { it.isNotEmpty() } ?: break
                total += packageList.size
                logger.info("Retrieved ${packageList.size} packages to recount version")
                packageList.forEach {
                    try {
                        val result = updateVersionCount(it) ?: return@forEach
                        if (result) {
                            updated += 1
                        } else {
                            logger.error("Failed to update package[${it.key}] in repo[${it.projectId}/${it.repoName}]")
                            failed += 1
                        }
                    } catch (e: Exception) {
                        logger.error(
                            "Failed to recount or update. Package[${it.key}]. Repo[${it.projectId}/${it.repoName}]. " +
                            "Message: ${e.message}"
                        )
                        failed += 1
                    }
                }
            }
        }.apply {
            val elapsedTime = HumanReadable.time(this)
            logger.info(
                "Recounted version of all packages. Total recounted[$total], " +
                        "including updated[$updated], failed[$failed]. Elapse[$elapsedTime]"
            )
        }
    }

    override fun repairDockerManifestPath() {
        var updated = 0L
        var failed = 0L
        var total = 0L
        var pageNumber = 1
        val pageSize = 1000
        measureNanoTime {
            do {
                val pageRequest = Pages.ofRequest(pageNumber, pageSize)
                val dockerPackages =
                    packageDao.find(Query(where(TPackage::type).isEqualTo(PackageType.DOCKER)).with(pageRequest))
                dockerPackages.forEach { tPackage ->
                    val criteria = where(TPackageVersion::packageId).isEqualTo(tPackage.id)
                        .and(TPackageVersion::manifestPath).isEqualTo(null)
                    packageVersionDao.find(Query(criteria)).forEach { tVersion ->
                        logger.info(
                            "Repairing manifestPath: [${tPackage.projectId}/${tPackage.repoName}] " +
                                    "[${tPackage.key}:${tVersion.name}]"
                        )
                        if (updateVersionManifestPath(tPackage.key, tVersion)) updated++ else failed++
                        total++
                    }
                }
                pageNumber++
            } while (dockerPackages.size == pageSize)
        }.apply {
            val elapsedTime = HumanReadable.time(this)
            logger.info(
                "update docker manifestPath finished. Total to update[$total], success[$updated], " +
                        "fail[$failed], elapsed[$elapsedTime]"
            )
        }
    }

    private fun updateVersionManifestPath(
        packageKey: String,
        version: TPackageVersion
    ): Boolean {
        version.manifestPath = "/${PackageKeys.resolveDocker(packageKey)}/${version.name}/manifest.json"
        if (version.metadata.none { it.key == SOURCE_TYPE }) {
            logger.info("metadata sourceType miss")
            val tMetadata = TMetadata(
                key = SOURCE_TYPE,
                value = ArtifactChannel.REPLICATION,
                system = true
            )
            version.metadata = version.metadata.plusElement(tMetadata)
        }
        return try {
            packageVersionDao.save(version)
            true
        } catch (e: Exception) {
            logger.warn("Failed to save TPackageVersion [$packageKey / ${version.name}]", e)
            false
        }
    }

    /**
     * artifactPath to be modified like this format: /@types/node/-/@types/node-17.0.13.tgz
     */
    override fun repairNpmArtifactPath(): Map<String, Long> {
        var success = 0L
        var fail = 0L
        val criteria = where(TRepository::type).isEqualTo(RepositoryType.NPM)
            .and(TRepository::category).isEqualTo(RepositoryCategory.REMOTE)
        repositoryDao.find(Query(criteria)).forEach { repo ->
            var pageNumber = 1
            do {
                val packages = packageService.listPackagePage(
                    repo.projectId, repo.name, PackageListOption(pageNumber, PAGE_SIZE, "@")
                ).records
                packages.filter { it.name.startsWith("@") && it.name.contains("/") }.forEach {
                    val modifiedResult = repairNpmArtifactPathWithinPackage(it.projectId, it.repoName, it.id!!)
                    success += modifiedResult.first
                    fail += modifiedResult.second
                }
                pageNumber++
            } while (packages.size == PAGE_SIZE)
        }
        val total = success + fail
        logger.info("repair npm artifactPath complete. total[$total], success[$success], fail[$fail]")
        return mapOf(
            "success" to success,
            "fail" to fail,
            "total" to total
        )
    }

    override fun repairOciManifestPath(): Map<String, Long> {
        var success = 0L
        var fail = 0L
        var skip = 0L
        val repos = repositoryService.allRepos(null, null, repoType = RepositoryType.DOCKER).filterNotNull()
        repos.forEach { repo ->
            var pageNumber = 1
            do {
                val packages = packageService.listPackagePage(
                    repo.projectId, repo.name, PackageListOption(pageNumber, PAGE_SIZE)
                ).records
                packages.filter { it.historyVersion.any { v -> v.contains("sha256") } }.forEach {
                    val modifiedResult =
                        repairOciManifestPathWithinPackage(it.projectId, it.repoName, it.id!!)
                    success += modifiedResult.first
                    fail += modifiedResult.second
                    skip += modifiedResult.third
                }
                pageNumber++
            } while (packages.size == PAGE_SIZE)
        }
        val total = success + fail + skip
        logger.info("repair oci manifestPath complete. total[$total], success[$success], fail[$fail], skip[$skip]")
        return mapOf(
            "success" to success,
            "fail" to fail,
            "skip" to skip,
            "total" to total
        )
    }

    private fun repairOciManifestPathWithinPackage(
        projectId: String,
        repoName: String,
        packageId: String
    ): Triple<Long, Long, Long> {
        var success = 0L
        var fail = 0L
        var skip = 0L
        val versions = packageVersionDao.find(PackageQueryHelper.versionListQuery(packageId, "sha256"))
        versions.forEach {
            // 本地仓库迁移修复, 仅多架构包的摘要版本需要修复
            // /image/manifest/sha256:xxx 移动到 /image/manifest/sha256:xxx/manifest.json
            if (
                it.metadata.any { m -> m.key == "refreshed" } &&
                it.manifestPath?.endsWith("/manifest.json") == true &&
                nodeDao.exists(projectId, repoName, it.manifestPath!!.removeSuffix("/manifest.json"))
            ) {
                if (nodeDao.exists(projectId, repoName, it.manifestPath!!)) {
                    logger.warn(
                        "==== skip move node [${it.manifestPath!!.removeSuffix("/manifest.json")}]" +
                                " in [$projectId/$repoName]"
                    )
                    skip++
                    return@forEach
                }
                // dir conflict
                val tempPath = it.manifestPath!!.removeSuffix("/manifest.json").replace(":", "__")
                nodeService.moveNode(
                    NodeMoveCopyRequest(
                        srcProjectId = projectId,
                        srcRepoName = repoName,
                        srcFullPath = it.manifestPath!!.removeSuffix("/manifest.json"),
                        destFullPath = tempPath,
                        operator = SYSTEM_USER
                    )
                )
                nodeService.moveNode(
                    NodeMoveCopyRequest(
                        srcProjectId = projectId,
                        srcRepoName = repoName,
                        srcFullPath = tempPath,
                        destFullPath = it.manifestPath!!,
                        operator = SYSTEM_USER
                    )
                )
                logger.info(
                    "==== move node [${it.manifestPath!!.removeSuffix("/manifest.json")}] to " +
                        "[${it.manifestPath!!}] in [$projectId/$repoName]"
                )
                success++
                return@forEach
            }
            // 其它修复
            // /image/manifest/sha256__xxx -> /image/manifest/sha256:xxx/<list.>manifest.json
            if (
                it.manifestPath?.substringAfterLast("/")?.startsWith("sha256__") == true &&
                it.manifestPath == it.artifactPath
            ) {
                var fat = false
                val manifestNode = nodeDao.findNode(projectId, repoName, it.manifestPath!!)!!
                val mediaType = manifestNode.metadata?.find { m -> m.key == "mediaType" }?.value
                if (
                    mediaType == "application/vnd.docker.distribution.manifest.list.v2+json" ||
                    mediaType == "application/vnd.oci.image.index.v1+json"
                ) fat = true
                val newPath = it.manifestPath!!.replace("__", ":") +
                        if (fat) "/list.manifest.json" else "/manifest.json"
                nodeService.moveNode(
                    NodeMoveCopyRequest(
                        srcProjectId = projectId,
                        srcRepoName = repoName,
                        srcFullPath = it.manifestPath!!,
                        destFullPath = newPath,
                        operator = SYSTEM_USER
                    )
                )
                val query = Query(Criteria.where(ID).isEqualTo(it.id))
                val update = Update()
                    .set(TPackageVersion::artifactPath.name, newPath)
                    .set(TPackageVersion::manifestPath.name, newPath)
                val result = packageVersionDao.updateFirst(query, update)
                if (result.modifiedCount == 1L) {
                    success++
                    logger.info(
                        "==== successfully modify m&aPath[${it.artifactPath}] to [$newPath] in [$projectId/$repoName]"
                    )
                } else {
                    fail++
                    logger.warn(
                        "==== fail to modify m&aPath[${it.artifactPath}] to [$newPath] in [$projectId/$repoName]"
                    )
                }
            }
        }
        return Triple(success, fail, skip)
    }

    private fun repairNpmArtifactPathWithinPackage(
        projectId: String,
        repoName: String,
        packageId: String
    ): Pair<Long, Long> {
        var success = 0L
        var fail = 0L
        val versions = packageVersionDao.find(PackageQueryHelper.versionListQuery(packageId))
        versions.forEach {
            if (it.artifactPath?.matches(npmArtifactPathRegex) == true) {
                val pathWithHyphen = it.artifactPath!!.replace(replaceRegex, "/-")
                val pathWithoutHyphen = it.artifactPath!!.replace("/-/", "/download/")
                val newPath = if (nodeDao.exists(projectId, repoName, pathWithHyphen)) {
                    pathWithHyphen
                } else if (nodeDao.exists(projectId, repoName, pathWithoutHyphen)) {
                    pathWithoutHyphen
                } else {
                    fail++
                    logger.warn("tgz node [$pathWithHyphen], [$pathWithoutHyphen] not found in [$projectId/$repoName]")
                    return@forEach
                }
                val query = Query(Criteria.where(ID).isEqualTo(it.id))
                val update = Update().set(TPackageVersion::artifactPath.name, newPath)
                val result = packageVersionDao.updateFirst(query, update)
                if (result.modifiedCount == 1L) {
                    success++
                    logger.info(
                        "successfully modify artifactPath[${it.artifactPath}] to [$newPath] in [$projectId/$repoName]"
                    )
                } else {
                    fail++
                    logger.warn(
                        "fail to modify artifactPath[${it.artifactPath}] to [$newPath] in [$projectId/$repoName]"
                    )
                }
            }
        }
        return Pair(success, fail)
    }

    private fun updateVersionCount(tPackage: TPackage): Boolean? {
        val actualCount = packageVersionDao.countVersion(tPackage.id!!)
        return if (actualCount == tPackage.versions) {
            null
        } else {
            logger.info(
                "Updating version count of package[${tPackage.key}] " +
                    "in repo[${tPackage.projectId}/${tPackage.repoName}] (${tPackage.versions} -> $actualCount)"
            )
            val query = PackageQueryHelper.packageQuery(tPackage.projectId, tPackage.repoName, tPackage.key)
            val update = Update().set(TPackage::versions.name, actualCount)
            packageDao.updateFirst(query, update).modifiedCount == 1L
        }
    }

    private fun doRepairPackageHistoryVersion(tPackage: TPackage) {
        with(tPackage) {
            val allVersion = packageService.listAllVersion(projectId, repoName, key, VersionListOption())
                .map { it.name }
            historyVersion = historyVersion.toMutableSet().apply { addAll(allVersion) }
            packageDao.save(this)
        }
    }

    private fun queryPackage(page: Int): Page<TPackage> {
        val query = Query().with(
            Sort.by(Sort.Direction.DESC, TPackage::projectId.name, TPackage::repoName.name, TPackage::key.name)
        )
        val totalRecords = packageDao.count(query)
        val pageRequest = Pages.ofRequest(page, 10000)
        val records = packageDao.find(query.with(pageRequest))
        return Pages.ofResponse(pageRequest, totalRecords, records)
    }

    companion object {
        private val npmArtifactPathRegex = Regex("/@[\\w.-]+/[\\w.-]+/-/@[\\w.-]+/[\\w.-]+-.+\\.tgz")
        private val replaceRegex = Regex("/-/@[\\w.-]+")
        private const val PAGE_SIZE = 1000
        private val logger: Logger = LoggerFactory.getLogger(PackageRepairServiceImpl::class.java)
    }
}
