package com.tencent.bkrepo.scanner.service.container

import com.github.dockerjava.api.command.WaitContainerResultCallback
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Binds
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Volume
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.okhttp.OkDockerHttpClient
import com.tencent.bkrepo.scanner.config.container.ContainerTaskConfig
import com.tencent.bkrepo.scanner.exception.CreateContainerFailedException
import com.tencent.bkrepo.scanner.exception.RunContainerFailedException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DockerRunTime @Autowired constructor(val config: ContainerTaskConfig) {

    private val dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerConfig(config.dockerHost)
        .withApiVersion(config.apiVerion)
        .build()

    private val longHttpClient = OkDockerHttpClient.Builder()
        .dockerHost(dockerConfig.dockerHost)
        .sslConfig(dockerConfig.sslConfig)
        .connectTimeout(5000000)
        .readTimeout(30000000)
        .build()

    private val httpDockerCli = DockerClientBuilder
        .getInstance(dockerConfig)
        .withDockerHttpClient(longHttpClient)
        .build()

    fun stopContainer(containerId: String) {
        httpDockerCli.killContainerCmd(containerId).exec()
    }

    fun createContainer(workDir: String): String {
        try {
            val bind = Volume(config.containerDir)
            val binds = Binds(Bind(workDir, bind))
            val containerId = httpDockerCli.createContainerCmd(config.imageName)
                .withHostConfig(HostConfig().withBinds(binds))
                .withCmd(config.args)//容器工作空间路径
                .withTty(true)
                .withStdinOpen(true)
                .exec().id
            logger.info("run container instance Id [$workDir, $containerId]")
            return containerId
        } catch (e: Exception) {
            logger.error("create docker container exception[$workDir, $e]")
            throw CreateContainerFailedException("create docker container exception")
        }
    }

    /**
     * 调起容器执行一次性任务
     * @param workDir  工作目录
     * @throws RunContainerFailedException
     */
    fun runContainerOnce(workDir: String, containerId: String) {
        try {
            httpDockerCli.startContainerCmd(containerId).exec()
            val resultCallback = WaitContainerResultCallback()
            httpDockerCli.waitContainerCmd(containerId).exec(resultCallback)
            resultCallback.awaitCompletion()
            logger.info("task docker run success [$workDir, $containerId]")
        } catch (e: Exception) {
            logger.warn("exec docker task exception[$workDir, $e]")
            throw RunContainerFailedException("exec docker task exception")
        } finally {
            httpDockerCli.removeContainerCmd(containerId).withForce(true).exec()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DockerRunTime::class.java)
    }
}
