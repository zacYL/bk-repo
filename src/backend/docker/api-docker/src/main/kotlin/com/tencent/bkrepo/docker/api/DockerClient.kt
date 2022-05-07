package com.tencent.bkrepo.docker.api

import com.tencent.bkrepo.common.api.constant.DOCKER_SERVICE_NAME
import io.swagger.annotations.ApiOperation
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
@Primary
@ApiOperation("docker service delete api")
@FeignClient(DOCKER_SERVICE_NAME, contextId = "dockerClient")
@RequestMapping("/service")
interface DockerClient {

    @DeleteMapping("/{projectId}/{repoName}")
    fun deleteVersion(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam packageKey: String,
        @RequestParam version: String,
        @RequestParam operator: String
    ): Boolean

}
