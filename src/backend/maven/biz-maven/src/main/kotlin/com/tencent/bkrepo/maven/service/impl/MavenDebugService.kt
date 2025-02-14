package com.tencent.bkrepo.maven.service.impl

import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.maven.exception.JarFormatException
import com.tencent.bkrepo.maven.service.MavenExtService
import com.tencent.bkrepo.maven.util.JarUtils
import com.tencent.bkrepo.maven.util.MavenGAVCUtils.toMavenGAVC
import com.tencent.bkrepo.repository.api.*
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.metadata.packages.PackageMetadataSaveRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageListOption
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream

@Service
class MavenDebugService(
    private val projectClient: ProjectClient,
    private val packageClient: PackageClient,
    private val repositoryClient: RepositoryClient,
    private val nodeClient: NodeClient,
    private val storageManager: StorageManager,
    private val mavenExtService: MavenExtService,
    private val packageMetadataClient: PackageMetadataClient
) {
    fun dependenciesForeach() {
        // 遍历所有项目
        val projects = projectClient.listProject().data
        logger.info("find projects: ${projects?.size}")
        // 遍历项目下maven仓库
        projects?.let { projectInfos ->
            projectInfos.forEach { project ->
                logger.info("find project: ${project.name}")
                val repos = repositoryClient.listRepo(project.name, type = RepositoryType.MAVEN.name).data
                // 遍历仓库下包
                repos?.let { repositoryInfos ->
                    repositoryInfos.forEach { repo ->
                        forEachPackageInRepo(repo)
                    }
                }
            }
        }
    }

    private fun forEachPackageInRepo(repo: RepositoryInfo) {
        logger.info("find repo: ${repo.name}")
        var pageNumber = 1
        while (true) {
            val packages = findPackage(repo.projectId, repo.name, pageNumber)
            if (packages.isNullOrEmpty()) break
            pageNumber++
            // 遍历包下面的版本
            packages.forEach { packageVersion ->
                val versions = packageClient.listAllVersion(
                    repo.projectId,
                    repo.name,
                    packageKey = packageVersion.key
                ).data
                versions?.let { versionList ->
                    versionList.forEach { version ->
                        flushVersionDependent(repo, packageVersion.key, version)
                    }
                }
            }
        }
    }

    private fun flushVersionDependent(repo: RepositoryInfo, packageKey: String, version: PackageVersion) {
        logger.info("flush data: [${repo.projectId}/${repo.name}/$packageKey/${version.name}]")
        val repoDetail = repositoryClient.getRepoDetail(repo.projectId, repo.name).data!!
        // 找到版本关联的文件
        val jarPath = version.contentPath
        if (jarPath != null) {
            val mavenGAVC = jarPath.toMavenGAVC()
            val jarNode = nodeClient.getNodeDetail(repo.projectId, repo.name, jarPath).data
            if (jarNode != null) {
                val oldVersionMetadata = version.packageMetadata
                // 更新包版本信息
                val metadata: MutableMap<String, Any> = mutableMapOf(
                    "groupId" to mavenGAVC.groupId,
                    "artifactId" to mavenGAVC.artifactId,
                    "version" to mavenGAVC.version,
                    "packaging" to mavenGAVC.packaging
                )
                mavenGAVC.classifier?.let {
                    metadata["classifier"] = it
                }
                oldVersionMetadata.forEach {
                    if (!metadata.containsKey(it.key)) {
                        metadata[it.key] = it.value
                    }
                }
                logger.info("update package version metadata: $metadata")
                packageMetadataClient.saveMetadata(
                    PackageMetadataSaveRequest(
                        projectId = repo.projectId,
                        repoName = repo.name,
                        packageKey = packageKey,
                        version = version.name,
                        versionMetadata = metadata.map { MetadataModel(key = it.key, value = it.value, system = true, display = true) }
                    )
                )
                val model = if (mavenGAVC.packaging == "pom") {
                    storageManager.loadArtifactInputStream(jarNode, repoDetail.storageCredentials)?.use {
                        MavenXpp3Reader().read(it)
                    }
                } else {
                    val jarFile = File.createTempFile("maven", jarNode.name)
                    try {
                        FileOutputStream(jarFile).use { outputStream ->
                            storageManager.loadArtifactInputStream(jarNode, repoDetail.storageCredentials)?.use {
                                it.copyTo(outputStream)
                            }
                        }
                        JarUtils.parseModelInJar(jarFile)
                    } catch (e: JarFormatException) {
                        logger.error("parse jar error: ${jarNode.name}")
                        null
                    } finally {
                        jarFile.delete()
                    }
                }
                model?.let { mavenExtService.addVersionDependents(mavenGAVC, it, repo.projectId, repo.name) }
            } else {
                logger.info("jarNode is null: [${repo.projectId}/${repo.name}/$jarPath]")
            }
        } else {
            logger.error(
                "package_version: [$packageKey/${version.name}] " +
                    "has no jar file"
            )
        }
    }

    private fun findPackage(projectId: String, repoName: String, pageNumber: Int): List<PackageSummary>? {
        return packageClient.listPackagePage(
            projectId = projectId,
            repoName = repoName,
            option = PackageListOption(
                pageSize = PAGE_SIZE,
                pageNumber = pageNumber
            )
        ).data?.records
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MavenDebugService::class.java)
        private const val PAGE_SIZE = 10000
    }
}
