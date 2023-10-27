package com.tencent.bkrepo.pypi.artifact.repository

import com.tencent.bkrepo.common.api.exception.BadRequestException
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.repository.composite.CompositeRepository
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.pypi.constants.ELEMENT_SUFFIX
import com.tencent.bkrepo.pypi.constants.PACKAGE_INDEX_TITLE
import com.tencent.bkrepo.pypi.constants.PSEUDO_CONTAIN_TEXT
import com.tencent.bkrepo.pypi.constants.PypiQueryType
import com.tencent.bkrepo.pypi.constants.QUERY_TYPE
import com.tencent.bkrepo.pypi.constants.SELECTOR_A
import com.tencent.bkrepo.pypi.constants.SIMPLE_PAGE_CONTENT
import com.tencent.bkrepo.pypi.constants.VERSION_INDEX_TITLE
import com.tencent.bkrepo.repository.api.ProxyChannelClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.util.*

@Service
@Primary
class PypiCompositeRepository(
    private val pypiLocalRepository: PypiLocalRepository,
    private val pypiRemoteRepository: PypiRemoteRepository,
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

    @Suppress("UNCHECKED_CAST")
    override fun query(context: ArtifactQueryContext): Any? {
        return when (context.getAttribute<PypiQueryType>(QUERY_TYPE)) {
            PypiQueryType.PACKAGE_INDEX,
            PypiQueryType.VERSION_INDEX -> {
                val artifactName = context.artifactInfo.getArtifactName().removePrefix("/")
                val localPage = try {
                    pypiLocalRepository.query(context) as? String
                } catch (e: NotFoundException) { null }
                val remotePage = queryFromProxyRepo(context) as? String
                if (localPage == null && remotePage == null) {
                    throw NotFoundException(ArtifactMessageCode.NODE_NOT_FOUND, artifactName)
                } else if (localPage == null || remotePage == null) {
                    return localPage ?: remotePage
                } else {
                    val pseudoSelector =
                        if (artifactName.isBlank()) "" else String.format(PSEUDO_CONTAIN_TEXT, artifactName)
                    val elementPages = listOf(localPage, remotePage)
                        .map { Jsoup.parse(it).body().select(SELECTOR_A + pseudoSelector) }
                        .takeIf { it.isNotEmpty() }
                        ?: throw NotFoundException(ArtifactMessageCode.NODE_NOT_FOUND, artifactName)
                    val anchorSet = TreeSet<Element>(compareBy { it.text() })
                    elementPages.forEach { anchorSet.addAll(it) }
                    val title = if (artifactName.isBlank()){
                        PACKAGE_INDEX_TITLE
                    } else {
                        String.format(VERSION_INDEX_TITLE, artifactName)
                    }
                    val content = Elements(anchorSet).joinToString(ELEMENT_SUFFIX, postfix = "<br />")
                    return String.format(SIMPLE_PAGE_CONTENT.trimIndent(), title, content)
                }
            }
            PypiQueryType.VERSION_DETAIL -> pypiLocalRepository.query(context)
            null -> throw BadRequestException(CommonMessageCode.REQUEST_CONTENT_INVALID)
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
