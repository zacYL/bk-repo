package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.docker.api.DockerClient
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.service.artifact.ArtifactClientService
import com.tencent.bkrepo.repository.service.artifact.DockerClientService
import com.tencent.bkrepo.repository.service.artifact.MavenClientService
import com.tencent.bkrepo.repository.service.artifact.NpmClientService

object ArtifactClientServiceFactory {

    private val map = mutableMapOf<PackageType, ArtifactClientService>()

    init {
        map[PackageType.NPM] = SpringContextUtils.getBean(NpmClientService::class.java)
        map[PackageType.MAVEN] = SpringContextUtils.getBean(MavenClientService::class.java)
        map[PackageType.DOCKER] = SpringContextUtils.getBean(DockerClientService::class.java)
        //TODO 其他依赖源
    }

    fun getArtifactClientService(packageType: PackageType): ArtifactClientService {
        return map[packageType]!!
    }
}
