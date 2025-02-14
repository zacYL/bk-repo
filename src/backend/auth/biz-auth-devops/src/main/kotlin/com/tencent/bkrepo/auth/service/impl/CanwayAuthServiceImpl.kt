package com.tencent.bkrepo.auth.service.impl

import com.tencent.bkrepo.auth.pojo.permission.CanwayBkrepoInstance
import com.tencent.bkrepo.auth.service.CanwayAuthService
import com.tencent.bkrepo.repository.api.RepositoryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CanwayAuthServiceImpl(
    private val repositoryClient: RepositoryClient,
) :CanwayAuthService{
    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayPermissionServiceImpl::class.java)
    }

    override fun instanceld(projectId: String): List<CanwayBkrepoInstance> {
        val repositoryInfolist = repositoryClient.listRepo(projectId).data
        return repositoryInfolist?.map { CanwayBkrepoInstance(instanceId = it.name, instanceName = it.name) }
            ?: listOf()
    }
}
