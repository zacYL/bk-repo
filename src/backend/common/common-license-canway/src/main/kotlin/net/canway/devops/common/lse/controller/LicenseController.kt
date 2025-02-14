package net.canway.devops.common.lse.controller

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import io.swagger.annotations.ApiOperation
import net.canway.devops.common.lse.pojo.LicenseInfo
import net.canway.devops.common.lse.service.LicenseService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/license")
class LicenseController(
    private val licenseService: LicenseService
) {

    @ApiOperation("查询许可信息")
    @GetMapping
    fun getLicense(): Response<LicenseInfo> {
        return ResponseBuilder.success(licenseService.getLicense())
    }
}
