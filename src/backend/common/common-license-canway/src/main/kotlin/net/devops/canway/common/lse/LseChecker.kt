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
import net.canway.license.service.impl.LicenseAuthServiceImpl
import net.canway.license.utils.Constant
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.DefaultSchemePortResolver
import org.apache.http.ssl.SSLContexts
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class LseChecker{

    private var httpClient: HttpClient
    private var monitorTh: Thread? = null
    private var vo: Result<Any>? = null
    private var run = true

    init {
        // Trust own CA and all self-signed certs
        val sslContext = SSLContexts.custom().loadTrustMaterial(TrustSelfSignedStrategy()).build()
        // Allow SSL protocol only
        val sslsf = SSLConnectionSocketFactory(sslContext, null, null, NoopHostnameVerifier())
        httpClient = HttpClients.custom()
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    .setConnectionRequestTimeout(15000)
                    .setConnectTimeout(15000)
                    .setSocketTimeout(15000).build()
            )
            .setConnectionTimeToLive(180, TimeUnit.SECONDS)
            .setSSLSocketFactory(sslsf)
            .setSchemePortResolver(DefaultSchemePortResolver()).build()

        if (monitorTh == null || !monitorTh!!.isAlive) {
            monitorTh = object : Thread() {

                private val minSleepTime = 60 * 1000L

                private val maxSleepTime = 2 * 3600 * 1000L

                override fun run() {
                    var sleepTime = minSleepTime
                    while (run) {
                        //vo = checkLseImmediately()
                        vo = checkCwLseImmediately(CI_MODULE_NAME)
                        if (vo != null && Constant.SUCCESS == vo!!.code) {
                            // 检查下次要再向LicenceServer通信的时间,根据有效期来定
//                            if (vo!!.validEndTime != null) {
//                                sleepTime = vo!!.validEndTime!!.time - System.currentTimeMillis()
//                            }
                        } else { // 错误情况下，每分钟都会尝试去连接验证Licence，以求最快速度恢复系统服务
                            sleepTime = minSleepTime
                        }

                        if (sleepTime > maxSleepTime) {
                            sleepTime = maxSleepTime
                        }
                        try {
                            sleep(sleepTime)
                        } catch (ignored: InterruptedException) {
                        }
                    }
                }
            }
            monitorTh!!.start()
        }
    }

    fun checkCwLseImmediately(module:String) : Result<Any>{
        val result =  LicenseAuthServiceImpl().requestAuth(module)
        logger.info("$result")
        return result
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(LseChecker::class.java)
        const val CI_MODULE_NAME = "CI"
    }
}
