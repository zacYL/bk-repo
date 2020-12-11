/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.service.exception

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.exception.StatusCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.log.LoggerHolder.logBusinessException
import com.tencent.bkrepo.common.service.log.LoggerHolder.logSystemException
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.service.util.LocaleMessageUtils
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 全局统一异常处理
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ErrorCodeException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleException(exception: ErrorCodeException): Response<Void> {
        val errorMessage = LocaleMessageUtils.getLocalizedMessage(exception.messageCode, exception.params)
        logBusinessException(exception, "[${exception.messageCode.getCode()}]$errorMessage")
        return ResponseBuilder.fail(exception.messageCode.getCode(), errorMessage)
    }

    @ExceptionHandler(StatusCodeException::class)
    fun handleException(exception: StatusCodeException): Response<Void> {
        logBusinessException(exception)
        HttpContextHolder.getResponse().status = exception.status.value
        return ResponseBuilder.fail(exception.status.value, exception.message)
    }

    /**
     * 参数处理异常
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleException(exception: MissingServletRequestParameterException): Response<Void> {
        val messageCode = CommonMessageCode.PARAMETER_MISSING
        val errorMessage = LocaleMessageUtils.getLocalizedMessage(messageCode, arrayOf(exception.parameterName))
        logBusinessException(exception, "[${messageCode.getCode()}]$errorMessage")
        return ResponseBuilder.fail(messageCode.getCode(), errorMessage)
    }

    /**
     * 参数处理异常
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleException(exception: HttpMessageNotReadableException): Response<Void> {
        val messageCode = CommonMessageCode.REQUEST_CONTENT_INVALID
        val errorMessage = LocaleMessageUtils.getLocalizedMessage(messageCode, null)
        logBusinessException(exception, "[${messageCode.getCode()}]$errorMessage")
        return ResponseBuilder.fail(messageCode.getCode(), errorMessage)
    }

    /**
     * header参数异常
     */
    @ExceptionHandler(MissingRequestHeaderException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleException(exception: MissingRequestHeaderException): Response<Void> {
        val messageCode = CommonMessageCode.HEADER_MISSING
        val errorMessage = LocaleMessageUtils.getLocalizedMessage(messageCode, arrayOf(exception.headerName))
        logBusinessException(exception, "[${messageCode.getCode()}]$errorMessage")
        return ResponseBuilder.fail(messageCode.getCode(), errorMessage)
    }

    /**
     * 参数处理异常
     */
    @ExceptionHandler(MissingKotlinParameterException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleException(exception: MissingKotlinParameterException): Response<Void> {
        val messageCode = CommonMessageCode.PARAMETER_MISSING
        val parameterName = exception.parameter.name.orEmpty()
        val errorMessage = LocaleMessageUtils.getLocalizedMessage(messageCode, arrayOf(parameterName))
        logBusinessException(exception, "[${messageCode.getCode()}]$errorMessage")
        return ResponseBuilder.fail(messageCode.getCode(), errorMessage)
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    fun handleException(exception: HttpRequestMethodNotSupportedException): Response<Void> {
        val messageCode = CommonMessageCode.OPERATION_UNSUPPORTED
        val errorMessage = LocaleMessageUtils.getLocalizedMessage(messageCode, null)
        logBusinessException(exception, "[${messageCode.getCode()}]$errorMessage")
        return ResponseBuilder.fail(messageCode.getCode(), errorMessage)
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    fun handleException(exception: HttpMediaTypeNotSupportedException): Response<Void> {
        val messageCode = CommonMessageCode.MEDIA_TYPE_UNSUPPORTED
        val errorMessage = LocaleMessageUtils.getLocalizedMessage(messageCode, null)
        logBusinessException(exception, "[${messageCode.getCode()}]$errorMessage")
        return ResponseBuilder.fail(messageCode.getCode(), errorMessage)
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleException(exception: Exception): Response<Void> {
        logSystemException(exception)
        val errorMessage = LocaleMessageUtils.getLocalizedMessage(CommonMessageCode.SYSTEM_ERROR, null)
        return ResponseBuilder.fail(CommonMessageCode.SYSTEM_ERROR.getCode(), errorMessage)
    }
}
