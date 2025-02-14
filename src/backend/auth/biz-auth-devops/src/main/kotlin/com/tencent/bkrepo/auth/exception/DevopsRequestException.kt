package com.tencent.bkrepo.auth.exception

class DevopsRequestException(val error: String) : RuntimeException(error)
