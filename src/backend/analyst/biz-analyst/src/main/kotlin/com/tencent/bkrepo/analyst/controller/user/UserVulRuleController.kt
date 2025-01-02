package com.tencent.bkrepo.analyst.controller.user

import com.tencent.bkrepo.analyst.pojo.request.VulRuleCreateRequest
import com.tencent.bkrepo.analyst.pojo.request.VulRuleDeleteRequest
import com.tencent.bkrepo.analyst.pojo.response.VulRuleDeleteResult
import com.tencent.bkrepo.analyst.pojo.response.VulRuleInfo
import com.tencent.bkrepo.analyst.service.VulRuleService
import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_NUMBER
import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_SIZE
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.constraints.Min
import javax.validation.constraints.Size

@RestController
@RequestMapping("/api/vul/rule")
class UserVulRuleController(
    private val vulRuleService: VulRuleService
) {
    @GetMapping("/{vulId}")
    @Principal(PrincipalType.GENERAL)
    fun getByVulId(
        @PathVariable("vulId") vulId: String,
        @RequestParam(required = false) pass: Boolean? = null
    ): Response<VulRuleInfo> {
        return ResponseBuilder.success(vulRuleService.getByVulId(vulId, pass))
    }

    @PostMapping("/list")
    @Principal(PrincipalType.GENERAL)
    fun listByVulIds(
        @RequestBody
        @Size(min = 1, max = 200, message = "操作个数必须在1和200之间")
        vulIds: List<String>
    ): Response<List<VulRuleInfo>> {
        return ResponseBuilder.success(vulRuleService.listByVulIds(vulIds))
    }

    @PostMapping("/create")
    @Principal(PrincipalType.ADMIN)
    fun create(
        @RequestBody request: VulRuleCreateRequest
    ): Response<Void> {
        vulRuleService.create(request)
        return ResponseBuilder.success()
    }

    @DeleteMapping("/delete")
    @Principal(PrincipalType.ADMIN)
    fun delete(
        @RequestBody request: VulRuleDeleteRequest
    ): Response<VulRuleDeleteResult> {
        return ResponseBuilder.success(vulRuleService.delete(request))
    }

    @GetMapping("/page")
    @Principal(PrincipalType.GENERAL)
    fun pageByVulId(
        @ApiParam(value = "当前页", required = false, defaultValue = "0")
        @RequestParam(required = false, defaultValue = "$DEFAULT_PAGE_NUMBER")
        @Min(0, message = "pageNumber起始为0")
        pageNumber: Int = DEFAULT_PAGE_NUMBER,
        @ApiParam(value = "分页大小", required = false, defaultValue = "20")
        @RequestParam(required = false, defaultValue = "$DEFAULT_PAGE_SIZE")
        @Min(1, message = "pageSize分页至少需有一条数据")
        pageSize: Int = DEFAULT_PAGE_SIZE,
        @RequestParam(required = false) vulId: String? = null,
        @RequestParam(required = false) pass: Boolean? = null,
    ): Response<Page<VulRuleInfo>?> {
        return ResponseBuilder.success(vulRuleService.pageByVulId(pageNumber, pageSize, vulId, pass))
    }
}
