package com.tencent.bkrepo.nuget.artifact.repository

import com.tencent.bkrepo.common.artifact.repository.composite.CompositeRepository
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.constant.NugetQueryType
import com.tencent.bkrepo.nuget.constant.PACKAGE_NAME
import com.tencent.bkrepo.nuget.constant.QUERY_TYPE
import com.tencent.bkrepo.nuget.pojo.artifact.NugetRegistrationArtifactInfo
import com.tencent.bkrepo.nuget.pojo.v3.metadata.feed.Feed
import com.tencent.bkrepo.nuget.pojo.v3.metadata.index.RegistrationIndex
import com.tencent.bkrepo.nuget.pojo.v3.metadata.leaf.RegistrationLeaf
import com.tencent.bkrepo.nuget.pojo.v3.metadata.page.RegistrationPage
import com.tencent.bkrepo.nuget.util.NugetUtils
import com.tencent.bkrepo.nuget.util.NugetVersionUtils
import com.tencent.bkrepo.nuget.util.RemoteRegistrationUtils
import com.tencent.bkrepo.repository.api.ProxyChannelClient
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
@Primary
class NugetCompositeRepository(
    private val localRepository: NugetLocalRepository,
    private val remoteRepository: NugetRemoteRepository,
    proxyChannelClient: ProxyChannelClient
) : CompositeRepository(localRepository, remoteRepository, proxyChannelClient) {

    override fun query(context: ArtifactQueryContext): Any? {
        return when (context.getAttribute<NugetQueryType>(QUERY_TYPE)!!) {
            NugetQueryType.PACKAGE_VERSIONS -> enumerateVersions(context)
            NugetQueryType.SERVICE_INDEX -> feed(context.artifactInfo as NugetArtifactInfo)
            NugetQueryType.REGISTRATION_INDEX -> registrationIndex(context)
            NugetQueryType.REGISTRATION_PAGE -> registrationPage(context)
            NugetQueryType.REGISTRATION_LEAF -> registrationLeaf(context)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun enumerateVersions(context: ArtifactQueryContext): List<String>? {
        val localQueryResult = localRepository.query(context) as? List<String> ?: emptyList()
        val remoteQueryResult = mapFirstProxyRepo(context) {
            require(it is ArtifactQueryContext)
            remoteRepository.query(it) as? List<String>
        } ?: emptyList()
        return localQueryResult.union(remoteQueryResult).takeIf { it.isNotEmpty() }?.sortedWith {
            v1, v2 -> NugetVersionUtils.compareSemVer(v1, v2)
        }
    }

    private fun feed(artifactInfo: NugetArtifactInfo): Feed {
        return NugetUtils.renderServiceIndex(artifactInfo)
    }

    private fun registrationIndex(context: ArtifactQueryContext): RegistrationIndex? {
        val nugetArtifactInfo = context.artifactInfo as NugetRegistrationArtifactInfo
        val registrationPath = context.getStringAttribute("registrationPath")!!
        val localResult = localRepository.query(context) as? RegistrationIndex
        val remoteResult = mapFirstProxyRepo(context) {
            require(it is ArtifactQueryContext)
            remoteRepository.query(it) as? RegistrationIndex
        }
        val v3BaseUrl = NugetUtils.getV3Url(nugetArtifactInfo)
        val v3RegistrationUrl = "$v3BaseUrl/$registrationPath"
        return if (localResult == null) {
            remoteResult
        } else if (remoteResult == null) {
            localResult
        } else {
            RemoteRegistrationUtils.combineRegistrationIndex(
                localResult, remoteResult, nugetArtifactInfo, v3RegistrationUrl
            )
        }
    }

    private fun registrationPage(context: ArtifactQueryContext): RegistrationPage? {
        val nugetArtifactInfo = context.artifactInfo as NugetRegistrationArtifactInfo
        val registrationPath = context.getStringAttribute("registrationPath")!!
        val localResult = localRepository.query(context) as? RegistrationPage
        val remoteResult = mapFirstProxyRepo(context) {
            require(it is ArtifactQueryContext)
            remoteRepository.query(it) as? RegistrationPage
        }
        val v3BaseUrl = NugetUtils.getV3Url(nugetArtifactInfo)
        val v3RegistrationUrl = "$v3BaseUrl/$registrationPath"
        return if (localResult == null) {
            remoteResult
        } else if (remoteResult == null) {
            localResult
        } else {
            RemoteRegistrationUtils.combineRegistrationPage(
                localResult, remoteResult, nugetArtifactInfo, v3RegistrationUrl
            )
        }
    }

    private fun registrationLeaf(context: ArtifactQueryContext): RegistrationLeaf? {
        return localRepository.query(context) as? RegistrationLeaf
    }
}
