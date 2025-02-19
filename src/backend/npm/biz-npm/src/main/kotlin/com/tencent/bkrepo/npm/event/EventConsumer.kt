package com.tencent.bkrepo.npm.event

import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.event.repo.RepositoryCleanEvent
import com.tencent.bkrepo.common.artifact.util.okhttp.BasicAuthInterceptor
import com.tencent.bkrepo.common.artifact.util.okhttp.HttpClientBuilderFactory
import com.tencent.bkrepo.npm.constants.NPM_REPLICA_RESOLVE
import com.tencent.bkrepo.npm.constants.NPM_REPO_TYPE
import com.tencent.bkrepo.npm.constants.OHPM_REPO_TYPE
import com.tencent.bkrepo.npm.service.ServiceNpmClientService
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

@Component("artifactEvent")
class EventConsumer(
    private val serviceNpmClientService: ServiceNpmClientService,
) : Consumer<ArtifactEvent> {

    override fun accept(event: ArtifactEvent) {
        val type = event.data[RepositoryCleanEvent::packageType.name]
        require(supportRepo.contains(type)) { return }

        when (event.type) {
            EventType.REPOSITORY_CLEAN -> {
                cleanRepository(event)
            }

            EventType.REPLICATION -> {
                resolveReplication(event)
            }

            else -> {
                return
            }
        }
    }

    /**
     * 制品分发，处理package.json的下载链接
     */
    private fun resolveReplication(event: ArtifactEvent) {
        with(event) {
            val domain = event.data["domain"] as? String
                ?: throw IllegalArgumentException("domain not found in event data")
            val httpClient = buildHttpClient()

            val requestBody = RequestBody.create(
                MediaType.parse(MediaTypes.APPLICATION_JSON),
                event.toJsonString()
            )
            val url = "$domain/npm$NPM_REPLICA_RESOLVE"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
            try {
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        logger.info("response:${response.body()!!.string()}")
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun ArtifactEvent.buildHttpClient(): OkHttpClient {
        val httpClient = HttpClientBuilderFactory
            .create()
            .addInterceptor(BasicAuthInterceptor(data["username"] as String, data["password"] as String))
            .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
            .build()
        return httpClient
    }

    private fun EventConsumer.cleanRepository(event: ArtifactEvent) {
        with(event) {
            val versionList = data[RepositoryCleanEvent::versionList.name] as List<String>
            versionList.forEach {
                serviceNpmClientService.deleteVersion(
                    projectId,
                    repoName,
                    data[RepositoryCleanEvent::packageKey.name] as String,
                    it,
                    SYSTEM_USER
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)

        /**
         * 远程请求连接超时时间，单位ms
         */
        const val CONNECT_TIMEOUT: Long = 10 * 1000L

        /**
         * 远程请求读超时时间，单位ms
         */
        const val READ_TIMEOUT: Long = 10 * 1000L

        val supportRepo = listOf(NPM_REPO_TYPE, OHPM_REPO_TYPE)
    }
}
