package com.tencent.bkrepo.auth.controller.user

import com.tencent.bkrepo.auth.service.impl.ExtTokenServiceImpl
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/token")
class ExtTokenController(
    private val extTokenServiceImpl: ExtTokenServiceImpl
) {

    @Principal(PrincipalType.ADMIN)
    @PostMapping("/migrate")
    fun migrate() {
        extTokenServiceImpl.migHistoryTokenData()
    }
}
