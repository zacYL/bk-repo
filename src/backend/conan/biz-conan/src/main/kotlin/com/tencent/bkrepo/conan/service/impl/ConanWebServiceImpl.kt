package com.tencent.bkrepo.conan.service.impl

import com.tencent.bkrepo.common.artifact.exception.PackageNotFoundException
import com.tencent.bkrepo.conan.constant.ConanMessageCode
import com.tencent.bkrepo.conan.exception.ConanFileNotFoundException
import com.tencent.bkrepo.conan.pojo.artifact.ConanArtifactInfo
import com.tencent.bkrepo.conan.pojo.user.PackageVersionInfo
import com.tencent.bkrepo.conan.service.ConanWebService
import com.tencent.bkrepo.conan.utils.ObjectBuildUtil.buildBasicInfo
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ConanWebServiceImpl(
    private val nodeClient: NodeClient,
    private val packageClient: PackageClient
) : ConanWebService {

    override fun artifactDetail(conanArtifactInfo: ConanArtifactInfo, packageKey: String, version: String): PackageVersionInfo? {
        with(conanArtifactInfo) {
            val packageVersion = packageClient.findVersionByName(projectId, repoName, packageKey, version).data ?: run {
                logger.warn("packageKey [$packageKey] don't found.")
                throw PackageNotFoundException(packageKey)
            }
            val nodeDetail = nodeClient.getNodeDetail(projectId, repoName, packageVersion.contentPath!!).data ?: run {
                logger.warn("node [${packageVersion.contentPath}] don't found.")
                throw ConanFileNotFoundException(ConanMessageCode.CONAN_RECIPE_NOT_FOUND, packageVersion.contentPath!!, "$projectId|$repoName")
            }
            return PackageVersionInfo(buildBasicInfo(nodeDetail, packageVersion), packageVersion.packageMetadata)
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ConanWebServiceImpl::class.java)

    }
}