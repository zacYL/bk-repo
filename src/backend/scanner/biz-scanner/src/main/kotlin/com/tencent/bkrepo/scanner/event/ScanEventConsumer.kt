package com.tencent.bkrepo.scanner.event

import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.scanner.pojo.request.AtomScanRequest
import com.tencent.bkrepo.scanner.service.ScanService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.util.function.Consumer

/**
 * 构件事件消费者，用于触发制品更新扫描
 * 制品有新的推送时，筛选已开启自动扫描的方案进行扫描
 * 对应binding name为artifactEvent-in-0
 */
@Component("artifactEvent")
class ScanEventConsumer(
    private val scanService: ScanService
) : Consumer<ArtifactEvent> {

    /**
     * 允许接收的事件类型
     */
    private val acceptTypes = setOf(
        EventType.NODE_CREATED,
        EventType.VERSION_CREATED,
        EventType.VERSION_UPDATED
    )

    override fun accept(event: ArtifactEvent) {
        logger.info("ScanEventConsumer accept:${event.toJsonString()}")
        with(event) {
            if (!acceptTypes.contains(type)) {
                return
            }

            val request = when (type) {
                EventType.NODE_CREATED -> {//GENERIC仓库
                    logger.info("event.resourceKey[${event.resourceKey}]")
                    val artifactName = File(resourceKey).name
                    //只支持ipa/apk类型包
                    if (!artifactName.endsWith(".apk") && !artifactName.endsWith(".ipa")) {
                        return
                    }
                    AtomScanRequest(
                        projectId = projectId,
                        repoName = repoName,
                        repoType = RepositoryType.GENERIC,
                        artifactName = artifactName,
                        fullPath = resourceKey
                    )
                }
                EventType.VERSION_CREATED, EventType.VERSION_UPDATED -> {
                    val packageType = (data["packageType"] as? String).orEmpty()
                    if (packageType != RepositoryType.MAVEN.name) return
                    AtomScanRequest(
                        projectId = projectId,
                        repoName = repoName,
                        repoType = RepositoryType.MAVEN,
                        artifactName = (data["packageName"] as? String).orEmpty(),
                        packageKey = data["packageKey"].toString(),
                        version = data["packageVersion"].toString()
                    )
                }
                else -> throw UnsupportedOperationException()
            }
            scanService.atomScan(userId, request)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScanEventConsumer::class.java)
    }
}
