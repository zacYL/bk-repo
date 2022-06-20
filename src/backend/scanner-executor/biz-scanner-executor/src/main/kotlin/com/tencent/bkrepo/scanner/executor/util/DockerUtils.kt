package com.tencent.bkrepo.scanner.executor.util

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.command.WaitContainerResultCallback
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Ulimit
import com.tencent.bkrepo.common.api.exception.SystemErrorException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

object DockerUtils {
    private val logger = LoggerFactory.getLogger(DockerUtils::class.java)

    /**
     * 拉取镜像最大时间
     */
    private const val DEFAULT_PULL_IMAGE_DURATION = 15 * 60 * 1000L

    /**
     * 默认为1024，降低此值可降低容器在CPU时间分配中的优先级
     */
    private const val CONTAINER_CPU_SHARES = 512

    /**
     * 拉取镜像
     */
    fun pullImage(dockerClient: DockerClient, tag: String) {
        val images = dockerClient.listImagesCmd().exec()
        val exists = images.any { image ->
            image.repoTags.any { it == tag }
        }
        if (exists) {
            return
        }
        logger.info("pulling image: $tag")
        val elapsedTime = measureTimeMillis {
            val result = dockerClient
                .pullImageCmd(tag)
                .exec(PullImageResultCallback())
                .awaitCompletion(DEFAULT_PULL_IMAGE_DURATION, TimeUnit.MILLISECONDS)
            if (!result) {
                throw SystemErrorException(CommonMessageCode.SYSTEM_ERROR, "image $tag pull failed")
            }
        }
        logger.info("image $tag pulled, elapse: $elapsedTime")
    }

    fun containerRun(
        dockerClient: DockerClient,
        containerId: String,
        timeout: Long
    ): Boolean {
        dockerClient.startContainerCmd(containerId).exec()
        val resultCallback = WaitContainerResultCallback()
        dockerClient.waitContainerCmd(containerId).exec(resultCallback)
        return resultCallback.awaitCompletion(timeout, TimeUnit.MILLISECONDS)
    }

    fun dockerHostConfig(
        bind: Bind,
        maxTime: Long
    ): HostConfig {
        return HostConfig().apply {
            withBinds(bind)
            withUlimits(arrayOf(Ulimit("fsize", maxTime, maxTime)))
            // 降低容器CPU优先级，限制可用的核心，避免调用DockerDaemon获其他系统服务时超时
            withCpuShares(CONTAINER_CPU_SHARES)
            val processorCount = Runtime.getRuntime().availableProcessors()
            if (processorCount > 2) {
                withCpusetCpus("0-${processorCount - 2}")
            } else if (processorCount == 2) {
                withCpusetCpus("0")
            }
        }
    }

    fun ignoreExceptionExecute(failedMsg: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            logger.warn("$failedMsg, ${e.message}")
        }
    }
}

