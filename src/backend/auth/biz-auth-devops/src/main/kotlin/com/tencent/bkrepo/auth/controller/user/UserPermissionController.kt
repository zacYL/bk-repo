package com.tencent.bkrepo.auth.controller.user

import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/permission")
class UserPermissionController(
    private val permissionService: PermissionService
) {
    @Principal(PrincipalType.ADMIN)
    @GetMapping("/migrate")
    fun migrate() {
        permissionService.migHistoryPermissionData()
    }
}
