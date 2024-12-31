package com.tencent.bkrepo.maven.event

import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.event.repo.RepositoryCleanEvent
import com.tencent.bkrepo.maven.service.MavenDeleteService
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component("artifactEvent")
class EventConsumer (
    private val service: MavenDeleteService
) : Consumer<ArtifactEvent> {
    override fun accept(event: ArtifactEvent) {
        require(event.type == EventType.REPOSITORY_CLEAN) { return }
        val packageType = event.data[RepositoryCleanEvent::packageType.name]
        if (packageType != PackageType.MAVEN.name && packageType != PackageType.GRADLE.name) {
            return
        }
        with(event) {
            val versionList = data[RepositoryCleanEvent::versionList.name] as List<String>
            versionList.forEach {
                service.deleteVersion(
                    projectId,
                    repoName,
                    data[RepositoryCleanEvent::packageKey.name] as String,
                    it,
                    SYSTEM_USER
                )
            }
        }
    }
}
