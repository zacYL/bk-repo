package com.tencent.bkrepo.cocoapods.utils

import com.tencent.bkrepo.cocoapods.pojo.CocoapodsRemoteConfiguration
import com.tencent.bkrepo.cocoapods.pojo.enums.RemoteRepoType
import com.tencent.bkrepo.cocoapods.pojo.user.BasicInfo
import com.tencent.bkrepo.common.artifact.event.repo.RepoCreatedEvent
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.pojo.configuration.RepositoryConfiguration
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.apache.commons.lang3.EnumUtils
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter

object ObjectBuildUtil {
    private val logger = LoggerFactory.getLogger(ObjectBuildUtil::class.java)
    fun buildBasicInfo(nodeDetail: NodeDetail, packageVersion: PackageVersion): BasicInfo {
        with(nodeDetail) {
            return BasicInfo(
                version = packageVersion.name,
                fullPath = fullPath,
                size = size,
                sha256 = sha256.orEmpty(),
                md5 = md5.orEmpty(),
                stageTag = packageVersion.stageTag,
                projectId = projectId,
                repoName = repoName,
                downloadCount = packageVersion.downloads,
                createdBy = createdBy,
                createdDate = packageVersion.createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                lastModifiedBy = lastModifiedBy,
                lastModifiedDate = packageVersion.lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME)
            )
        }
    }

    fun RepositoryConfiguration.toCocoapodsRemoteConfiguration(): CocoapodsRemoteConfiguration {
       return CocoapodsRemoteConfiguration(
            type = EnumUtils.getEnum(RemoteRepoType::class.java, this.getStringSetting("type")),
            downloadUrl = this.getStringSetting("downloadUrl")
        )
    }

    /**
     * 仓库创建事件
     */
    fun buildCreatedEvent(
        projectId: String,
        repoName: String,
        operator: String
    ): RepoCreatedEvent {
        return RepoCreatedEvent(
            projectId = projectId,
            repoName = repoName,
            userId = operator,
            repoType = RepositoryType.COCOAPODS
        )
    }
}
