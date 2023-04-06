package com.tencent.bkrepo.analyst.controller.user

import com.tencent.bkrepo.analyst.pojo.response.CveWhitelistInfo
import com.tencent.bkrepo.analyst.service.CveWhitelistService
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ListPattern
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import io.swagger.annotations.ApiParam
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.constraints.Min
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@RestController
@Validated
@RequestMapping("/api/cve_whitelist")
class UserCveWhitelistController(
    private val cveWhitelistService: CveWhitelistService
) {
    @GetMapping("/{cveId}")
    fun getByCveId(
        @PathVariable("cveId")
        @Pattern(regexp = "^(CVE)-\\d{4}-\\d{4,}$", message = "cveId格式为CVE-XXXX-XXXX...")
        cveId: String
    ): Response<CveWhitelistInfo?> {
        return ResponseBuilder.success(cveWhitelistService.getByCveId(cveId))
    }

    @PostMapping("/list")
    fun getByCveIds(
        @RequestBody
        @Size(min = 1, max = 200, message = "操作个数必须在1和200之间")
        @ListPattern(regexp = "^(CVE)-\\d{4}-\\d{4,}$", message = "cveId格式为CVE-XXXX-XXXX...")
        cveIds: List<String>
    ): Response<List<CveWhitelistInfo?>> {
        return ResponseBuilder.success(cveWhitelistService.getByCveIds(cveIds))
    }

    @PutMapping("/{cveId}")
    @Principal(PrincipalType.ADMIN)
    fun insert(
        @RequestAttribute("userId") userId: String,
        @PathVariable("cveId")
        @Pattern(regexp = "^(CVE)-\\d{4}-\\d{4,}$", message = "cveId格式为CVE-XXXX-XXXX...")
        cveId: String
    ): Response<Boolean> {
        cveWhitelistService.insert(cveId, userId)
        return ResponseBuilder.success()
    }

    @PutMapping("/batch")
    @Principal(PrincipalType.ADMIN)
    fun insertBatch(
        @RequestAttribute("userId") userId: String,
        @RequestBody
        @Size(min = 1, max = 200, message = "操作个数必须在1和200之间")
        @ListPattern(regexp = "^(CVE)-\\d{4}-\\d{4,}$", message = "cveId格式为CVE-XXXX-XXXX...")
        cveIds: List<String>
    ): Response<Boolean> {
        cveWhitelistService.insertBatch(cveIds, userId)
        return ResponseBuilder.success()
    }

    @DeleteMapping("/{cveId}")
    @Principal(PrincipalType.ADMIN)
    fun deleteByCveId(
        @RequestAttribute("userId") userId: String,
        @PathVariable("cveId")
        @Pattern(regexp = "^(CVE)-\\d{4}-\\d{4,}$", message = "cveId格式为CVE-XXXX-XXXX...")
        cveId: String
    ): Response<Boolean> {
        cveWhitelistService.deleteByCveId(cveId, userId)
        return ResponseBuilder.success()
    }

    @GetMapping("/page")
    fun pageByCveId(
        @RequestParam cveId: String?,
        @ApiParam(value = "当前页", required = false, defaultValue = "0")
        @RequestParam @Min(0, message = "pageNumber起始为0") pageNumber: Int?,
        @ApiParam(value = "分页大小", required = false, defaultValue = "20")
        @RequestParam @Min(1, message = "pageSize分页至少需有一条数据") pageSize: Int?
    ): Response<Page<CveWhitelistInfo>?> {
        return ResponseBuilder.success(cveWhitelistService.searchByCveId(cveId, pageNumber, pageSize))
    }
}
