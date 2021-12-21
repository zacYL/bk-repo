package com.tencent.bkrepo.common.cpack.controller

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.cpack.pojo.MailRequest
import com.tencent.bkrepo.common.cpack.pojo.UrlRequest
import com.tencent.bkrepo.common.cpack.service.NotifyService
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/notify")
class NotifyController(
    private val notifyService: NotifyService
) {

    @PostMapping("/mail")
    fun mail(
        @RequestAttribute userId: String,
        @RequestBody urlRequest: UrlRequest
    ): Response<Boolean> {
        notifyService.fileShare(userId, urlRequest.url)
        return ResponseBuilder.success()
    }

    @PostMapping("/mail/user")
    fun mailUser(
        @RequestAttribute userId: String,
        @RequestBody request: MailRequest
    ): Response<Boolean> {
        notifyService.fileShare(userId, request.url, request.users)
        return ResponseBuilder.success()
    }
}
