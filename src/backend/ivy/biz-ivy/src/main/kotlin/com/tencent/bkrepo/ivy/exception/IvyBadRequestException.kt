package com.tencent.bkrepo.ivy.exception

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.MessageCode

class IvyBadRequestException(
    messageCode: MessageCode, vararg params: String
) : ErrorCodeException(messageCode = messageCode, params = params)
