package com.tencent.bkrepo.common.devops.inter


import com.tencent.bkrepo.common.devops.pojo.CanwayProjectSubjectData
import com.tencent.bkrepo.common.devops.pojo.DevopsDepartment
import com.tencent.bkrepo.common.devops.pojo.request.CanwayUserGroupRequest
import com.tencent.bkrepo.common.devops.pojo.response.CanwayGroupResponse
import com.tencent.bkrepo.common.devops.pojo.response.CanwayResponse
import com.tencent.bkrepo.common.devops.pojo.response.CanwayUser
import retrofit2.Call
import retrofit2.http.*

interface DevopsInterface {

    /**
     * 获取用户所属组织包括父级
     * [userId] 用户
     */
    @GET("/ms/usermanager/api/service/organization/departments/{userId}")
    fun departmentsByUserId(@Path("userId") userId: String): Call<CanwayResponse<List<DevopsDepartment>>?>

    /**
     * 查询项目下所有部门或用户组
     * [projectId] 项目
     */
    @GET("/ms/auth/api/service/project/{projectId}/member/direct/list")
    fun organizationByProjectId(
        @Path("projectId") projectId: String,
        @Query("subjectCode") subjectCode: String? = null
    ): Call<CanwayResponse<List<CanwayProjectSubjectData>>?>

    /**
     * 自助查询用户组成员
     */
    @POST("/ms/auth/api/service/custom/user_group/member/query")
    @Headers("Content-Type: application/json")
    fun groupInformation(
        @Body
        canwayUserGroupRequest: CanwayUserGroupRequest
    ): Call<CanwayResponse<List<CanwayGroupResponse>>?>

    /**
     * 查看用户是否有系统管理员权限
     */
    @GET("/ms/auth/api/service/system/superior_admin")
    fun identifySystemManageAuth(
        @Header("X-DEVOPS-UID") userId: String
    ): Call<CanwayResponse<Boolean>?>

    /**
     * 查看用户是否有租户管理员权限
     */
    @GET("/ms/auth/api/service/tenant/{tenantId}/member/superior_admin")
    fun identifyTenantManageAuth(
        @Header("X-DEVOPS-UID") userId: String,
        @Path("tenantId") tenantId: String
    ): Call<CanwayResponse<Boolean>?>

    /**
     * 查看用户是否有项目管理员权限
     */
    @GET("/ms/auth/api/service/project/{projectId}/admin/superior_admin")
    fun identifyProjectManageAuth(
        @Header("X-DEVOPS-UID") userId: String,
        @Path("projectId") projectId: String
    ): Call<CanwayResponse<Boolean>?>

    /**
     * 查询指定CI项目下项目成员
     * @param projectId 项目
     */
    @GET("/ms/auth/api/service/project/{projectId}/member/list")
    fun usersByProjectId(
        @Path("projectId") projectId: String,
    ): Call<CanwayResponse<List<CanwayUser>>?>
}
