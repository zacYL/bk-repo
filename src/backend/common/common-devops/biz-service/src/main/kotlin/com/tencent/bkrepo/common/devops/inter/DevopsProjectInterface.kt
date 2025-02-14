package com.tencent.bkrepo.common.devops.inter

import com.tencent.bkrepo.common.devops.pojo.response.CanwayResponse
import com.tencent.bkrepo.common.devops.pojo.response.DevopsProject
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface DevopsProjectInterface {
    /**
     * 加载CI 下用户有权限的项目列表，用户信息由Cookie传入
     * @param typeId 2：项目，1：需求池
     */
    @GET("/ms/project/api/user/project/cw/selectByType")
    fun projects(
        @Query("typeId") typeId: Int = 2,
        @Header("Cookie") cookie: String
    ): Call<CanwayResponse<List<DevopsProject>>>
}
