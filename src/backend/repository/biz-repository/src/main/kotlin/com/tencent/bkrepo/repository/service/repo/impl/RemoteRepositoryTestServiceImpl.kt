package com.tencent.bkrepo.repository.service.repo.impl

import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.util.AuthenticationUtil
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.repository.pojo.repo.ConnectionStatusInfo
import com.tencent.bkrepo.repository.pojo.repo.RemoteUrlRequest
import com.tencent.bkrepo.repository.service.repo.RemoteRepositoryTestService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.*

@Service
class RemoteRepositoryTestServiceImpl : RemoteRepositoryTestService {
    @Deprecated("plan to be reconstructed")
    override fun testRemoteUrl(remoteUrlRequest: RemoteUrlRequest): ConnectionStatusInfo {
        val remoteRepository = ArtifactContextHolder.getRepository(RepositoryCategory.REMOTE) as RemoteRepository
        val remoteConfiguration = RemoteConfiguration(
            url = remoteUrlRequest.url,
            credentials = remoteUrlRequest.credentials,
            network = remoteUrlRequest.network
        )
        val repoType = remoteUrlRequest.type?.uppercase(Locale.getDefault())
        return try {
            if (repoType == RepositoryType.DOCKER.name || repoType == RepositoryType.OCI.name) {
                val baseUrl = URL(remoteConfiguration.url)
                val v2Url = URL(baseUrl, "/v2/")
                val v2Config = RemoteConfiguration(
                    url = v2Url.toString(),
                    network = remoteUrlRequest.network
                )
                logger.info("oci challenge v2 endpoint: $v2Url")
                val v2Response = remoteRepository.getResponse(v2Config)
                val v2Code = v2Response.code
                val v2Reason = HttpStatus.valueOf(v2Code).reasonPhrase
                if (v2Code == 401) {
                    val wwwAuthenticate = v2Response.header(HttpHeaders.WWW_AUTHENTICATE)
                    val authProperty = AuthenticationUtil.parseWWWAuthenticateHeader(wwwAuthenticate!!, "*")
                    val urlStr =
                        AuthenticationUtil.buildAuthenticationUrl(authProperty!!, remoteUrlRequest.credentials.username)
                    logger.info("oci authentication url: $urlStr")
                    remoteConfiguration.url = urlStr!!
                } else {
                    return ConnectionStatusInfo(v2Response.code < 400, "${v2Response.code} $v2Reason")
                }
            }
            val response = remoteRepository.getResponse(remoteConfiguration)
            val isMavenDirectory = repoType == RepositoryType.MAVEN.name &&
                    remoteUrlRequest.url.contains("maven.aliyun.com") &&
                    response.code == HttpStatus.NOT_FOUND.value &&
                    response.body?.string()?.contains("暂不支持通过仓库URL浏览仓库内容") ?: false
            if (isMavenDirectory) return ConnectionStatusInfo(true, HttpStatus.OK.reasonPhrase)
            val reason = HttpStatus.valueOf(response.code).reasonPhrase
            ConnectionStatusInfo(response.code < 400, "${response.code} $reason")
        } catch (exception: SocketTimeoutException) {
            ConnectionStatusInfo(false, "${HttpStatus.REQUEST_TIMEOUT.value} ${HttpStatus.REQUEST_TIMEOUT.name}")
        } catch (exception: UnknownHostException) {
            ConnectionStatusInfo(false, ("Unknown Host" + exception.message?.let { ": $it" }))
        } catch (exception: Exception) {
            ConnectionStatusInfo(false, exception.message ?: exception.javaClass.simpleName)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteRepositoryTestServiceImpl::class.java)
    }
}
