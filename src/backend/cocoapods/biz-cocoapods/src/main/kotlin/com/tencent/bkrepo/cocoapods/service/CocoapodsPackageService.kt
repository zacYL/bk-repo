package com.tencent.bkrepo.cocoapods.service

import com.tencent.bkrepo.cocoapods.pojo.artifact.CocoapodsArtifactInfo
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CocoapodsPackageService(
    private val packageClient: PackageClient
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
