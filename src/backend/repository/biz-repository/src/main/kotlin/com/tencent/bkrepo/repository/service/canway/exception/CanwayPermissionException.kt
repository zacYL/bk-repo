package com.tencent.bkrepo.repository.service.canway.exception

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.StatusCodeException
import com.tencent.bkrepo.repository.service.canway.CANWAY_PERMISSION

open class CanwayPermissionException(message: String = CANWAY_PERMISSION): StatusCodeException(HttpStatus.FORBIDDEN, message)