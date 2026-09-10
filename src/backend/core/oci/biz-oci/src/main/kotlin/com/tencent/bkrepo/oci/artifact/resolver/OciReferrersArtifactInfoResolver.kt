package com.tencent.bkrepo.oci.artifact.resolver

import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.resolve.path.ArtifactInfoResolver
import com.tencent.bkrepo.common.artifact.resolve.path.Resolver
import com.tencent.bkrepo.oci.pojo.artifact.OciArtifactInfo.Companion.REFERRERS_SUFFIX
import com.tencent.bkrepo.oci.pojo.artifact.OciReferrersArtifactInfo
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerMapping

@Component
@Resolver(OciReferrersArtifactInfo::class)
class OciReferrersArtifactInfoResolver : ArtifactInfoResolver {

    override fun resolve(
        projectId: String,
        repoName: String,
        artifactUri: String,
        request: HttpServletRequest
    ): ArtifactInfo {
        val requestUrl = ArtifactContextHolder.getUrlPath(this.javaClass.name)!!
        val packageName = requestUrl.substringBeforeLast(REFERRERS_SUFFIX)
            .removePrefix("/v2/$projectId/$repoName/")
        validate(packageName)
        val attributes = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as Map<*, *>
        val digest = attributes["digest"]?.toString()?.trim().orEmpty()
        return OciReferrersArtifactInfo(projectId, repoName, packageName, digest)
    }

    private fun validate(packageName: String) {
        Preconditions.checkNotBlank(packageName, "packageName")
        Preconditions.matchPattern(packageName, PACKAGE_NAME_PATTERN, "package name [$packageName] invalid")
    }

    companion object {
        const val PACKAGE_NAME_PATTERN = "[a-z0-9]+([._-][a-z0-9]+)*(/[a-z0-9]+([._-][a-z0-9]+)*)*"
    }
}
