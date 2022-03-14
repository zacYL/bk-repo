package com.tencent.bkrepo.scanner.config

import com.tencent.bkrepo.common.artifact.config.ArtifactConfigurerSupport
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.artifact.repository.virtual.VirtualRepository
import com.tencent.bkrepo.common.security.http.core.HttpAuthSecurity
import com.tencent.bkrepo.common.security.http.core.HttpAuthSecurityCustomizer
import org.springframework.context.annotation.Configuration

@Configuration
class ExecutorConfigurer : ArtifactConfigurerSupport() {

    override fun getRepositoryType() = RepositoryType.NONE
    override fun getLocalRepository(): LocalRepository = object : LocalRepository() {}
    override fun getRemoteRepository(): RemoteRepository = object : RemoteRepository() {}
    override fun getVirtualRepository(): VirtualRepository = object : VirtualRepository() {}

    override fun getAuthSecurityCustomizer() = object : HttpAuthSecurityCustomizer {
        override fun customize(httpAuthSecurity: HttpAuthSecurity) {
            httpAuthSecurity.withPrefix("/scan")
        }
    }
}
