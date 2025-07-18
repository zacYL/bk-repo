/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2023 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.ddc.utils

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.service.util.LocaleMessageUtils
import com.tencent.bkrepo.ddc.model.TDdcBlob
import com.tencent.bkrepo.ddc.model.TDdcRef
import com.tencent.bkrepo.ddc.serialization.CbObject
import org.slf4j.LoggerFactory

object DdcUtils {
    private val logger = LoggerFactory.getLogger(DdcUtils::class.java)

    const val DIR_BLOBS = "blobs"

    fun TDdcRef.fullPath() = "/$bucket/$key"

    fun TDdcBlob.fullPath() = "/blobs/$blobId"

    fun buildRef(bucket: String, key: String): String = "ref/$bucket/$key"

    fun toError(e: Exception): Pair<CbObject, Int> {
        val statusCode = if (e is ErrorCodeException) {
            e.status.value
        } else {
            HttpStatus.INTERNAL_SERVER_ERROR.value
        }
        val msg = if (e is ErrorCodeException) {
            LocaleMessageUtils.getLocalizedMessage(e.messageCode, e.params)
        } else {
            HttpStatus.INTERNAL_SERVER_ERROR.name
        }
        return toError(e, statusCode, msg)
    }

    fun toError(e: Exception, statusCode: Int, msg: String): Pair<CbObject, Int> {
        if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value) {
            logger.error("batch op failed:\n${e.stackTraceToString()}")
        } else if (statusCode != HttpStatus.NOT_FOUND.value && statusCode != HttpStatus.BAD_REQUEST.value) {
            logger.info("batch op failed:\n${e.stackTraceToString()}")
        }
        val obj = CbObject.build {
            it.writeString("title", msg)
            it.writeInteger("status", statusCode)
        }
        return Pair(obj, statusCode)
    }
}
