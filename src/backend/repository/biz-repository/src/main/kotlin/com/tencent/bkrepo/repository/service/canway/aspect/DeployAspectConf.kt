package com.tencent.bkrepo.repository.service.canway.aspect

import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.service.canway.bk.BkUserService
import com.tencent.bkrepo.repository.service.canway.conf.CanwayAuthConf
import com.tencent.bkrepo.repository.service.canway.conf.CanwayMailConf
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "deploy", name = ["mode"], havingValue = "devops", matchIfMissing = true)
class DeployAspectConf(
    val canwayAuthConf: CanwayAuthConf,
    val bkUserService: BkUserService,
    val mailSender: JavaMailSender,
    val nodeClient: NodeClient,
    val canwayMailConf: CanwayMailConf
) {
    @Bean
    fun nodeAspect() = CanwayNodeAspect()

    @Bean
    fun repositoryAspect() = CanwayRepositoryAspect(canwayAuthConf)

    @Bean
    fun packageAspect() = CanwayPackageAspect()

    @Bean
    fun shareAspect() = CanwayShareAspect(canwayMailConf, canwayAuthConf, bkUserService, mailSender, nodeClient)
}
