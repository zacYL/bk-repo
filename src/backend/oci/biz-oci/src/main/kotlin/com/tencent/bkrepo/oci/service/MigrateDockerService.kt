package com.tencent.bkrepo.oci.service

import com.tencent.bkrepo.common.api.util.StreamUtils.readText
import com.tencent.bkrepo.common.artifact.constant.SOURCE_TYPE
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.metadata.util.PackageKeys
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.oci.constant.DOCKER_DISTRIBUTION_MANIFEST_LIST_V2
import com.tencent.bkrepo.oci.constant.IMAGE_INDEX_MEDIA_TYPE
import com.tencent.bkrepo.oci.constant.OLD_DOCKER_MEDIA_TYPE
import com.tencent.bkrepo.oci.model.Descriptor
import com.tencent.bkrepo.oci.model.ManifestSchema2
import com.tencent.bkrepo.oci.pojo.digest.OciDigest
import com.tencent.bkrepo.oci.util.OciLocationUtils
import com.tencent.bkrepo.oci.util.OciUtils
import com.tencent.bkrepo.repository.api.NodeService
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.api.StorageCredentialsClient
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveCopyRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageListOption
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionUpdateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MigrateDockerService(
    private val projectClient: ProjectClient,
    private val packageClient: PackageClient,
    private val repositoryClient: RepositoryClient,
    private val nodeClient: NodeService,
    private val storageManager: StorageManager,
    private val storageCredentialsClient: StorageCredentialsClient
) {
    private val logger = LoggerFactory.getLogger(MigrateDockerService::class.java)

    /**
     * docker：
     *  {projectId}/{repoName}/{packageName}/{version}
     * docker manifest:
     *  {projectId}/{repoName}/{packageName}/{version}/list.manifest.json
     *
     * oci:
     *  {projectId}/{repoName}/{packageName}/blobs/{version}
     *  {projectId}/{repoName}/{packageName}/manifest/{version}
     */
    fun migrate(overwrite: Boolean) {
        logger.info("migrate Docker pkg start")
        val projects = projectClient.listProject().data
        projects?.forEach { projectInfo ->
            val repos = repositoryClient.listRepo(projectInfo.name, type = RepositoryType.DOCKER.name).data
            repos?.forEach { repo ->
                migrateRepo(repo, overwrite)
            }
        }
    }

    private fun migrateRepo(repo: RepositoryInfo, overwrite: Boolean) {
        logger.info("migrate ${repo.projectId}/${repo.name} Docker pkg start")
        val storageCredentials = storageCredentialsClient.findByKey(repo.storageCredentialsKey).data
        var pageNumber = 1
        var packages = packageClient.listPackagePage(
            projectId = repo.projectId,
            repoName = repo.name,
            option = PackageListOption(pageNumber = pageNumber, pageSize = PAGE_SIZE)
        ).data?.records
        while (!packages.isNullOrEmpty()) {
            packages.forEach { pkg ->
                val versionList = packageClient.listAllVersion(
                    projectId = pkg.projectId,
                    repoName = pkg.repoName,
                    packageKey = pkg.key
                ).data
                versionList?.forEach { version ->
                    migrateVersion(
                        pkg.projectId,
                        pkg.repoName,
                        pkg.key,
                        version,
                        storageCredentials,
                        overwrite
                    )
                }
            }
            pageNumber++
            packages = packageClient.listPackagePage(
                projectId = repo.projectId,
                repoName = repo.name,
                option = PackageListOption(pageNumber = pageNumber, pageSize = PAGE_SIZE)
            ).data?.records
        }
    }

    private fun migrateVersion(
        projectId: String,
        repoName: String,
        packageKey: String,
        packageVersion: PackageVersion,
        storageCredentials: StorageCredentials?,
        overwrite: Boolean
    ) {
        logger.info("migrate $projectId/$repoName/$packageKey/${packageVersion.name} Docker pkg version start")
        // 迁移完成的版本，写入元数据标记，并在重复执行时根据元数据标记判断是否跳过
        val refreshedMetadata = packageVersion.packageMetadata.firstOrNull { it.key == REFRESHED }
        if (refreshedMetadata != null) {
            logger.info("skip $projectId/$repoName/$packageKey/${packageVersion.name} migrate")
            return
        }

        val pkgName = PackageKeys.resolveDocker(packageKey)
        val path = "/$pkgName"
        val manifestPath = "$path/${packageVersion.name}/manifest.json"
        val manifestListPath = "$path/${packageVersion.name}/list.manifest.json"
        val manifestNode = nodeClient.getNodeDetail(projectId, repoName, manifestPath).data ?: run {
            nodeClient.getNodeDetail(projectId, repoName, manifestListPath).data
        }

        if (manifestNode != null) {
            val manifestNodeFullPath = manifestNode.fullPath
            // 分别处理 manifest.json 和 list.manifest.json
            val type = manifestNode.nodeMetadata.firstOrNull { it.key == OLD_DOCKER_MEDIA_TYPE }
            var migrateStatus = true
            // harbor 迁移到cpack的docker制品不存在 type
            if (
                type != null &&
                (type.value == DOCKER_DISTRIBUTION_MANIFEST_LIST_V2 || type.value == IMAGE_INDEX_MEDIA_TYPE)
            ) {
                // list.manifest.json 只需要迁移本身
                // {projectId}/{repoName}/{packageName}/{version}/list.manifest.json
                migrateStatus = doMigrateManifest(manifestNode, path, packageVersion.name, overwrite)
                if (migrateStatus) {
                    deleteOldDockerArtifact(projectId, repoName, pkgName, packageVersion.name)
                }
            } else {
                // 其他情况按照 manifest.json 处理
                // manifest.json 需要迁移blobs和manifest
                val manifest = loadManifest(manifestNode, storageCredentials) ?: run {
                    logger.error("load manifest[$manifestNodeFullPath] is null")
                    return
                }
                OciUtils.manifestIterator(manifest).forEach {
                    migrateStatus = migrateStatus
                            && doMigrateBlob(manifestNode, it, path, packageVersion.name, overwrite)
                }
                // blob迁移完成后，迁移 manifest.json
                if (migrateStatus) {
                    migrateStatus = doMigrateManifest(manifestNode, path, packageVersion.name, overwrite)
                }
                // manifest.json 迁移完成后，删除旧blob和manifest.json
                if (migrateStatus) {
                    // check oci artifact
                    migrateStatus = checkOciArtifact(
                        projectId,
                        repoName,
                        packageKey,
                        packageVersion.name,
                        storageCredentials
                    )
                    deleteOldDockerArtifact(projectId, repoName, pkgName, packageVersion.name)
                    logger.info("migrate $projectId/$repoName/$packageKey/${packageVersion.name} success...")
                }
            }
            val metadata = packageVersion.packageMetadata.toMutableList()
            metadata.add(MetadataModel(key = REFRESHED, value = true))
            if (packageVersion.manifestPath == null && metadata.none { it.key == SOURCE_TYPE }) {
                metadata.add(MetadataModel(key = SOURCE_TYPE, value = ArtifactChannel.REPLICATION))
            }
            val newManifestFullPath = "$path/manifest/${packageVersion.name}/${manifestNode.name}"
            // 更新包版本信息 metadat, artifactPath, manifestPath
            packageClient.updateVersion(PackageVersionUpdateRequest(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageKey,
                versionName = packageVersion.name,
                manifestPath = newManifestFullPath,
                artifactPath = newManifestFullPath,
                packageMetadata = metadata,
            ))
        } else {
            logger.info("$path manifestPath,manifestListPath node not find")
        }
    }

    private fun deleteOldDockerArtifact(
        projectId: String,
        repoName: String,
        pkgName: String,
        version: String
    ) {
        val oldDockerPath = "/$pkgName/$version"
        logger.info("delete old docker artifact start, path:[$oldDockerPath]")
        nodeClient.deleteNode(NodeDeleteRequest(projectId, repoName, oldDockerPath, SYSTEM_USER))
        logger.info("delete old docker artifact path:[$oldDockerPath] success...")
    }

    private fun checkOciArtifact(
        projectId: String,
        repoName: String,
        pkgKey: String,
        version: String,
        storageCredentials: StorageCredentials?
    ): Boolean {
        logger.info("check oci artifact start")
        // 检查版本是否存在
        packageClient.findVersionByName(projectId, repoName, pkgKey, version).data ?: run {
            logger.error("$projectId/$repoName/$pkgKey/$version not find")
            return false
        }
        // get manifest.json
        val pkgName = PackageKeys.resolveDocker(pkgKey)
        val manifestFullPath = OciLocationUtils.buildManifestPath(pkgName, version)
        // check manifest.json node
        val manifestNode = nodeClient.getNodeDetail(projectId, repoName, manifestFullPath).data ?: run {
            logger.error("$projectId/$repoName/$pkgKey/$version manifest not find")
            return false
        }
        val manifest = loadManifest(manifestNode, storageCredentials) ?: run {
            logger.error("load manifest[${manifestNode.fullPath}] is null")
            return false
        }

        var checkStatus = true
        OciUtils.manifestIterator(manifest).forEach {
            val blobFullPath = OciLocationUtils.blobVersionPathLocation(version, pkgName, it.filename)
            nodeClient.checkExist(projectId, repoName, blobFullPath).data?.let { exist ->
                if (!exist) {
                    logger.error("blob[$projectId/$repoName/$blobFullPath] not find")
                }
                checkStatus = checkStatus && exist
            } ?: logger.error("blob[$projectId/$repoName/$blobFullPath] check fail")
        }
        logger.info("check oci artifact result:[$checkStatus]")
        return checkStatus
    }

    private fun doMigrateManifest(
        manifestNode: NodeDetail,
        path: String,
        version: String,
        overwrite: Boolean
    ): Boolean {
        logger.info(
            "migrating manifest[${manifestNode.fullPath}] in repo ${manifestNode.projectId}/${manifestNode.repoName}"
        )
        val newManifestFullPath = "$path/manifest/$version/${manifestNode.name}"
        nodeClient.copyNode(
            NodeMoveCopyRequest(
                srcProjectId = manifestNode.projectId,
                srcRepoName = manifestNode.repoName,
                srcFullPath = manifestNode.fullPath,
                destProjectId = manifestNode.projectId,
                destRepoName = manifestNode.repoName,
                destFullPath = newManifestFullPath,
                operator = SYSTEM_USER,
                overwrite = overwrite
            )
        )
        return true
    }

    private fun doMigrateBlob(
        manifestNode: NodeDetail,
        descriptor: Descriptor,
        path: String,
        version: String,
        overwrite: Boolean
    ): Boolean {
        logger.info(
            "migrating blob digest [${descriptor.digest}] in repo ${manifestNode.projectId}/${manifestNode.repoName}"
        )
        if (!OciDigest.isValid(descriptor.digest)) {
            logger.info("Invalid blob digest [$descriptor]")
            return false
        }
        val oldBlobFullPath = "$path/$version/${descriptor.filename}"
        val newBlobFullPath = "$path/blobs/$version/${descriptor.filename}"
        nodeClient.copyNode(
            NodeMoveCopyRequest(
                srcProjectId = manifestNode.projectId,
                srcRepoName = manifestNode.repoName,
                srcFullPath = oldBlobFullPath,
                destProjectId = manifestNode.projectId,
                destRepoName = manifestNode.repoName,
                destFullPath = newBlobFullPath,
                operator = SYSTEM_USER,
                overwrite = overwrite
            )
        )
        return true
    }

    private fun loadManifest(
        manifestNode: NodeDetail,
        storageCredentials: StorageCredentials?
    ): ManifestSchema2? {
        return try {
            val manifestBytes = storageManager.loadArtifactInputStream(manifestNode, storageCredentials)!!.readText()
            OciUtils.stringToManifestV2(manifestBytes)
        } catch (e: Exception) {
            logger.error("load manifest[${manifestNode.fullPath}] fail, ${e.message}")
            null
        }
    }

    fun migrateRepository(projectId: String, repoName: String, overwrite: Boolean) {
        val repoInfo = repositoryClient.getRepoInfo(projectId, repoName).data ?: run {
            logger.error("repo[$projectId/$repoName] find fail")
            return
        }
        require(repoInfo.type == RepositoryType.DOCKER)
        migrateRepo(repoInfo, overwrite)
    }

    fun migratePackageVersion(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String,
        overwrite: Boolean
    ) {
        val repoDetail = repositoryClient.getRepoDetail(
            projectId,
            repoName,
            type = RepositoryType.DOCKER.name
        ).data ?: run {
            logger.error("repo[$projectId/$repoName] find fail")
            return
        }
        val packageVersion = packageClient.findVersionByName(projectId, repoName, packageKey, version).data ?: run {
            logger.error("repo[$projectId/$repoName/$packageKey/$version] find fail")
            return
        }
        migrateVersion(projectId, repoName, packageKey, packageVersion, repoDetail.storageCredentials, overwrite)
    }

    companion object {
        const val REFRESHED = "refreshed"
        const val PAGE_SIZE = 1000
    }
}
