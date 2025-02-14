package com.tencent.bkrepo.cocoapods.service

import com.tencent.bkrepo.cocoapods.pojo.artifact.CocoapodsArtifactInfo
import com.tencent.bkrepo.common.metadata.util.PackageKeys
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CocoapodsPackageService(
    private val packageClient: PackageClient,
    private val nodeClient: NodeClient,
) {
    fun createVersion(
        artifactInfo: CocoapodsArtifactInfo,
        size: Long,
    ) {
        with(artifactInfo) {
            val request = PackageVersionCreateRequest(
                projectId = projectId,
                repoName = repoName,
                packageName = name,
                packageKey = PackageKeys.ofCocoapods(name),
                packageType = PackageType.COCOAPODS,
                versionName = version,
                size = size,
                artifactPath = getArtifactFullPath(),
                packageMetadata = listOf(),
                overwrite = true,
                createdBy = SecurityUtils.getUserId(),
            )
            packageClient.createVersion(request, HttpContextHolder.getClientAddress())
            logger.info("created version for cocoapods,repo [$repoName], package[$name $version]")
        }
    }

    fun deleteVersionFile(artifactInfo: CocoapodsArtifactInfo, packageVersion: PackageVersion) {
        with(artifactInfo) {
            val filePath = packageVersion.contentPath?.substringBeforeLast("/")
            val specsPath = ".specs/${name}/${packageVersion.name}"
            var request = NodeDeleteRequest(projectId, repoName, filePath ?: "", SecurityUtils.getUserId())
            nodeClient.deleteNode(request)
            logger.info("delete filePath:[$filePath]")
            request = NodeDeleteRequest(projectId, repoName, specsPath, SecurityUtils.getUserId())
            nodeClient.deleteNode(request)
            logger.info("delete specsPath:[$specsPath]")
        }
    }

    fun deletePackageFile(artifactInfo: CocoapodsArtifactInfo) {
        logger.info("delete package file...")
        with(artifactInfo) {
            val packageFilePath = "/$orgName"
            val packageSpecsPath = ".specs/$name"
            var request = NodeDeleteRequest(projectId, repoName, packageFilePath, SecurityUtils.getUserId())
            nodeClient.deleteNode(request)
            logger.info("delete packageFilePath:[$packageFilePath]")
            request = NodeDeleteRequest(projectId, repoName, packageSpecsPath, SecurityUtils.getUserId())
            nodeClient.deleteNode(request)
            logger.info("delete packageSpecsPath:[$packageSpecsPath]")
        }
    }

    fun buildDownloadRecord(artifactInfo: CocoapodsArtifactInfo, userId: String): PackageDownloadRecord? {
        with(artifactInfo) {
            return PackageDownloadRecord(
                projectId = projectId,
                repoName = repoName,
                packageKey = PackageKeys.ofCocoapods(name),
                packageVersion = version,
                userId = userId
            )
        }
    }


    companion object {
        private val logger = LoggerFactory.getLogger(CocoapodsPackageService::class.java)
    }
}
