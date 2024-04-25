package com.tencent.bkrepo.common.devops.client

import com.tencent.bkrepo.common.api.constant.DEVOPS_USER_MANAGER_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.devops.pojo.accesstoken.DevopsAccessToken
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Api("Devops Token服务接口")
@Primary
@FeignClient(DEVOPS_USER_MANAGER_SERVICE_NAME, contextId = "ServiceAccessTokenClient")
@RequestMapping("/api/service/token")
interface ServiceAccessTokenClient {

    @ApiOperation("查询token详细信息")
    @GetMapping("/organization/list")
    fun getTokenDetail(@RequestParam personalToken: String): Response<DevopsAccessToken?>
}
