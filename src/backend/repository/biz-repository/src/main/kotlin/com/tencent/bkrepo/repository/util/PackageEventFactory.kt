package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.common.artifact.event.packages.VersionCreatedEvent
import com.tencent.bkrepo.common.artifact.event.packages.VersionDeletedEvent
import com.tencent.bkrepo.common.artifact.event.packages.VersionDownloadEvent
import com.tencent.bkrepo.common.artifact.event.packages.VersionUpdatedEvent
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.request.PackageUpdateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionUpdateRequest

/**
 * 包版本事件构造类
 */
object PackageEventFactory {

    /**
     * 包版本创建事件-upload 新包
     */
    fun buildCreatedEvent(request: PackageVersionCreateRequest): VersionCreatedEvent {
        with(request) {
            return VersionCreatedEvent(
                projectId = projectId,
                repoName = repoName,
                packageType = packageType.name,
                packageKey = packageKey,
                packageName = packageName,
                packageVersion = versionName,
                userId = createdBy
            )
        }
    }

    /**
     * 包版本更新事件-upload 覆盖上传
     */
    fun buildUpdatedEvent(request: PackageVersionCreateRequest): VersionUpdatedEvent {
        with(request) {
            return VersionUpdatedEvent(
                projectId = projectId,
                repoName = repoName,
                packageType = packageType.name,
                packageKey = packageKey,
                packageName = packageName,
                packageVersion = versionName,
                userId = createdBy
            )
        }
    }

    fun buildUpdatedEvent(
        request: PackageVersionUpdateRequest,
        packageType: String,
        packageName: String,
        createdBy: String
    ): VersionUpdatedEvent {
        with(request) {
            return VersionUpdatedEvent(
                projectId = projectId,
                repoName = repoName,
                packageType = packageType,
                packageKey = packageKey,
                packageName = packageName,
                packageVersion = versionName,
                userId = createdBy
            )
        }
    }

    fun buildUpdatedEvent(
        request: PackageUpdateRequest,
        packageType: String,
        packageName: String,
        createdBy: String
    ): VersionUpdatedEvent {
        with(request) {
            return VersionUpdatedEvent(
                projectId = projectId,
                repoName = repoName,
                packageType = packageType,
                packageKey = packageKey,
                packageName = packageName,
                packageVersion = null,
                userId = createdBy
            )
        }
    }

    /**
     * 包版本下载事件
     */
    fun buildDownloadEvent(
        projectId: String,
        repoName: String,
        packageType: PackageType,
        packageKey: String,
        packageName: String,
        versionName: String,
        createdBy: String
    ): VersionDownloadEvent {
        return VersionDownloadEvent(
            projectId = projectId,
            repoName = repoName,
            packageType = packageType.name,
            packageKey = packageKey,
            packageName = packageName,
            packageVersion = versionName,
            userId = createdBy
        )
    }

    /**
     * 包版本下载事件
     */
    fun buildDeletedEvent(
        projectId: String,
        repoName: String,
        packageType: PackageType,
        packageKey: String,
        packageName: String,
        versionName: String?,
        createdBy: String
    ): VersionDeletedEvent {
        return VersionDeletedEvent(
            projectId = projectId,
            repoName = repoName,
            packageType = packageType.name,
            packageKey = packageKey,
            packageName = packageName,
            packageVersion = versionName,
            userId = createdBy
        )
    }
}
