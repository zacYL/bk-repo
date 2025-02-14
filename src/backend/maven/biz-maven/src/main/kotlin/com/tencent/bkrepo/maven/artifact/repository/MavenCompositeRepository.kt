package com.tencent.bkrepo.maven.artifact.repository

import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.constant.SOURCE_TYPE
import com.tencent.bkrepo.common.artifact.exception.ArtifactNotInWhitelistException
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.repository.composite.CompositeRepository
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.metadata.service.repo.ProxyChannelService
import com.tencent.bkrepo.maven.artifact.MavenArtifactInfo
import com.tencent.bkrepo.maven.util.MavenStringUtils.isSnapshotMetadataChecksumUri
import com.tencent.bkrepo.maven.util.MavenStringUtils.isSnapshotMetadataUri
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class MavenCompositeRepository(
        private val mavenLocalRepository: MavenLocalRepository,
        private val mavenRemoteRepository: MavenRemoteRepository,
        proxyChannelService: ProxyChannelService
) : CompositeRepository(mavenLocalRepository, mavenRemoteRepository, proxyChannelService) {

    @Suppress("TooGenericExceptionCaught")
    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        var artifactResource: ArtifactResource? = null
        try {
            val fullPath = context.artifactInfo.getArtifactFullPath()
            artifactResource = if (fullPath.isSnapshotMetadataUri() || fullPath.isSnapshotMetadataChecksumUri()) {
                downloadFromFirstProxyRepo(context) ?: mavenLocalRepository.onDownload(context)
            } else {
                mavenLocalRepository.onDownload(context) ?: downloadFromFirstProxyRepo(context)
            }
        } catch (notFoundException: NotFoundException) {
            artifactResource = downloadFromFirstProxyRepo(context)
            // 这里是为了保留各依赖源实现的异常
            if (artifactResource == null) {
                throw notFoundException
            }
        }
        return artifactResource
    }

    override fun whitelistInterceptor(context: ArtifactDownloadContext) {
        (context.artifactInfo as MavenArtifactInfo).let {
            if (it.isArtifact() && whitelistSwitchService.get(RepositoryType.MAVEN)) {
                nodeService.getNodeDetail(
                    ArtifactInfo(it.projectId, it.repoName, it.getArtifactFullPath())
                )?.let { nodeDetail ->
                    nodeDetail.nodeMetadata.forEach { metadataModel ->
                        if (metadataModel.key == SOURCE_TYPE &&
                            metadataModel.value == ArtifactChannel.PROXY.name &&
                            !remotePackageWhitelistService.existWhitelist(
                                RepositoryType.MAVEN, "${it.groupId}:${it.artifactId}", it.versionId,
                            )
                        ) {
                            throw ArtifactNotInWhitelistException()
                        }
                    }
                }
            }
        }
    }

    override fun <R> mapFirstProxyRepo(context: ArtifactContext, action: (ArtifactContext) -> R?): R? {
        val proxyChannelList = getProxyChannelList(context)
        for (setting in proxyChannelList) {
            try {
                action(getContextFromProxyChannel(context, setting))?.let {
                    // 无论请求是否成功, 都会返回kotlin.Unit
                    if (it != Unit) {
                        return it
                    }
                }
            } catch (downloadException: ArtifactNotInWhitelistException) {
                throw ArtifactNotInWhitelistException()
            } catch (ignored: Exception) {
                logger.warn("Failed to execute map with channel ${setting.name}", ignored)
            }
        }
        return null
    }

    private fun downloadFromFirstProxyRepo(context: ArtifactDownloadContext): ArtifactResource? {
        return mapFirstProxyRepo(context) {
            require(it is ArtifactDownloadContext)
            // 这里只会返回空，异常不会抛出
            mavenRemoteRepository.onDownload(it)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MavenCompositeRepository::class.java)
    }
}
