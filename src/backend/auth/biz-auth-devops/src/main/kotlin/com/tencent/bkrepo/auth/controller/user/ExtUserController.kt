package com.tencent.bkrepo.auth.controller.user

import com.tencent.bkrepo.auth.service.impl.ExtUserServiceImpl
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/user")
class ExtUserController(
    private val extUserServiceImpl: ExtUserServiceImpl
) {
    @Principal(PrincipalType.ADMIN)
    @PostMapping("/migrate")
    fun migrate() {
        extUserServiceImpl.migrateUserToDevOps()
    }
}
