package com.tencent.bkrepo.auth.service.impl

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.CanwayAction
import org.springframework.stereotype.Service

@Service
class ActionService {
    fun listActions(repo: String): Set<CanwayAction> {
        return when (repo) {
            "custom" -> setOf(
                CanwayAction(PermissionAction.REPO_MANAGE, "仓库管理"),
                CanwayAction(PermissionAction.FOLDER_MANAGE, "目录管理"),
                CanwayAction(PermissionAction.ARTIFACT_COPY, "制品复制"),
                CanwayAction(PermissionAction.ARTIFACT_RENAME, "重命名"),
                CanwayAction(PermissionAction.ARTIFACT_MOVE, "制品移动"),
                CanwayAction(PermissionAction.ARTIFACT_SHARE, "制品分享"),
                CanwayAction(PermissionAction.ARTIFACT_DOWNLOAD, "制品下载"),
                CanwayAction(PermissionAction.ARTIFACT_READWRITE, "制品上传"),
                CanwayAction(PermissionAction.ARTIFACT_READ, "制品查看"),
                CanwayAction(PermissionAction.ARTIFACT_DELETE, "制品删除")
            )
            "pipeline" -> setOf(
                CanwayAction(PermissionAction.REPO_MANAGE, "仓库管理"),
                CanwayAction(PermissionAction.ARTIFACT_READ, "制品查看"),
                CanwayAction(PermissionAction.ARTIFACT_SHARE, "制品分享"),
                CanwayAction(PermissionAction.ARTIFACT_DOWNLOAD, "制品下载")
            )
            else -> setOf(
                CanwayAction(PermissionAction.REPO_MANAGE, "仓库管理"),
                CanwayAction(PermissionAction.ARTIFACT_UPDATE, "制品晋级"),
                CanwayAction(PermissionAction.ARTIFACT_READWRITE, "制品上传"),
                CanwayAction(PermissionAction.ARTIFACT_READ, "制品查看"),
                CanwayAction(PermissionAction.ARTIFACT_DELETE, "制品删除"),
                CanwayAction(PermissionAction.ARTIFACT_DOWNLOAD, "制品下载")
            )
        }
    }
}
