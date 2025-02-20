package com.tencent.bkrepo.ivy.artifact.repository

import com.tencent.bkrepo.common.artifact.repository.virtual.VirtualRepository
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class IvyCompositeRepository : VirtualRepository() {

    companion object {
        private val logger = LoggerFactory.getLogger(IvyCompositeRepository::class.java)
    }
}
