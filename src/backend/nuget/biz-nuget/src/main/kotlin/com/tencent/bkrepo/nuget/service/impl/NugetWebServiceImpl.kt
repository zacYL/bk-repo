package com.tencent.bkrepo.nuget.service.impl

import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.util.toXmlString
import com.tencent.bkrepo.common.artifact.manager.PackageManager
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.common.artifact.util.http.UrlFormatter
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.constant.NugetProperties
import com.tencent.bkrepo.nuget.model.v2.search.NuGetSearchRequest
import com.tencent.bkrepo.nuget.pojo.artifact.NugetDeleteArtifactInfo
import com.tencent.bkrepo.nuget.pojo.artifact.NugetDownloadArtifactInfo
import com.tencent.bkrepo.nuget.pojo.domain.NugetDomainInfo
import com.tencent.bkrepo.nuget.pojo.response.PackageListResponse
import com.tencent.bkrepo.nuget.pojo.user.BasicInfo
import com.tencent.bkrepo.nuget.pojo.user.PackageVersionInfo
import com.tencent.bkrepo.nuget.service.NugetWebService
import com.tencent.bkrepo.nuget.util.NugetUtils
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.packages.PackageListOption
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class NugetWebServiceImpl(
    private val nugetProperties: NugetProperties,
    private val packageManager: PackageManager,
    private val nodeClient: NodeClient,
    private val packageClient: PackageClient
) : NugetWebService, ArtifactService() {

    override fun deletePackage(userId: String, artifactInfo: NugetDeleteArtifactInfo) {
        repository.remove(ArtifactRemoveContext())
    }

    override fun deleteVersion(userId: String, artifactInfo: NugetDeleteArtifactInfo) {
        repository.remove(ArtifactRemoveContext())
    }

    override fun detailVersion(
        artifactInfo: NugetArtifactInfo,
        packageKey: String,
        version: String
    ): PackageVersionInfo {
        return with(artifactInfo) {
            val packageVersion = packageManager.findVersionByName(projectId, repoName, packageKey, version)
            val fullPath = packageVersion.contentPath.orEmpty()
            val nodeDetail = nodeClient.getNodeDetail(projectId, repoName, fullPath).data ?: run {
                logger.warn("node [$fullPath] don't found.")
                throw NotFoundException(ArtifactMessageCode.NODE_NOT_FOUND, fullPath)
            }
            val basicInfo = buildBasicInfo(nodeDetail, packageVersion)
            PackageVersionInfo(basicInfo, packageVersion.packageMetadata)
        }
    }

    override fun getRegistryDomain(): NugetDomainInfo {
        return NugetDomainInfo(UrlFormatter.formatHost(nugetProperties.domain))
    }

    override fun getServiceDocument(artifactInfo: NugetArtifactInfo) {
        val response = HttpContextHolder.getResponse()
        try {
            var serviceDocument = NugetUtils.getServiceDocumentResource()
            serviceDocument = serviceDocument.replace(
                "\$\$baseUrl\$\$",
                HttpContextHolder.getRequest().requestURL.toString()
            )
            response.contentType = MediaTypes.APPLICATION_XML
            response.writer.write(serviceDocument)
        } catch (exception: IOException) {
            logger.error("unable to read resource: $exception")
            throw exception
        }
    }

    override fun download(userId: String, artifactInfo: NugetDownloadArtifactInfo) {
        repository.download(ArtifactDownloadContext())
    }

    override fun findPackagesById(artifactInfo: NugetArtifactInfo, searchRequest: NuGetSearchRequest) {
        val option = PackageListOption(pageSize = 1000, packageName = searchRequest.id)
        val packageList = mutableListOf<PackageSummary>()
        while (true) {
            val packagePageList = packageClient.listPackagePage(
                artifactInfo.projectId,
                artifactInfo.repoName,
                option
            ).data?.records
                .takeUnless { it.isNullOrEmpty() }
                ?: break
            option.pageNumber ++
            packageList.addAll(packagePageList)
        }
        val v3RegistrationUrl = NugetUtils.getV3Url(artifactInfo)
        val resultList = packageList.map { convert(v3RegistrationUrl, it) }
        val response = HttpContextHolder.getResponse()
        response.contentType = MediaTypes.APPLICATION_XML
        response.writer.write(resultList.toXmlString())
    }

    private fun convert(v3RegistrationUrl: String, packageInfo: PackageSummary): PackageListResponse {
        with(packageInfo) {
            return PackageListResponse(
                registrationUrl = NugetUtils.buildRegistrationIndexUrl(v3RegistrationUrl, name),
                packageId = name,
                creator = createdBy,
                description = description ?: "",
                versionsCount = versions,
                latestVersion = latest,
                downloads = downloads
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NugetWebServiceImpl::class.java)

        fun buildBasicInfo(nodeDetail: NodeDetail, packageVersion: PackageVersion): BasicInfo {
            with(nodeDetail) {
                return BasicInfo(
                    packageVersion.name,
                    fullPath,
                    size,
                    sha256!!,
                    md5!!,
                    packageVersion.stageTag,
                    projectId,
                    repoName,
                    packageVersion.downloads,
                    createdBy,
                    createdDate,
                    lastModifiedBy,
                    lastModifiedDate
                )
            }
        }
    }
}
