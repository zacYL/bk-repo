package com.tencent.bkrepo.repository.service.artifact

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DefaultClientService : ArtifactClientService {
    override fun deleteVersion(projectId: String, repoName: String, packageKey: String, version: String) {
        logger.info("projectId:[$projectId}] repoName:[$repoName}] The current repository does not support")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefaultClientService::class.java)
    }
}
