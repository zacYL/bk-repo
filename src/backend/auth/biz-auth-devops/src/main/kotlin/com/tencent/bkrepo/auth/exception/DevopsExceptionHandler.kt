package com.tencent.bkrepo.auth.exception

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
class DevopsExceptionHandler {

    @ExceptionHandler(DevopsRequestException::class)
    @ResponseStatus(HttpStatus.PRECONDITION_FAILED)
    fun handleException(exception: DevopsRequestException): DevopsExceptionResponse {
        logger.error(exception.error)
        logger.error("$exception")
        return DevopsExceptionResponse(HttpStatus.BAD_REQUEST.name, exception.message)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(DevopsExceptionHandler::class.java)
    }
}
