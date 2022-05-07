package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.service.artifact.*
import com.tencent.bkrepo.repository.service.repo.impl.RepositoryCleanServiceImpl
import org.slf4j.LoggerFactory

object ArtifactClientServiceFactory {

    private val map = mutableMapOf<PackageType, ArtifactClientService>()
    private val logger = LoggerFactory.getLogger(ArtifactClientServiceFactory::class.java)

    init {
        map[PackageType.NPM] = SpringContextUtils.getBean(NpmClientService::class.java)
        map[PackageType.MAVEN] = SpringContextUtils.getBean(MavenClientService::class.java)
        map[PackageType.DOCKER] = SpringContextUtils.getBean(DockerClientService::class.java)
        //TODO 其他依赖源
    }

    fun getArtifactClientService(packageType: PackageType): ArtifactClientService {
        //TODO 删除日志
        logger.info("get client service type is [$packageType]")
        val clientService = map[packageType]
        clientService?.let {
            logger.info("client service is not null")
            return clientService
        }?:return SpringContextUtils.getBean(OtherClientService::class.java)

    }
}
