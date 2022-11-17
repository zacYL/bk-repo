package com.tencent.bkrepo.common.devops.client

import com.tencent.bkrepo.common.devops.DEPLOY_CANWAY
import com.tencent.bkrepo.common.devops.conf.DevopsConf
import com.tencent.bkrepo.common.devops.enums.InstanceType
import com.tencent.bkrepo.common.devops.inter.DevopsInterface
import com.tencent.bkrepo.common.devops.pojo.CanwayGroup
import com.tencent.bkrepo.common.devops.pojo.DevopsDepartment
import com.tencent.bkrepo.common.devops.pojo.response.CanwayUser
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

    /**
     * 查询用户在当前租户下的部门
     * @param userId 用户
     * @param tenantId 租户
     */
    fun departmentsByUserIdAndTenantId(userId: String, tenantId: String): List<DevopsDepartment>? {
        return devopsApi.departmentsByUserIdAndTenantId(userId = userId, tenantId = tenantId).execute().body()?.data
    }

    fun identifyProjectManageAuth(userId: String, projectId: String): Boolean? {
        return devopsApi.identifyProjectManageAuth(userId, projectId).execute().body()?.data
    }

    fun usersByProjectId(
        projectId: String,
        withAdmin: Boolean = true,
        withParentAdmin: Boolean = false
    ): List<CanwayUser>? {
        return devopsApi.usersByProjectId(projectId, withAdmin, withParentAdmin).execute().body()?.data
    }

    fun departmentsByProjectId(projectId: String): List<DevopsDepartment>? {
        return devopsApi.departmentsByProjectId(projectId).execute().body()
    }

    fun childrenDepartments(departmentId: String): List<DevopsDepartment>? {
        return devopsApi.childrenDepartments(departmentId).execute().body()
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

fun main() {
    val conf = DevopsConf().apply {
        appCode = "bk_ci"
        appSecret = "1206bbf7-2e29-4bb7-b5b6-7d34b93cfdf8"
        bkHost = "http://paas.upgtest.com"
        devopsHost = "http://devops.upgtest.com"
    }

    val devopsApi = DevopsClient(conf).devopsApi
    val response = devopsApi.usersByProjectId("we77e4",true, false).execute()
    println(response.body())
}
