package com.tencent.bkrepo.auth.controller.user

import com.tencent.bkrepo.auth.service.impl.ExtPermissionServiceImpl
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/permission")
class ExtPermissionController(
    private val extPermissionServiceImpl: ExtPermissionServiceImpl
) {
    @Principal(PrincipalType.ADMIN)
    @GetMapping("/migrate")
    fun migrate() {
        extPermissionServiceImpl.migHistoryPermissionData()
    }

    @Principal(PrincipalType.ADMIN)
    @PostMapping("/migrateToDevOps")
    fun migrateToDevOps() {
        extPermissionServiceImpl.migrateToDevOps()
    }
}
