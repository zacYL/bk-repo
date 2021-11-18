package com.tencent.bkrepo.common.devops.api.exception

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.devops.api.CANWAY_PERMISSION

open class CanwayPermissionException(message: String = CANWAY_PERMISSION) :
    ErrorCodeException(HttpStatus.FORBIDDEN, CommonMessageCode.REQUEST_DENIED, arrayOf(message))
