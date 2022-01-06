package com.tencent.bkrepo.generic.exception

import com.tencent.bkrepo.common.api.exception.TemporaryCodeException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class GenericExceptionHandler {

    @ExceptionHandler(TemporaryCodeException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleException(exception: TemporaryCodeException): GenericExceptionResponse {
        return GenericExceptionResponse(HttpStatus.FORBIDDEN.toString(), null)
    }

}