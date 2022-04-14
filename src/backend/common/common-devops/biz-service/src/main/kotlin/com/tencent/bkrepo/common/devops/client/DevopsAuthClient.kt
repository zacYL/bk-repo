package com.tencent.bkrepo.common.devops.client

import com.tencent.bkrepo.common.devops.DEPLOY_CANWAY
import com.tencent.bkrepo.common.devops.conf.DevopsConf
import com.tencent.bkrepo.common.devops.enums.InstanceType
import com.tencent.bkrepo.common.devops.inter.DevopsAuthInterface
import com.tencent.bkrepo.common.devops.util.http.CertTrustManager
import okhttp3.OkHttpClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Service
@ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = DEPLOY_CANWAY, matchIfMissing = true)
class DevopsAuthClient(
    devopsConf: DevopsConf
) {

    private final val devops = devopsConf.devopsHost

    val devopsApi: DevopsAuthInterface = Retrofit.Builder().baseUrl(devops)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(DevopsAuthInterface::class.java)

    fun isAdmin(userId: String, type: InstanceType, instanceCode: String? = null): Boolean? {
        return devopsApi.isAdmin(userId = userId, type = type.value, instanceCode = instanceCode).execute().body()?.data
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
