package com.tencent.bkrepo.npm.artifact.repository

import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toCompactJsonString
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.constant.SOURCE_TYPE
import com.tencent.bkrepo.common.artifact.exception.ArtifactNotInWhitelistException
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.repository.composite.CompositeRepository
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.metadata.service.repo.ProxyChannelService
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.constants.NPM_FILE_FULL_PATH
import com.tencent.bkrepo.npm.model.metadata.NpmPackageMetaData
import com.tencent.bkrepo.npm.utils.NpmStreamUtils.toArtifactStream
import com.tencent.bkrepo.npm.utils.NpmUtils
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * 使用自定义的bean对象替换到composite上层实现
 */
@Component
@Primary
class NpmCompositeRepository(
    private val localRepository: LocalRepository,
    private val remoteRepository: RemoteRepository,
    proxyChannelService: ProxyChannelService
) : CompositeRepository(localRepository, remoteRepository, proxyChannelService) {

    override fun whitelistInterceptor(context: ArtifactDownloadContext) {
        if (whitelistSwitchService.get(RepositoryType.NPM)) {
            val fullPath = context.getStringAttribute(NPM_FILE_FULL_PATH)?: return
            logger.info("npm local fullPath: $fullPath")
            val packageInfo = NpmUtils.parseNameAndVersionFromFullPath(context.artifactInfo.getArtifactFullPath())
            logger.info("npm local packageInfo: [${packageInfo.first} : ${packageInfo.second}]")
            nodeService.getNodeDetail(ArtifactInfo(context.projectId, context.repoName, fullPath))?.let { nodeDetail ->
                // 如果节点在仓库中存在
                logger.info("npm local nodeDetail: $nodeDetail")
                nodeDetail.nodeMetadata.forEach { metadataModel ->
                    // 判断是否来自代理源缓存
                    if (metadataModel.key == SOURCE_TYPE &&
                        metadataModel.value == ArtifactChannel.PROXY.name &&
                        !remotePackageWhitelistService.existWhitelist(
                            context.repo.type, packageInfo.first, packageInfo.second
                        )
                    ) {
                        // 抛出异常 Http.status = 423
                        throw ArtifactNotInWhitelistException()
                    }
                }
            }
        }
    }

    override fun query(context: ArtifactQueryContext): ArtifactInputStream? {
        val name = (context.artifactInfo as NpmArtifactInfo).packageName
        val remoteQueryResult = mapFirstProxyRepo(context) {
            require(it is ArtifactQueryContext)
            remoteRepository.query(it) as ArtifactInputStream?
        }
        val packageKey = NpmUtils.packageKeyByRepoType(name, context.repositoryDetail.type)
        val packageSummary =
            packageService.findPackageByKey(context.projectId, context.repoName, packageKey)
        val localQueryResult = if (packageSummary != null || remoteQueryResult == null)
            localRepository.query(context) as ArtifactInputStream? else null
        return if (localQueryResult != null && remoteQueryResult != null) {
            val localVersionMap = localQueryResult.readJsonString<NpmPackageMetaData>().versions.map
            val remotePackageMetaData = remoteQueryResult.readJsonString<NpmPackageMetaData>()
            remotePackageMetaData.versions.map.putAll(localVersionMap)
            remotePackageMetaData.toCompactJsonString().toArtifactStream()
        } else localQueryResult ?: remoteQueryResult
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NpmCompositeRepository::class.java)
    }
}
