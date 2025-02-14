package com.tencent.bkrepo.repository.listener

import com.tencent.bkrepo.common.artifact.event.packages.VersionCreatedEvent
import com.tencent.bkrepo.common.artifact.event.packages.VersionDownloadEvent
import com.tencent.bkrepo.common.artifact.event.packages.VersionUpdatedEvent
import com.tencent.bkrepo.common.metadata.service.packages.PackageDownloadsService
import com.tencent.bkrepo.repository.dao.PackageUploadsDao
import com.tencent.bkrepo.repository.model.TPackageUploadRecord
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class VersionTransferListener(
    private val packageUploadsDao: PackageUploadsDao,
    private val packageDownloadsService: PackageDownloadsService
) {

    /**
     * 创建包版本时创建或覆盖上传记录到数据库
     */
    @Async
    @EventListener(VersionCreatedEvent::class)
    fun handle(event: VersionCreatedEvent) {
        with(event) {
            saveUploadRecord(projectId, repoName, packageKey, packageName, packageVersion, userId)
        }
    }

    /**
     * 覆盖原有的包版本时创建或覆盖上传记录
     */
    @Async
    @EventListener(VersionUpdatedEvent::class)
    fun handle(event: VersionUpdatedEvent) {
        with(event) {
            saveUploadRecord(projectId, repoName, packageKey, packageName, packageVersion, userId)
        }
    }

    /**
     * 页面操作下载包版本时创建下载记录
     */
    @Async
    @EventListener(VersionDownloadEvent::class)
    fun handle(event: VersionDownloadEvent) {
        with(event) {
            val downloadRecord = PackageDownloadRecord(projectId, repoName, packageKey, packageVersion, userId)
            packageDownloadsService.record(downloadRecord)
        }
    }

    @Suppress("LongParameterList")
    private fun saveUploadRecord(
        projectId: String,
        repoName: String,
        packageKey: String,
        packageName: String,
        packageVersion: String,
        userId: String
    ) {
        val oldRecord = packageUploadsDao.findByVersion(projectId, repoName, packageKey, packageVersion)
        oldRecord?.apply {
            date = LocalDate.now().toString()
            this.userId = userId
        }
        packageUploadsDao.save(
            oldRecord ?: TPackageUploadRecord(
                projectId = projectId,
                repoName = repoName,
                key = packageKey,
                name = packageName,
                version = packageVersion,
                date = LocalDate.now().toString(),
                userId = userId
            )
        )
    }
}
