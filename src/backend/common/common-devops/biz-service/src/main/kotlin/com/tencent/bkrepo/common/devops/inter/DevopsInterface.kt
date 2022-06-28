package com.tencent.bkrepo.common.devops.inter

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.devops.pojo.CanwayGroup
import com.tencent.bkrepo.common.devops.pojo.DevopsDepartment
import com.tencent.bkrepo.common.devops.pojo.response.CanwayResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DevopsInterface {

    /**
     * 加载CI 下用户有权限的项目列表，用户信息由Cookie传入
     * @param type 2：项目，1：需求池
     */
    @GET("/ms/permission/api/service/administrator/{userId}/{type}")
    fun isAdmin(
        @Path("userId") userId: String,
        @Path("type") type: Int,
        @Query("instanceCode") instanceCode: String? = null
    ): Call<CanwayResponse<Boolean>>

    /**
     * 项目下部门
     * [projectId] 项目
     */
    @GET("/ms/permission/api/service/resource_instance/view/department/project/{projectId}")
    fun departmentsByProjectId(@Path("projectId") projectId: String): Call<List<DevopsDepartment>?>

    /**
     * 部门下子部门
     * [departmentId] 父部门
     */
    @GET("/ms/permission/api/service/organization/under/{departmentId}")
    fun childrenDepartments(@Path("departmentId") departmentId: String): Call<List<DevopsDepartment>?>

    /**
     * 获取用户所属组织
     * [userId] 用户
     */
    @GET("/ms/usermanager/api/service/organization/organization/list")
    fun departmentByUserId(@Query("userId") userId: String): Call<List<DevopsDepartment>?>

    /**
     * 获取用户所属组织包括父级
     * [userId] 用户
     */
    @GET("/ms/usermanager/api/service/organization/departments/{userId}")
    fun departmentsByUserId(@Path("userId") userId: String): Call<CanwayResponse<List<DevopsDepartment>>?>

    /**
     * 查询项目下所有用户组
     * [projectId] 项目
     */
    @GET("/ms/permission/api/service/resource_instance/view/group/project/{projectId}")
    fun groupsByProjectId(@Path("projectId") projectId: String): Call<CanwayResponse<List<CanwayGroup>>?>

    /**
     * 查询用户在当前租户下的部门
     * @param userId 用户
     * @param tenantId 租户
     */
    @GET("/ms/permission/api/service/organization/department")
    fun departmentsByUserIdAndTenantId(
        @Query("userId") userId: String,
        @Query("tenantId") tenantId: String
    ): Call<Response<List<DevopsDepartment>>?>
}
