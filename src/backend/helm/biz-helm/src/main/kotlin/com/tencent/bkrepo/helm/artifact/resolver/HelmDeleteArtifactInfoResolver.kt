package com.tencent.bkrepo.helm.artifact.resolver

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.resolve.path.ArtifactInfoResolver
import com.tencent.bkrepo.common.artifact.resolve.path.Resolver
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.helm.constants.NAME
import com.tencent.bkrepo.helm.constants.PACKAGE_KEY
import com.tencent.bkrepo.helm.constants.VERSION
import com.tencent.bkrepo.helm.pojo.artifact.HelmDeleteArtifactInfo
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerMapping
import javax.servlet.http.HttpServletRequest

@Component
@Resolver(HelmDeleteArtifactInfo::class)
class HelmDeleteArtifactInfoResolver : ArtifactInfoResolver {
    override fun resolve(
        projectId: String,
        repoName: String,
        artifactUri: String,
        request: HttpServletRequest
    ): ArtifactInfo {
        // 判断是客户端的请求还是页面发送的请求分别进行处理
        val requestURL = request.requestURL
        return when {
            // 页面删除包请求
            requestURL.contains(PACKAGE_DELETE_PREFIX) -> {
                val packageKey = request.getParameter(PACKAGE_KEY)
                HelmDeleteArtifactInfo(projectId, repoName, packageKey)
            }
            // 页面删除包版本请求
            requestURL.contains(PACKAGE_VERSION_DELETE_PREFIX) -> {
                val packageKey = request.getParameter(PACKAGE_KEY)
                val version = request.getParameter(VERSION)
                HelmDeleteArtifactInfo(projectId, repoName, packageKey, version)
            }
            else -> {
                // 客户端请求删除版本
                val attributes = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as Map<*, *>
                val id = attributes[NAME].toString().trim()
                val version = attributes[VERSION].toString().trim()
                HelmDeleteArtifactInfo(projectId, repoName, PackageKeys.ofHelm(id), version)
            }
        }
    }

    companion object {
        private const val PACKAGE_DELETE_PREFIX = "/ext/package/delete/"
        private const val PACKAGE_VERSION_DELETE_PREFIX = "/ext/version/delete/"
    }
}
