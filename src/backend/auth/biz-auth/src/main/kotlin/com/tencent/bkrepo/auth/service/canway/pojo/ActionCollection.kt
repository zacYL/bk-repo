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
        PermissionAction.FOLDER_MANAGE
    )

    fun isCanwayAction(action: PermissionAction): Boolean {
        return canwayActions.contains(action)
    }

    fun getActionsByCanway(action: PermissionAction): Set<PermissionAction> {
        return when (action) {
            PermissionAction.REPO_MANAGE -> setOf(PermissionAction.MANAGE)
            PermissionAction.FOLDER_MANAGE -> setOf(PermissionAction.WRITE, PermissionAction.DELETE)
            PermissionAction.ARTIFACT_COPY -> setOf(PermissionAction.WRITE)
            PermissionAction.ARTIFACT_RENAME -> setOf(PermissionAction.UPDATE)
            PermissionAction.ARTIFACT_MOVE -> setOf(PermissionAction.WRITE, PermissionAction.DELETE)
            PermissionAction.ARTIFACT_SHARE -> setOf(PermissionAction.WRITE, PermissionAction.UPDATE)
            PermissionAction.ARTIFACT_DOWNLOAD -> setOf(PermissionAction.UPDATE)
            PermissionAction.ARTIFACT_READWRITE -> setOf(PermissionAction.UPDATE, PermissionAction.READ)
            PermissionAction.ARTIFACT_READ -> setOf(PermissionAction.READ)
            PermissionAction.ARTIFACT_UPDATE -> setOf(PermissionAction.UPDATE)
            else -> setOf(action)
        }
    }
}
