package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.service.artifact.ArtifactRegistryService
import com.tencent.bkrepo.repository.service.artifact.MavenArtifactRegistryService
import com.tencent.bkrepo.repository.service.artifact.NpmArtifactRegistryService

object ArtifactRegistryServiceFactory {

    private val map = mutableMapOf<PackageType, ArtifactRegistryService>()

    init {
        map[PackageType.NPM] = SpringContextUtils.getBean(NpmArtifactRegistryService::class.java)
        map[PackageType.MAVEN] = SpringContextUtils.getBean(MavenArtifactRegistryService::class.java)
    }

    fun getArtifactRegistryService(packageType: PackageType): ArtifactRegistryService {
        return map[packageType]!!
    }
}