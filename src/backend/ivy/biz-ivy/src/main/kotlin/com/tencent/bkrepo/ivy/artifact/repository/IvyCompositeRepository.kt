package com.tencent.bkrepo.ivy.artifact.repository

import com.tencent.bkrepo.common.artifact.repository.composite.CompositeRepository
import com.tencent.bkrepo.common.metadata.service.repo.ProxyChannelService
import com.tencent.bkrepo.repository.api.ProxyChannelClient
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class IvyCompositeRepository(
    private val mavenLocalRepository: IvyLocalRepository,
    private val mavenRemoteRepository: IvyRemoteRepository,
    proxyChannelService: ProxyChannelService
) : CompositeRepository(mavenLocalRepository, mavenRemoteRepository, proxyChannelService) {

    companion object {
        private val logger = LoggerFactory.getLogger(IvyCompositeRepository::class.java)
    }
}
