package com.tencent.bkrepo.ivy.artifact.repository

import com.tencent.bkrepo.common.artifact.repository.composite.CompositeRepository
import com.tencent.bkrepo.common.metadata.service.repo.ProxyChannelService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class IvyCompositeRepository(
    private val ivyLocalRepository: IvyLocalRepository,
    private val ivyRemoteRepository: IvyRemoteRepository,
    proxyChannelService: ProxyChannelService
) : CompositeRepository(ivyLocalRepository, ivyRemoteRepository, proxyChannelService) {

    companion object {
        private val logger = LoggerFactory.getLogger(IvyCompositeRepository::class.java)
    }
}
