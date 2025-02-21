package com.tencent.bkrepo.common.devops.client

import com.tencent.bkrepo.common.devops.DEPLOY_CANWAY
import com.tencent.bkrepo.common.devops.conf.DevopsConf
import com.tencent.bkrepo.common.devops.enums.subjectCode
import com.tencent.bkrepo.common.devops.inter.DevopsInterface
import com.tencent.bkrepo.common.devops.pojo.CanwayGroup
import com.tencent.bkrepo.common.devops.pojo.DevopsDepartment
import com.tencent.bkrepo.common.devops.pojo.request.CanwayUserGroupRequest
import com.tencent.bkrepo.common.devops.pojo.response.CanwayGroupResponse
import com.tencent.bkrepo.common.devops.pojo.response.CanwayUser
import com.tencent.bkrepo.common.devops.util.http.CertTrustManager
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
        val subjectCanwayGroupList =
            devopsApi.organizationByProjectId(projectId = projectId, subjectCode = subjectCode.GROUP.name).execute()
                .body()?.data ?: return listOf()
        if (subjectCanwayGroupList.isEmpty()) return listOf()
        val groupInformation =
            devopsApi.groupInformation(CanwayUserGroupRequest(groupIds = subjectCanwayGroupList.map { it.subjectId }))
                .execute().body()?.data ?: listOf()
        val canwayGroupList = mutableListOf<CanwayGroup>()
        subjectCanwayGroupList.forEach { subjectCanwayGroup ->
            canwayGroupList.add(
                CanwayGroup(
                    id = subjectCanwayGroup.subjectId,
                    name = subjectCanwayGroup.subjectName,
                    users = groupInformation.filter { subjectCanwayGroup.subjectId == it.userGroupId }
                        .map { it.userId })
            )
        }
        return canwayGroupList
    }

    /**
     * 查询用户组所有用户
     */
    fun groupInformationByGroupIds(canwayUserGroupRequest: CanwayUserGroupRequest): List<CanwayGroupResponse>? {
        return devopsApi.groupInformation(canwayUserGroupRequest).execute().body()?.data
    }

    /**
     * 查看用户是否有系统管理员权限
     */
    fun identifySystemManageAuth(userId: String): Boolean? {
        return devopsApi.identifySystemManageAuth(userId).execute().body()?.data
    }

    /**
     * 查看用户是否有租户管理员权限
     */
    fun identifyTenantManageAuth(userId: String, tenantId: String): Boolean? {
        return devopsApi.identifyTenantManageAuth(userId, tenantId).execute().body()?.data
    }

    /**
     * 查看用户是否有项目管理员权限
     */
    fun identifyProjectManageAuth(userId: String, projectId: String): Boolean? {
        return devopsApi.identifyProjectManageAuth(userId, projectId).execute().body()?.data
    }

    fun usersByProjectId(
        projectId: String
    ): List<CanwayUser>? {
        return devopsApi.usersByProjectId(projectId).execute().body()?.data
    }

    fun departmentsByProjectId(projectId: String): List<DevopsDepartment>? {
        val subjectCanwayDepartment =
            devopsApi.organizationByProjectId(projectId, subjectCode = subjectCode.DEPARTMENT.name).execute()
                .body()?.data
        return subjectCanwayDepartment?.map {
            DevopsDepartment(
                id = it.subjectId,
                name = it.subjectName
            )
        } ?: listOf()
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
