package com.tencent.bkrepo.helm.service.impl

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.metadata.service.node.NodeService
import com.tencent.bkrepo.common.metadata.service.packages.PackageService
import com.tencent.bkrepo.common.metadata.util.PackageKeys
import com.tencent.bkrepo.helm.service.ServiceHelmClientService
import com.tencent.bkrepo.helm.utils.HelmMetadataUtils
import com.tencent.bkrepo.helm.utils.HelmUtils
import com.tencent.bkrepo.helm.utils.ObjectBuilderUtil
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ServiceHelmClientImpl(
    private val packageService: PackageService,
    private val nodeService: NodeService
) : ServiceHelmClientService {
    override fun deleteVersion(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String,
        operator: String
    ) {
        packageService.findVersionByName(projectId, repoName, packageKey, version)?.let {
            packageService.deleteVersion(projectId, repoName, packageKey, version)
            val name = PackageKeys.resolveHelm(packageKey)
            val chartPath = HelmUtils.getChartFileFullPath(name, version)
            val provPath = HelmUtils.getProvFileFullPath(name, version)
            if (chartPath.isNotBlank()) {
                val request = NodeDeleteRequest(projectId, repoName, chartPath, operator)
                nodeService.deleteNode(request)
            }
            if (provPath.isNotBlank()) {
                nodeService.deleteNode(NodeDeleteRequest(projectId, repoName, provPath, operator))
            }
            updatePackageExtension(projectId,repoName,packageKey)
        }?: logger.warn("[$projectId/$repoName/$packageKey/$version] version not found")
    }

    private fun updatePackageExtension(
        projectId: String,
        repoName: String,
        packageKey: String
    ){
        val name = PackageKeys.resolveHelm(packageKey)
        val version = packageService.findPackageByKey(projectId, repoName, packageKey)?.latest
        val chartPath = HelmUtils.getChartFileFullPath(name, version!!)
        val map = nodeService.getNodeDetail(ArtifactInfo(projectId, repoName, chartPath))?.metadata
        val chartInfo = map?.let { it1 -> HelmMetadataUtils.convertToObject(it1) }
        chartInfo?.appVersion?.let {
            val packageUpdateRequest = ObjectBuilderUtil.buildPackageUpdateRequest(
                projectId,
                repoName,
                name,
                chartInfo.appVersion!!,
                chartInfo.description
            )
            packageService.updatePackage(packageUpdateRequest)
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(HelmOperationService::class.java)
    }
}
