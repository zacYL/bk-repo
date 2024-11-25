package com.tencent.bkrepo.cocoapods.service

import com.tencent.bkrepo.cocoapods.exception.CocoapodsMessageCode
import com.tencent.bkrepo.cocoapods.exception.CocoapodsPodSpecNotFoundException
import com.tencent.bkrepo.cocoapods.pojo.artifact.CocoapodsArtifactInfo
import com.tencent.bkrepo.cocoapods.pojo.user.PackageVersionInfo
import com.tencent.bkrepo.cocoapods.utils.ObjectBuildUtil.buildBasicInfo
import com.tencent.bkrepo.common.artifact.exception.PackageNotFoundException
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.PackageMetadataClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CocoapodsWebService(
    private val nodeClient: NodeClient,
    private val packageClient: PackageClient,
    private val cocoapodsFileService: CocoapodsFileService
){
    fun deletePackage(artifactInfo: CocoapodsArtifactInfo, packageKey: String) {
        logger.info("cocoapods delete package...")
        with(artifactInfo){
            packageClient.listAllVersion(projectId,repoName,packageKey).data?.forEach{
                packageClient.deleteVersion(projectId, repoName, packageKey, it.name)
                val copyArtifactInfo = artifactInfo.copy() as CocoapodsArtifactInfo
                copyArtifactInfo.apply {
                    this.version = it.name
                }
                cocoapodsFileService.deleteFile(copyArtifactInfo)
            }?: logger.warn("[$projectId/$repoName/$packageKey] version not found")
            packageClient.deletePackage(projectId,repoName,packageKey)
        }
    }

    fun deleteVersion(artifactInfo: CocoapodsArtifactInfo, packageKey: String, version: String) {
        logger.info("cocoapods delete version...")
        with(artifactInfo){
            this.version = version
            packageClient.deleteVersion(projectId,repoName,packageKey,version)
            cocoapodsFileService.deleteFile(artifactInfo)
        }
        logger.info("delete version artifactInfo [$artifactInfo]")
    }

    fun artifactDetail(
        cocoapodsArtifactInfo: CocoapodsArtifactInfo,
        packageKey: String,
        version: String
    ): PackageVersionInfo {
        with(cocoapodsArtifactInfo) {
            val packageVersion = packageClient.findVersionByName(projectId, repoName, packageKey, version).data ?: run {
                logger.warn("packageKey [$packageKey] don't found.")
                throw PackageNotFoundException(packageKey)
            }
            val nodeDetail = nodeClient.getNodeDetail(projectId, repoName, packageVersion.contentPath!!).data ?: run {
                logger.warn("node [${packageVersion.contentPath}] don't found.")
                throw CocoapodsPodSpecNotFoundException(CocoapodsMessageCode.COCOAPODS_PODSPEC_NOT_FOUND)
            }
            return PackageVersionInfo(buildBasicInfo(nodeDetail, packageVersion), packageVersion.packageMetadata)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CocoapodsWebService::class.java)

    }
}