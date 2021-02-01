/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.devops.canway.common.lse

import net.canway.license.bean.Result
import net.canway.license.service.LicenseAuthService
import net.canway.license.utils.LicenseProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LseChecker constructor(
        private val licenseAuthService: LicenseAuthService
) {
    private var monitorTh: Thread? = null
    private var vo: Result<Any>? = null
    private var run = true

    init {
        if (monitorTh == null || !monitorTh!!.isAlive) {
            monitorTh = object : Thread() {
                private val sleepTime = 60 * 1000L
                override fun run() {
                    logger.info(LicenseProperties().toString())
                    while (run) {
                        vo = checkCwLseImmediately()
                        try {
                            sleep(sleepTime)
                        } catch (ignored: InterruptedException) {
                            logger.error("InterruptedException happen", ignored)
                        }
                    }
                }
            }
            monitorTh!!.start()
        }
    }

    fun checkLse(): Result<Any> {
        if (vo == null) {
            synchronized(this::class.java) {
                if (vo == null) {
                    vo = checkCwLseImmediately()
                }
            }
        }
        return vo!!
    }

    private fun checkCwLseImmediately(): Result<Any> {
        return licenseAuthService.requestAuth(MODULE_NAME, false)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(LseChecker::class.java)
        const val MODULE_NAME = "LIBRARY"
    }
}
