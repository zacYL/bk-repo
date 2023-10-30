package com.tencent.bkrepo.oci.listener.consumer

import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.event.repo.RepositoryCleanEvent
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.oci.pojo.artifact.OciArtifactInfo
import com.tencent.bkrepo.oci.service.OciOperationService
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component("artifactEvent")
class RepositoryCleanEventConsumer(
    private val ociOperationService: OciOperationService
) : Consumer<ArtifactEvent> {
    override fun accept(event: ArtifactEvent) {
        require(event.type == EventType.REPOSITORY_CLEAN) { return }
        with(event) {
            val repoType = data[RepositoryCleanEvent::packageType.name]
            require(repoType in listOf(RepositoryType.DOCKER.name, RepositoryType.OCI.name)) { return }
            val versionList = data[RepositoryCleanEvent::versionList.name] as List<String>
            versionList.forEach {
                val packageKey = data[RepositoryCleanEvent::packageKey.name] as String
                val packageName = if (repoType == RepositoryType.DOCKER.name) {
                    PackageKeys.resolveDocker(packageKey)
                } else {
                    PackageKeys.resolveOci(packageKey)
                }
                ociOperationService.deleteVersion(SYSTEM_USER, OciArtifactInfo(projectId, repoName, packageName, it))
            }
        }
    }
}
