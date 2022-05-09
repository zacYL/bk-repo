package com.tencent.bkrepo.common.devops.client

import com.tencent.bkrepo.common.devops.DEPLOY_CANWAY
import com.tencent.bkrepo.common.devops.conf.DevopsConf
import com.tencent.bkrepo.common.devops.enums.InstanceType
import com.tencent.bkrepo.common.devops.inter.DevopsInterface
import com.tencent.bkrepo.common.devops.pojo.CanwayGroup
import com.tencent.bkrepo.common.devops.pojo.DevopsDepartment
import com.tencent.bkrepo.common.devops.util.http.CertTrustManager
import okhttp3.OkHttpClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Service
@ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = DEPLOY_CANWAY, matchIfMissing = true)
class DevopsClient(
    devopsConf: DevopsConf
) {

    private final val devops = devopsConf.devopsHost

    val devopsApi: DevopsInterface = Retrofit.Builder().baseUrl(devops)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(DevopsInterface::class.java)

    /**
     * 加载CI 下用户有权限的项目列表，用户信息由Cookie传入
     * @param type 2：项目，1：需求池
     */
    fun isAdmin(userId: String, type: InstanceType, instanceCode: String? = null): Boolean? {
        return devopsApi.isAdmin(userId = userId, type = type.value, instanceCode = instanceCode).execute().body()?.data
    }

    /**
     * 获取用户所属组织包括父级
     * [userId] 用户
     */
    fun departmentsByUserId(userId: String): List<DevopsDepartment>? {
        return devopsApi.departmentsByUserId(userId = userId).execute().body()?.data
    }

    /**
     * 查询项目下所有用户组
     * [projectId] 项目
     */
    fun groupsByProjectId(projectId: String): List<CanwayGroup>? {
        return devopsApi.groupsByProjectId(projectId = projectId).execute().body()?.data
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
