package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.service.artifact.*

object ArtifactClientServiceFactory {
    private val map = mutableMapOf<PackageType, ArtifactClientService>()
    init {
        map[PackageType.NPM] = SpringContextUtils.getBean(NpmClientService::class.java)
        map[PackageType.MAVEN] = SpringContextUtils.getBean(MavenClientService::class.java)
        map[PackageType.DOCKER] = SpringContextUtils.getBean(DockerClientService::class.java)
        //TODO 其他依赖源
    }

    fun getArtifactClientService(packageType: PackageType): ArtifactClientService {
        val clientService = map[packageType]
        clientService?.let {
            return clientService
        }?:return SpringContextUtils.getBean(OtherClientService::class.java)

    }
}
