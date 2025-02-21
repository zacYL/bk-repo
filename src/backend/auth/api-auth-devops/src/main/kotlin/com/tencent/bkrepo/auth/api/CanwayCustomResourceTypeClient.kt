package net.canway.devops.auth.api.custom

import com.tencent.bkrepo.common.api.constant.AUTH_HEADER_DEVOPS_UID
import com.tencent.bkrepo.common.api.constant.DEVOPS_AUTH_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import net.canway.devops.auth.pojo.resource.action.ResourceActionVO
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping


@Api("平台权限服务权限接口")
@Primary
@FeignClient(DEVOPS_AUTH_SERVICE_NAME, contextId = "CanwayPermissionClient")
@RequestMapping("/api/service/custom/resource_type")
interface CanwayCustomResourceTypeClient {
    @ApiOperation("查看资源的动作")
    @PostMapping("/resource_action/list")
    fun listResourceAction(
        @RequestHeader(AUTH_HEADER_DEVOPS_UID)
        @ApiParam(value = "用户ID", required = true)
        userId: String,
        @ApiParam(value = "资源标识", required = true)
        @RequestBody
        resourceTypeCode: List<String>,
    ): Response<List<ResourceActionVO>>
}
