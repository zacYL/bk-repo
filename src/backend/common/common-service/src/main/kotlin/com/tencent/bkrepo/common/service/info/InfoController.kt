package com.tencent.bkrepo.common.service.info

import com.tencent.bkrepo.common.service.util.ResponseBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/info")
class InfoController(
    private val infoService: InfoService
) {
    @GetMapping("/release", produces = ["application/json;charset=utf-8"])
    fun releaseInfo() = ResponseBuilder.success(infoService.releaseInfo())
}
