package com.tencent.bkrepo.auth.service.canway.pojo

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction

object ActionCollection {
    private val canwayActions = setOf(
        PermissionAction.ARTIFACT_COPY,
        PermissionAction.ARTIFACT_DOWNLOAD,
        PermissionAction.ARTIFACT_MOVE,
        PermissionAction.ARTIFACT_READ,
        PermissionAction.ARTIFACT_READWRITE,
        PermissionAction.ARTIFACT_RENAME,
        PermissionAction.ARTIFACT_SHARE,
        PermissionAction.ARTIFACT_UPDATE,
        PermissionAction.REPO_MANAGE,
        PermissionAction.FOLDER_MANAGE,
        PermissionAction.ARTIFACT_DELETE
    )

    fun isCanwayAction(action: PermissionAction): Boolean {
        return canwayActions.contains(action)
    }

    fun getActionsByCanway(action: PermissionAction): Set<PermissionAction> {
        return when (action) {
            PermissionAction.REPO_MANAGE -> setOf(PermissionAction.REPO_MANAGE, PermissionAction.MANAGE)
            PermissionAction.FOLDER_MANAGE -> setOf(PermissionAction.FOLDER_MANAGE, PermissionAction.WRITE, PermissionAction.DELETE)
            PermissionAction.ARTIFACT_COPY -> setOf(PermissionAction.ARTIFACT_COPY, PermissionAction.WRITE)
            PermissionAction.ARTIFACT_RENAME -> setOf(PermissionAction.ARTIFACT_RENAME, PermissionAction.UPDATE)
            PermissionAction.ARTIFACT_MOVE -> setOf(PermissionAction.ARTIFACT_MOVE, PermissionAction.WRITE, PermissionAction.DELETE)
            PermissionAction.ARTIFACT_SHARE -> setOf(PermissionAction.ARTIFACT_SHARE, PermissionAction.WRITE, PermissionAction.UPDATE)
            PermissionAction.ARTIFACT_DOWNLOAD -> setOf(PermissionAction.ARTIFACT_DOWNLOAD, PermissionAction.UPDATE)
            PermissionAction.ARTIFACT_READWRITE -> setOf(PermissionAction.ARTIFACT_READWRITE, PermissionAction.UPDATE, PermissionAction.READ)
            PermissionAction.ARTIFACT_READ -> setOf(PermissionAction.ARTIFACT_READ, PermissionAction.READ)
            PermissionAction.ARTIFACT_UPDATE -> setOf(PermissionAction.ARTIFACT_UPDATE, PermissionAction.UPDATE)
            PermissionAction.ARTIFACT_DELETE -> setOf(PermissionAction.ARTIFACT_DELETE, PermissionAction.DELETE)
            else -> setOf(action)
        }
    }
}
