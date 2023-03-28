package com.tencent.bkrepo.pypi.artifact.repository

import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.artifact.repository.composite.CompositeRepository
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.repository.api.ProxyChannelClient
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class PypiCompositeRepository(
    private val pypiLocalRepository: PypiLocalRepository,
    private val pypiRemoteRepository: PypiRemoteRepository,
    proxyChannelClient: ProxyChannelClient
) : CompositeRepository(pypiLocalRepository, pypiRemoteRepository, proxyChannelClient) {

    @Suppress("TooGenericExceptionCaught")
    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        var artifactResource: ArtifactResource? = null
        try {
            artifactResource = pypiLocalRepository.onDownload(context)
        } catch (notFoundException: NotFoundException) {
            mapFirstProxyRepo(context) {
                require(it is ArtifactDownloadContext)
                // 这里只会返回空，异常不会抛出
                artifactResource = pypiRemoteRepository.onDownload(it)
                if (artifactResource != null) { return@mapFirstProxyRepo artifactResource } else { }
            }
            // 这里是为了保留各依赖源实现的异常
            if (artifactResource == null) {
                throw notFoundException
            }
        }
        if (artifactResource == null) {
            mapFirstProxyRepo(context) {
                require(it is ArtifactDownloadContext)
                artifactResource = pypiRemoteRepository.onDownload(it)
                if (artifactResource != null) { return@mapFirstProxyRepo artifactResource } else { }
            }
        }
        return artifactResource
    }

    override fun query(context: ArtifactQueryContext): Any? {
        var result: Any? = null
        try {
            result = pypiLocalRepository.query(context)
        } catch (notFoundException: NotFoundException) {
            mapFirstProxyRepo(context) {
                require(it is ArtifactQueryContext)
                // 这里只会返回空，异常不会抛出
                result = pypiRemoteRepository.query(it)
                if (result != null) { return@mapFirstProxyRepo result } else { }
            }
            // 这里是为了保留各依赖源实现的异常
            if (result == null) {
                throw notFoundException
            }
        }
        if (result == null) {
            mapFirstProxyRepo(context) {
                require(it is ArtifactQueryContext)
                result = pypiRemoteRepository.query(it)
                if (result != null) { return@mapFirstProxyRepo result } else { }
            }
        }
        return result
    }

    override fun <R> mapFirstProxyRepo(context: ArtifactContext, action: (ArtifactContext) -> R?): R? {
        val proxyChannelList = getProxyChannelList(context)
        for (setting in proxyChannelList) {
            try {
                action(getContextFromProxyChannel(context, setting))?.let {
                    // 无论请求是否成功, 都会返回kotlin.Unit
                    if (it != Unit && it != "") {
                        return it
                    }
                }
            } catch (ignored: Exception) {
                logger.warn("Failed to execute map with channel ${setting.name}", ignored)
            }
        }
        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PypiCompositeRepository::class.java)
    }
}
