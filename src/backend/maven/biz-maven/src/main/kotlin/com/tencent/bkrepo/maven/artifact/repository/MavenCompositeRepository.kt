package com.tencent.bkrepo.maven.artifact.repository

import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.artifact.repository.composite.CompositeRepository
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.repository.api.ProxyChannelClient
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class MavenCompositeRepository(
        private val mavenLocalRepository: MavenLocalRepository,
        private val mavenRemoteRepository: MavenRemoteRepository,
        proxyChannelClient: ProxyChannelClient
) : CompositeRepository(mavenLocalRepository, mavenRemoteRepository, proxyChannelClient) {

    @Suppress("TooGenericExceptionCaught")
    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        var artifactResource: ArtifactResource? = null
        try {
            artifactResource = mavenLocalRepository.onDownload(context)
        } catch (notFoundException: NotFoundException) {
            mapFirstProxyRepo(context) {
                require(it is ArtifactDownloadContext)
                // 这里只会返回空，异常不会抛出
                artifactResource = mavenRemoteRepository.onDownload(it)
                if (artifactResource != null) {
                    return@mapFirstProxyRepo artifactResource
                } else {}
            }
            // 这里是为了保留各依赖源实现的异常
            if (artifactResource == null) {
                throw notFoundException
            }
        }
        if (artifactResource == null) {
            mapFirstProxyRepo(context) {
                require(it is ArtifactDownloadContext)
                artifactResource = mavenRemoteRepository.onDownload(it)
                if (artifactResource != null) {
                    return@mapFirstProxyRepo artifactResource
                } else {}
            }
        }
        return artifactResource
    }

    override fun <R> mapFirstProxyRepo(context: ArtifactContext, action: (ArtifactContext) -> R?): R? {
        val proxyChannelList = getProxyChannelList(context)
        for (setting in proxyChannelList) {
            try {
                action(getContextFromProxyChannel(context, setting))?.let {
                    // 无论请求是否成功, 都会返回kotlin.Unit
                    if (it != Unit) { return it }
                }
            } catch (ignored: Exception) {
                logger.warn("Failed to execute map with channel ${setting.name}", ignored)
            }
        }
        return null
    }
    companion object {
        private val logger = LoggerFactory.getLogger(MavenCompositeRepository::class.java)
    }
}
