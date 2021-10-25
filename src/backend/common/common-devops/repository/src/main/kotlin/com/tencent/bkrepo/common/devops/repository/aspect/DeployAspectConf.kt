package com.tencent.bkrepo.common.devops.repository.aspect

import com.tencent.bkrepo.common.devops.api.conf.DevopsConf
import com.tencent.bkrepo.common.devops.api.conf.CanwayMailConf
import com.tencent.bkrepo.common.devops.api.service.BkUserService
import com.tencent.bkrepo.repository.api.NodeClient
import org.springframework.context.annotation.Bean
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
class DeployAspectConf(
    val devopsConf: DevopsConf,
    val bkUserService: BkUserService,
    val mailSender: JavaMailSender,
    val nodeClient: NodeClient,
    val canwayMailConf: CanwayMailConf
) {

    @Bean
    fun repositoryAspect() = CanwayRepositoryAspect(devopsConf)

    @Bean
    fun shareAspect() = CanwayShareAspect(canwayMailConf, devopsConf, bkUserService, mailSender, nodeClient)
}
