package com.tencent.bkrepo.common.devops.repository.aspect

import com.tencent.bkrepo.common.devops.api.conf.DevopsConf
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class DeployAspectConf(
    val devopsConf: DevopsConf
) {
    @Bean
    fun repositoryAspect() = CanwayRepositoryAspect(devopsConf)
}
