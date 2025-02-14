package com.tencent.bkrepo.repository.cpack.controller.user

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.cpack.service.CpackRepairService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/repair")
@Principal(PrincipalType.ADMIN)
class UserRepairController(
    private val cpackRepairService: CpackRepairService
) {

    @PostMapping("/repo/virtual-config")
    fun repairVirtualConfiguration(): Response<List<String>> {
        return ResponseBuilder.success(cpackRepairService.repairVirtualConfiguration())
    }
}
