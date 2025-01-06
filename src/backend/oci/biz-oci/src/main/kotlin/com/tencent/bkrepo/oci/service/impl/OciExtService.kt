package com.tencent.bkrepo.oci.service.impl

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.exception.RepoNotFoundException
import com.tencent.bkrepo.common.artifact.exception.VersionNotFoundException
import com.tencent.bkrepo.common.artifact.pojo.request.PackageVersionMoveCopyRequest
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactExtService
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.oci.constant.MANIFEST
import com.tencent.bkrepo.oci.constant.OCI_MANIFEST_LIST
import com.tencent.bkrepo.oci.constant.PACKAGE_KEY
import com.tencent.bkrepo.oci.constant.VERSION
import com.tencent.bkrepo.oci.pojo.artifact.OciArtifactInfo
import com.tencent.bkrepo.oci.pojo.artifact.OciDeleteArtifactInfo
import com.tencent.bkrepo.oci.pojo.user.OciPackageVersionInfo
import com.tencent.bkrepo.oci.service.OciOperationService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OciExtService(
    private val operationService: OciOperationService,
) : ArtifactExtService() {
    override fun getVersionDetail(userId: String, artifactInfo: ArtifactInfo): OciPackageVersionInfo {
        with(artifactInfo as OciArtifactInfo) {
            val packageKey = HttpContextHolder.getRequest().getParameter(PACKAGE_KEY)
            val version = HttpContextHolder.getRequest().getParameter(VERSION)
            return operationService.detailVersion(userId, this, packageKey, version)
        }
    }

    override fun deletePackage(userId: String, artifactInfo: ArtifactInfo) {
        operationService.deletePackage(userId, artifactInfo as OciArtifactInfo)
    }

    override fun deleteVersion(userId: String, artifactInfo: ArtifactInfo) {
        operationService.deleteVersion(userId, artifactInfo as OciArtifactInfo)
    }

    override fun buildVersionDeleteArtifactInfo(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String
    ): ArtifactInfo {
        return OciDeleteArtifactInfo(
            projectId = projectId,
            repoName = repoName,
            packageName = PackageKeys.resolveName(packageKey),
            version = version
        )
    }

    override fun moveCopyVersion(request: PackageVersionMoveCopyRequest, move: Boolean) {
        with(request) {
            val srcVersion = packageClient.findVersionByName(srcProjectId, srcRepoName, packageKey, version).data
                ?: throw VersionNotFoundException("$packageKey/$version")
            val manifestPath = srcVersion.manifestPath!!
            if (manifestPath.substringAfterLast("/") == OCI_MANIFEST_LIST) {
                val packagePath = manifestPath.removeSuffix("$version/$OCI_MANIFEST_LIST")
                val versionList = resolveDigestList(srcProjectId, srcRepoName, manifestPath)?.mapNotNull { digest ->
                    operationService.getNodeByDigest(srcProjectId, srcRepoName, digest, packagePath)?.fullPath?.let {
                        if (it.endsWith("/$MANIFEST")) it.removeSuffix("/$MANIFEST").substringAfterLast("/")
                        else null
                    }
                } ?: throw RuntimeException("manifest list [$srcProjectId/$srcRepoName/$manifestPath] resolve error!")
                logger.info("version list of [$srcProjectId/$srcRepoName/$packageKey/$version]: $versionList")
                // 这些版本可能同时被其它manifest list引用，因此不移除
                versionList.forEach { moveCopyVersion(request.copy(version = it), false) }
            }
            super.moveCopyVersion(request, move)
        }
    }

    private fun resolveDigestList(projectId: String, repoName: String, manifestPath: String): List<String>? {
        val node = nodeClient.getNodeDetail(projectId, repoName, manifestPath).data
            ?: throw NodeNotFoundException("$projectId/$repoName/$manifestPath")
        val srcRepo = repositoryClient.getRepoDetail(projectId, repoName).data
            ?: throw RepoNotFoundException("$projectId/$repoName")
        return operationService.loadManifestList(node.sha256!!, node.size, srcRepo.storageCredentials)
                ?.manifests?.map { it.digest }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OciExtService::class.java)
    }
}
