package com.tencent.bkrepo.repository.service.canway.exception

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode

open class CanwayPermissionException(
    reason: String = HttpStatus.FORBIDDEN.reasonPhrase
) : ErrorCodeException(HttpStatus.FORBIDDEN, CommonMessageCode.REQUEST_DENIED, arrayOf(reason))
