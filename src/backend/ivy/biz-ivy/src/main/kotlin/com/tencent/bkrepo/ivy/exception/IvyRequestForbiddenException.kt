package com.tencent.bkrepo.ivy.exception

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.MessageCode

class IvyRequestForbiddenException(messageCode: MessageCode, vararg params: Any) :
ErrorCodeException(messageCode = messageCode, status = HttpStatus.FORBIDDEN, params = params)
