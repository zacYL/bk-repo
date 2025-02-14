package com.tencent.bkrepo.repository.api

import com.tencent.bkrepo.common.api.constant.REPOSITORY_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.repository.pojo.config.ConfigType
import com.tencent.bkrepo.repository.pojo.config.GlobalConfigInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Api(description = "服务-全局配置接口")
@Primary
@FeignClient(REPOSITORY_SERVICE_NAME, contextId = "GlobalConfigClient")
@RequestMapping("/service/config")
interface GlobalConfigClient {
    @ApiOperation("查询配置信息")
    @GetMapping("/info")
    fun getConfig(@RequestParam type: ConfigType): Response<GlobalConfigInfo?>
}
