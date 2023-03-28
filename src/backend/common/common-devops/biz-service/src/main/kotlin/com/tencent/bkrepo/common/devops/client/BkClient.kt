package com.tencent.bkrepo.common.devops.client

import com.tencent.bkrepo.common.devops.DEPLOY_CANWAY
import com.tencent.bkrepo.common.devops.conf.DevopsConf
import com.tencent.bkrepo.common.devops.inter.BkInterface
import com.tencent.bkrepo.common.devops.pojo.BkChildrenDepartment
import com.tencent.bkrepo.common.devops.pojo.BkDepartmentUser
import com.tencent.bkrepo.common.devops.util.http.CertTrustManager
import okhttp3.OkHttpClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Service
@ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = DEPLOY_CANWAY, matchIfMissing = true)
class BkClient(
    private val devopsConf: DevopsConf
) {
    private final val bkHost = devopsConf.bkHost

    private final val bkInterface: BkInterface = Retrofit.Builder()
        .baseUrl(bkHost)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(BkInterface::class.java)

    fun listDepartments(
        bkUsername: String? = null,
        bkToken: String? = null,
        lookupField: String? = null,
        fields: String? = null,
        exactLookups: String? = null,
        fuzzyLookups: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): List<BkChildrenDepartment>? {
        require(!(bkUsername == null && bkToken == null)) {
            ("username and cookie.bk_token can't all be null ")
        }
        return bkInterface.listDepartments(
            bkAppCode = devopsConf.appCode,
            bkAppSecret = devopsConf.appSecret,
            bkUsername = bkUsername,
            bkToken = bkToken,
            lookupField = lookupField,
            fields = fields,
            exactLookups = exactLookups,
            fuzzyLookups = fuzzyLookups,
            page = page,
            pageSize = pageSize
        ).execute().body()?.data?.results
    }

    fun listDepartmentProfiles(
        bkUsername: String? = null,
        bkToken: String? = null,
        id: String? = null,
        lookupField: String? = null,
        recursive: Boolean? = null
    ): List<BkDepartmentUser>? {
        require(!(bkUsername == null && bkToken == null)) {
            ("username and cookie.bk_token can't all be null ")
        }
        return bkInterface.listDepartmentProfiles(
            bkAppCode = devopsConf.appCode,
            bkAppSecret = devopsConf.appSecret,
            bkUsername = bkUsername,
            bkToken = bkToken,
            id = id,
            lookupField = lookupField,
            recursive = recursive
        ).execute().body()?.data?.results
    }

    fun allDepartmentIds(
        bkUsername: String? = null,
        bkToken: String? = null
    ): List<Int> {
        require(!(bkUsername == null && bkToken == null)) {
            ("username and cookie.bk_token can't all be null ")
        }
        return bkInterface.allDepartmentIds(
            bkAppCode = devopsConf.appCode,
            bkAppSecret = devopsConf.appSecret,
            bkUsername = bkUsername,
            bkToken = bkToken,
            fields = "id"
        ).execute().body()?.data?.results?.map { it.id } ?: emptyList()
    }

    companion object {
        val okHttpClient: OkHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(
                CertTrustManager.disableValidationSSLSocketFactory,
                CertTrustManager.disableValidationTrustManager
            )
            .retryOnConnectionFailure(true)
            .hostnameVerifier(CertTrustManager.trustAllHostname)
            .connectTimeout(10L, TimeUnit.SECONDS)
            .readTimeout(20L, TimeUnit.SECONDS)
            .writeTimeout(20L, TimeUnit.SECONDS)
            .build()
    }
}
