package com.tencent.bkrepo.common.devops.inter

import com.tencent.bkrepo.common.devops.pojo.response.CanwayResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DevopsAuthInterface {

    /**
     * 加载CI 下用户有权限的项目列表，用户信息由Cookie传入
     * @param typeId 2：项目，1：需求池
     */
    @GET("/ms/permission/api/service/administrator/{userId}/{type}")
    fun isAdmin(
        @Path("userId") userId: String,
        @Path("type") type: Int,
        @Query("instanceCode") instanceCode: String? = null
    ): Call<CanwayResponse<Boolean>>
}
