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

package net.canway.devops.common.lse

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import net.canway.devops.common.lse.feign.LicenseFeign
import net.canway.license.bean.AuthRequest
import net.canway.license.bean.AuthResponse
import net.canway.license.exception.LicenseException
import net.canway.license.service.LicenseAuthService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Scheduled

class LseChecker {
    private var lseCache: AuthResponse? = null

    @Value("\${bk.paas.host:}")
    private val domain: String = ""

    @Autowired
    @Lazy
    private lateinit var licenseFeign: LicenseFeign

    fun checkLse(): AuthResponse {
        if (lseCache == null) {
            lseCache = checkCwLseImmediately()
        }
        return lseCache!!
    }

    @Scheduled(fixedDelay = LICENSE_UPDATE_INTERVAL)
    fun updateLseCache() {
        lseCache = checkCwLseImmediately()
        logger.info("License Cache Updated")
    }

    private fun checkCwLseImmediately(): AuthResponse {
        val authRequest = AuthRequest(domain, MODULE_NAME, System.currentTimeMillis())
        val request = LicenseAuthService.getRequest(authRequest)
        val result = licenseFeign.auth(request)
        val data = result.data()
        if (result.code() != 0 || data.isNullOrEmpty()) {
            logger.warn("License Access Failed")
            throw ErrorCodeException(CommonMessageCode.LICENSE_ACCESS_FAILED)
        }
        return try {
            LicenseAuthService.verify(data)
        } catch (e: LicenseException) {
            logger.error("License Verification Failed: $e")
            throw e
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(LseChecker::class.java)
        const val LICENSE_UPDATE_INTERVAL = 180 * 1000L
        const val MODULE_NAME = "CPack"
    }
}
