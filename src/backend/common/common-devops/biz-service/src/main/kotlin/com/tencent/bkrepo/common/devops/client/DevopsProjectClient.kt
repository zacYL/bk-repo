package com.tencent.bkrepo.common.devops.client

import com.tencent.bkrepo.common.devops.DEPLOY_CANWAY
import com.tencent.bkrepo.common.devops.conf.DevopsConf
import com.tencent.bkrepo.common.devops.inter.DevopsProjectInterface
import com.tencent.bkrepo.common.devops.pojo.response.DevopsProject
import com.tencent.bkrepo.common.devops.util.http.CertTrustManager
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import okhttp3.OkHttpClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Service
@ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = DEPLOY_CANWAY, matchIfMissing = true)
class DevopsProjectClient(
    devopsConf: DevopsConf
) {

    private final val devops = devopsConf.devopsHost

    val devopsApi: DevopsProjectInterface = Retrofit.Builder().baseUrl(devops)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(DevopsProjectInterface::class.java)

    fun projects(): List<DevopsProject>? {
        val cookies = HttpContextHolder.getRequest().cookies
        val cookieStr = StringBuilder().apply {
            cookies.forEach {
                append("${it.name}=${it.value};")
            }
        }.toString()
        val body = devopsApi.projects(cookie = cookieStr).execute().body()
        return body?.data
    }

    companion object {
        val okHttpClient: OkHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(
                CertTrustManager.disableValidationSSLSocketFactory,
                CertTrustManager.disableValidationTrustManager
            )
            .hostnameVerifier(CertTrustManager.trustAllHostname)
            .connectTimeout(3L, TimeUnit.SECONDS)
            .readTimeout(5L, TimeUnit.SECONDS)
            .writeTimeout(5L, TimeUnit.SECONDS)
            .build()
    }
}
