package com.tencent.bkrepo.docker.service.impl

import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.docker.service.DockerDeleteService
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DockerDeleteServiceImpl(
    private val packageClient: PackageClient,
    private val nodeClient: NodeClient
) : DockerDeleteService {
    override fun deleteVersion(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String,
        operator: String
    ): Boolean {
        val artifactName = PackageKeys.resolveDocker(packageKey)
        val fullPath = "/$artifactName/$version/"
        val deleteNodeRequest = NodeDeleteRequest(projectId, repoName, fullPath, operator)
        logger.info("docker service delete package version: {$projectId, $repoName, $packageKey, $version}")
        packageClient.deleteVersion(projectId, repoName, packageKey, version)
        logger.info("docker service delete node : { $deleteNodeRequest }")
        nodeClient.deleteNode(deleteNodeRequest)
        return true
    }

    companion object{
        val logger: Logger = LoggerFactory.getLogger(DockerDeleteServiceImpl::class.java)
    }
}