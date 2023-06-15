package com.tencent.bkrepo.pypi.artifact.repository

import com.tencent.bkrepo.common.api.constant.StringPool.SLASH
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.artifact.repository.composite.CompositeRepository
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.pypi.artifact.PypiProperties
import com.tencent.bkrepo.pypi.util.HttpUtil.getRedirectUrl
import com.tencent.bkrepo.repository.api.ProxyChannelClient
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class PypiCompositeRepository(
    private val pypiLocalRepository: PypiLocalRepository,
    private val pypiRemoteRepository: PypiRemoteRepository,
    private val pypiProperties: PypiProperties,
    proxyChannelClient: ProxyChannelClient
) : CompositeRepository(pypiLocalRepository, pypiRemoteRepository, proxyChannelClient) {

    @Suppress("TooGenericExceptionCaught")
    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        return try {
            pypiLocalRepository.onDownload(context) ?: downloadFromProxyRepo(context)
        } catch (notFoundException: NotFoundException) {
            // 这里是为了保留各依赖源实现的异常
            downloadFromProxyRepo(context) ?: throw notFoundException
        }
    }

    override fun query(context: ArtifactQueryContext): Any? {
        val request = context.request
        if (!request.servletPath.startsWith("/ext/version/detail") && !request.requestURI.endsWith(SLASH)) {
            val response = HttpContextHolder.getResponse()
            response.sendRedirect(getRedirectUrl(pypiProperties.domain, request.servletPath))
            return null
        }
        return try {
            if (context.artifactInfo.getArtifactFullPath() == "/") {
                queryFromProxyRepo(context) ?: pypiLocalRepository.query(context)
            } else pypiLocalRepository.query(context) ?: queryFromProxyRepo(context)
        } catch (notFoundException: NotFoundException) {
            // 这里是为了保留各依赖源实现的异常
            queryFromProxyRepo(context) ?: throw notFoundException
        }
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

    private fun queryFromProxyRepo(context: ArtifactQueryContext): Any? {
        return mapFirstProxyRepo(context) {
            require(it is ArtifactQueryContext)
            // 这里只会返回空，异常不会抛出
            pypiRemoteRepository.query(it)
        }
    }

    private fun downloadFromProxyRepo(context: ArtifactDownloadContext): ArtifactResource? {
        return mapFirstProxyRepo(context) {
            require(it is ArtifactDownloadContext)
            pypiRemoteRepository.onDownload(it)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PypiCompositeRepository::class.java)
    }
}
