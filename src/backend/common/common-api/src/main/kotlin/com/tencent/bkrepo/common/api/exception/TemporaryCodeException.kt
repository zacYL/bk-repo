package com.tencent.bkrepo.common.api.exception

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.message.MessageCode

/**
 * error code 异常
 */
open class TemporaryCodeException(
    val messageCode: MessageCode,
    vararg val params: Any,
    val status: HttpStatus = HttpStatus.FORBIDDEN
) : RuntimeException() {
    constructor(
        status: HttpStatus,
        messageCode: MessageCode,
        params: Array<out Any>
    ) : this(messageCode = messageCode, params = *params, status = status)

    override val message: String?
        get() = "[${messageCode.getCode()}]${messageCode.getKey()}"
}
