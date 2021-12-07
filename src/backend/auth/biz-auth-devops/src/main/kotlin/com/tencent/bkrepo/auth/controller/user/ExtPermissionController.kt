package com.tencent.bkrepo.auth.controller.user

import com.tencent.bkrepo.auth.DevopsAuthConfiguration
import com.tencent.bkrepo.auth.service.impl.ExtPermissionServiceImpl
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@ConditionalOnBean(DevopsAuthConfiguration::class)
@RequestMapping("/api/permission")
class ExtPermissionController(
    private val extPermissionServiceImpl: ExtPermissionServiceImpl
) {
    @Principal(PrincipalType.ADMIN)
    @GetMapping("/migrate")
    fun migrate() {
        extPermissionServiceImpl.migHistoryPermissionData()
    }
}
