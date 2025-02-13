package com.tencent.bkrepo.npm.service.impl

import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.constants.NPM_FILE_FULL_PATH
import com.tencent.bkrepo.npm.constants.PACKAGE_METADATA
import com.tencent.bkrepo.npm.model.metadata.NpmPackageMetaData
import com.tencent.bkrepo.npm.service.NpmOperationService
import com.tencent.bkrepo.npm.utils.NpmUtils
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.springframework.stereotype.Service

@Service
class NpmOperationServiceImpl(
    private val packageClient: PackageClient,
    private val nodeClient: NodeClient,
    private val storageManager: StorageManager,
) : NpmOperationService {
    override fun packageVersion(context: ArtifactContext): PackageVersion? {
        with(context) {
            val (packageName, packageVersion) = if (context is ArtifactUploadContext) {
                NpmUtils.parseNameAndVersionFromFullPath(getAttributes()[NPM_FILE_FULL_PATH] as String)
            } else {
                NpmUtils.parseNameAndVersionFromFullPath(artifactInfo.getArtifactFullPath())
            }
            val packageKey = NpmUtils.packageKeyByRepoType(packageName, context.repositoryDetail.type)
            return packageClient.findVersionByName(projectId, repoName, packageKey, packageVersion).data
        }
    }

    override fun loadPackageMetadata(context: ArtifactContext): NpmPackageMetaData? {
        with(context) {
            return getAttribute<NpmPackageMetaData>(PACKAGE_METADATA) ?: run {
                val npmArtifactInfo = artifactInfo as NpmArtifactInfo
                val fullPath = NpmUtils.getPackageMetadataPath(npmArtifactInfo.packageName)
                val pkgMetadataNode = nodeClient.getNodeDetail(projectId, repoName, fullPath).data
                storageManager.loadArtifactInputStream(pkgMetadataNode, storageCredentials)?.use {
                    it.readJsonString<NpmPackageMetaData>()
                }?.also { putAttribute(PACKAGE_METADATA, it) }
            }
        }
    }
}
