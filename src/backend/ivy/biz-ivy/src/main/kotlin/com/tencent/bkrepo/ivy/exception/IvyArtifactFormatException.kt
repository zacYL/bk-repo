package com.tencent.bkrepo.ivy.exception

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.MessageCode

class IvyArtifactFormatException(messageCode: MessageCode, vararg params: String) :
    ErrorCodeException(messageCode = messageCode, status = HttpStatus.NOT_ACCEPTABLE, params = params)

