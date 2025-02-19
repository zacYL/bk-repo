package com.tencent.bkrepo.npm.service

import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.npm.model.metadata.NpmPackageMetaData
import com.tencent.bkrepo.npm.utils.NpmUtils
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NpmReplicaService(
    private val nodeClient: NodeClient,
    private val repoClient: RepositoryClient,
    private val storageManager: StorageManager,
    private val storageProperties: StorageProperties,
) {

    /**
     * 处理制品分发，package.json和name-version.json文件的tarball字段
     */
    fun resolveIndexFile(event: ArtifactEvent) {
        with(event) {
            logger.info("resolve index event:$event")
            val packageFilePath = data["fullPath"] as? String
                ?: throw IllegalArgumentException("fullPath is missing or not a string")
            logger.info("resolve index file,path [$packageFilePath]")
            val domain = data["domain"] as? String
            val repoDetail = repoClient.getRepoDetail(projectId, repoName).data as RepositoryDetail

            val name = data["packageName"] as String
            val version = data["packageVersion"] as String
            // 处理package.json
            val packageJsonPath = NpmUtils.getPackageMetadataPath(name)
            updateIndexJson(packageJsonPath, repoDetail, domain, packageFilePath, version)
            // 处理name-version.json
            val nameVersionJsonPath = NpmUtils.getVersionPackageMetadataPath(name, version)
            updateIndexJson(nameVersionJsonPath, repoDetail, domain, packageFilePath, version)
            return
        }
    }

    private fun ArtifactEvent.updateIndexJson(
        packageJsonPath: String,
        repoDetail: RepositoryDetail,
        domain: String?,
        packageFilePath: String,
        version: String,
    ) {
        val nodeDetail = nodeClient.getNodeDetail(projectId, repoName, packageJsonPath).data as NodeDetail
        val indexFileInputStream =
            storageManager.loadArtifactInputStream(nodeDetail, repoDetail.storageCredentials?: storageProperties.defaultStorageCredentials()) ?: return

        indexFileInputStream.use {
            val packageMetaData = JsonUtils.objectMapper.readValue(it, NpmPackageMetaData::class.java)
            val tarball = "$domain/npm/$projectId/$repoName/$packageFilePath"
            packageMetaData.versions.map[version]?.dist?.tarball = tarball
            // 重新上传
            val inputStream = JsonUtils.objectMapper.writeValueAsString(packageMetaData).byteInputStream()
            val artifactFile =
                inputStream.use { metadataInputStream -> ArtifactFileFactory.build(metadataInputStream) }
            val packageJsonNode = NodeCreateRequest(
                projectId = projectId,
                repoName = repoName,
                fullPath = packageJsonPath,
                folder = false,
                overwrite = true,
                size = artifactFile.getSize(),
                sha256 = artifactFile.getFileSha256(),
                md5 = artifactFile.getFileMd5(),
                operator = SYSTEM_USER
            )
            storageManager.storeArtifactFile(packageJsonNode, artifactFile, repoDetail.storageCredentials)
            artifactFile.delete()
        }
    }

    private fun store(artifactFile: ArtifactFile, repoDetail: RepositoryDetail, indexFilePath: String) {
        logger.info("start to store $indexFilePath")
        with(repoDetail) {
            val nodeCreateRequest = NodeCreateRequest(
                projectId,
                name,
                indexFilePath,
                false,
                sha256 = artifactFile.getFileSha256(),
                md5 = artifactFile.getFileMd5(),
                overwrite = true,
                size = artifactFile.getSize()
            )
            logger.info("nodeCreateRequest: $nodeCreateRequest")
            val nodeDetail =
                storageManager.storeArtifactFile(nodeCreateRequest, artifactFile, storageCredentials)
            logger.info("the new nodeDetail is: $nodeDetail")
        }
        logger.info("store success")
    }

    companion object {
        val logger = LoggerFactory.getLogger(NpmReplicaService::class.java)
    }
}
