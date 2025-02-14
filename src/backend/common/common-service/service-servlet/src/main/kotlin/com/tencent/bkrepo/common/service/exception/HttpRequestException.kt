package com.tencent.bkrepo.common.service.exception

import okhttp3.Request

class HttpRequestException(request: Request) : RuntimeException("Http request error: ${request.url}")
