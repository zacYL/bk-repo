package com.tencent.bkrepo.auth.service.impl

import com.tencent.bkrepo.auth.ciApi
import com.tencent.bkrepo.auth.ciPermission
import com.tencent.bkrepo.auth.ciUserManager
import com.tencent.bkrepo.auth.exception.DevopsRequestException
import com.tencent.bkrepo.auth.pojo.CanwayGroup
import com.tencent.bkrepo.auth.pojo.DevopsDepartment
import com.tencent.bkrepo.auth.service.DevopsUserService
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.devops.api.conf.DevopsConf
import com.tencent.bkrepo.common.devops.api.pojo.response.CanwayResponse
import com.tencent.bkrepo.common.devops.api.util.http.CanwayHttpUtils
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = "canway", matchIfMissing = true)
class DevopsUserServiceImpl(
    devopsConf: DevopsConf
) : DevopsUserService {

    val devopsHost = devopsConf.devopsHost.trim('/')

    @Throws(DevopsRequestException::class)
    override fun usersByProjectId(projectId: String): List<String>? {
        val requestUrl = "$devopsHost$ciPermission$ciApi$userApi$projectId"
        val response = CanwayHttpUtils.doGet(requestUrl)
        return response.content.readJsonString<CanwayResponse<List<String>>>().data
    }

    @Throws(DevopsRequestException::class)
    override fun groupsByProjectId(projectId: String): List<CanwayGroup>? {
        val requestUrl = "$devopsHost$ciPermission$ciApi$groupApi$projectId"
        val response = CanwayHttpUtils.doGet(requestUrl)
        return response.content.readJsonString<CanwayResponse<List<CanwayGroup>>>().data
    }

    @Throws(DevopsRequestException::class)
    override fun departmentsByProjectId(projectId: String): List<DevopsDepartment>? {
        val requestUrl = "$devopsHost$ciPermission$ciApi$departmentApi$projectId"
        val response = CanwayHttpUtils.doGet(requestUrl)
        return response.content.readJsonString<CanwayResponse<List<DevopsDepartment>>>().data
    }

    @Throws(DevopsRequestException::class)
    override fun childrenDepartments(departmentId: String): List<DevopsDepartment>? {
        val requestUrl = "s$devopsHost$ciUserManager$ciApi$childrenDepartmentApi$departmentId"
        val response = CanwayHttpUtils.doGet(requestUrl)
        return response.content.readJsonString<CanwayResponse<List<DevopsDepartment>>>().data
    }

    companion object {
        const val userApi = "/service/resource_instance/view/project/"
        const val departmentApi = "/service/resource_instance/view/department/project/"
        const val groupApi = "/service/resource_instance/view/group/project/"
        const val childrenDepartmentApi = "/service/organization/under/"
    }
}
