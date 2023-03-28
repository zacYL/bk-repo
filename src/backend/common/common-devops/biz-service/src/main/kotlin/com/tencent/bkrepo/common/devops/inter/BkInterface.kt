package com.tencent.bkrepo.common.devops.inter

import com.tencent.bkrepo.common.devops.pojo.BkChildrenDepartment
import com.tencent.bkrepo.common.devops.pojo.BkDepartmentId
import com.tencent.bkrepo.common.devops.pojo.BkDepartmentUser
import com.tencent.bkrepo.common.devops.pojo.BkPage
import com.tencent.bkrepo.common.devops.pojo.BkResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface BkInterface {

    /**
     * 获取部门列表
     */
    @GET("/api/c/compapi/v2/usermanage/list_departments")
    fun listDepartments(
        @Query("bk_app_code") bkAppCode: String,
        @Query("bk_app_secret") bkAppSecret: String,
        // 当前用户用户名，应用免登录态验证白名单中的应用，用此字段指定当前用户
        @Query("bk_username") bkUsername: String? = null,
        // 当前用户登录态，bk_token与bk_username必须一个有效，bk_token可以通过Cookie获取
        @Query("bk_token") bkToken: String? = null,
        // 查找字段, 默认值为 'id'
        @Query("lookup_field") lookupField: String? = null,
        // 返回值字段, 例如"username,id"
        @Query("fields") fields: String? = null,
        // 精确查找内容列表, 例如"jack,pony"
        @Query("exact_lookups") exactLookups: String? = null,
        // 模糊查找内容列表, 例如"jack,pony"
        @Query("fuzzy_lookups") fuzzyLookups: String? = null,
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null
    ): Call<BkResponse<BkPage<BkChildrenDepartment>?>>

    /**
     * 获取部门列表
     */
    @GET("/api/c/compapi/v2/usermanage/list_departments")
    fun allDepartmentIds(
        @Query("bk_app_code") bkAppCode: String,
        @Query("bk_app_secret") bkAppSecret: String,
        // 当前用户用户名，应用免登录态验证白名单中的应用，用此字段指定当前用户
        @Query("bk_username") bkUsername: String? = null,
        // 当前用户登录态，bk_token与bk_username必须一个有效，bk_token可以通过Cookie获取
        @Query("bk_token") bkToken: String? = null,
        // 返回值字段, 例如"username,id"
        @Query("fields") fields: String? = null
    ): Call<BkResponse<BkPage<BkDepartmentId>?>>

    /**
     * 查询用户的部门信息
     */
    @GET("/api/c/compapi/v2/usermanage/list_department_profiles/")
    fun listDepartmentProfiles(
        @Query("bk_app_code") bkAppCode: String,
        @Query("bk_app_secret") bkAppSecret: String,
        // 当前用户用户名，应用免登录态验证白名单中的应用，用此字段指定当前用户
        @Query("bk_username") bkUsername: String? = null,
        // 当前用户登录态，bk_token与bk_username必须一个有效，bk_token可以通过Cookie获取
        @Query("bk_token") bkToken: String? = null,
        // 部门id
        @Query("id") id: String? = null,
        // 查询字段, 默认为 'id'
        @Query("lookup_field") lookupField: String? = null,
        // 是否级联查询部门用户,默认为否
        @Query("recursive") recursive: Boolean? = false
    ): Call<BkResponse<BkPage<BkDepartmentUser>?>>
}
