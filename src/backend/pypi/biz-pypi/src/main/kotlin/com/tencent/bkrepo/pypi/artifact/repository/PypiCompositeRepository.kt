package com.tencent.bkrepo.pypi.artifact.repository

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.repository.composite.CompositeRepository
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.pypi.artifact.PypiSimpleArtifactInfo
import com.tencent.bkrepo.pypi.constants.FILE_NAME_REGEX
import com.tencent.bkrepo.pypi.constants.INDENT
import com.tencent.bkrepo.pypi.constants.LINE_BREAK
import com.tencent.bkrepo.pypi.constants.PACKAGE_INDEX_TITLE
import com.tencent.bkrepo.pypi.constants.PSEUDO_MATCH_REGEX
import com.tencent.bkrepo.pypi.constants.REQUIRES_PYTHON_ATTR
import com.tencent.bkrepo.pypi.constants.SELECTOR_ANCHOR
import com.tencent.bkrepo.pypi.constants.SIMPLE_PAGE_CONTENT
import com.tencent.bkrepo.pypi.constants.VERSION_INDEX_TITLE
import com.tencent.bkrepo.pypi.util.HtmlUtils
import com.tencent.bkrepo.repository.api.ProxyChannelClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.util.TreeSet

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

    override fun query(context: ArtifactQueryContext): Any? {
        return when (val artifactInfo = context.artifactInfo) {
            is PypiSimpleArtifactInfo -> {
                val localPage = try {
                    pypiLocalRepository.query(context) as? String
                } catch (e: NotFoundException) { null }
                val remotePage = queryFromProxyRepo(context) as? String
                if (localPage == null && remotePage == null) {
                    throw NotFoundException(
                        ArtifactMessageCode.NODE_NOT_FOUND, artifactInfo.packageName ?: StringPool.SLASH
                    )
                } else if (localPage == null || remotePage == null) {
                    return localPage ?: remotePage
                } else {
                    combineIndex(artifactInfo.packageName, localPage, remotePage)
                }
            }
            else -> pypiLocalRepository.query(context)
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

    private fun combineIndex(
        packageName: String?,
        localIndex: String,
        remoteIndex: String
    ): String {
        val pseudoSelector = if (packageName == null) "" else String.format(PSEUDO_MATCH_REGEX, FILE_NAME_REGEX)
        val localElements = Jsoup.parse(localIndex).body().select(SELECTOR_ANCHOR + pseudoSelector)
        val remoteElements = Jsoup.parse(remoteIndex).body().select(SELECTOR_ANCHOR + pseudoSelector)
        val indexes = listOf(localElements, remoteElements)
            .filter { !it.isNullOrEmpty() }
            .ifEmpty { throw NotFoundException(ArtifactMessageCode.NODE_NOT_FOUND, packageName ?: StringPool.SLASH) }
        val compositeElements = if (indexes.size == 1) indexes.first() else {
            val anchorSet = TreeSet<Element>(compareBy { it.text() })
            indexes.forEach { anchorSet.addAll(it) }
            Elements(anchorSet)
        }
        val content = compositeElements.joinToString("$LINE_BREAK\n$INDENT", INDENT, LINE_BREAK)
        val encodedContent = if (packageName != null) {
            content.replace(Regex("$REQUIRES_PYTHON_ATTR=\"[^\"]*")) {
                HtmlUtils.partialEncode(it.value)
            }
        } else content
        val title = if (packageName == null) PACKAGE_INDEX_TITLE else String.format(VERSION_INDEX_TITLE, packageName)
        return String.format(SIMPLE_PAGE_CONTENT, title, title, encodedContent)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PypiCompositeRepository::class.java)
    }
}
