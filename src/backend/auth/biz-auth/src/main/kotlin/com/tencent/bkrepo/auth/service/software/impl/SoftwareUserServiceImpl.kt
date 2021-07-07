package com.tencent.bkrepo.auth.service.software.impl

import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_USER
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_VIEWER
import com.tencent.bkrepo.auth.constant.BK_SOFTWARE
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionDepartmentRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionUserRequest
import com.tencent.bkrepo.auth.pojo.software.SoftwareUseUnit
import com.tencent.bkrepo.auth.pojo.software.UnitType
import com.tencent.bkrepo.auth.pojo.software.request.UseUnitDeleteRequest
import com.tencent.bkrepo.auth.pojo.software.response.SoftwareUseUnitResponse
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.auth.service.software.SoftwareUserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SoftwareUserServiceImpl(
    private val permissionService: PermissionService
) : SoftwareUserService {
    override fun unit(repoName: String): SoftwareUseUnitResponse {
        // 加载仓库内置权限
        val userSet = mutableSetOf<SoftwareUseUnit>()
        val departmentSet = mutableSetOf<SoftwareUseUnit>()
        val permissions = permissionService.listBuiltinPermission(BK_SOFTWARE, repoName)
        for (permission in permissions) {
            if (permission.permName == AUTH_BUILTIN_USER) {
                permission.users.map { userId ->
                    userSet.add(
                        SoftwareUseUnit(
                            unitId = userId,
                            unitType = UnitType.USER,
                            allowPush = true
                        )
                    )
                }
                permission.departments.map { departmentId ->
                    departmentSet.add(
                        SoftwareUseUnit(
                            unitId = departmentId,
                            unitType = UnitType.DEPARTMENT,
                            allowPush = true
                        )
                    )
                }
            }
            if (permission.permName == AUTH_BUILTIN_VIEWER) {
                permission.users.map { userId ->
                    userSet.add(
                        SoftwareUseUnit(
                            unitId = userId,
                            unitType = UnitType.USER,
                            allowPush = false
                        )
                    )
                }
                permission.departments.map { departmentId ->
                    departmentSet.add(
                        SoftwareUseUnit(
                            unitId = departmentId,
                            unitType = UnitType.DEPARTMENT,
                            allowPush = false
                        )
                    )
                }
            }
        }
        return SoftwareUseUnitResponse(user = userSet, department = departmentSet)
    }

    @Transactional
    override fun updatePermission(repoName: String, set: Set<String>, unitType: UnitType, push: Boolean): Boolean {
        // 加载内置权限
        val permissions = permissionService.listBuiltinPermission(BK_SOFTWARE, repoName)
        // 使用者
        val permissionUser = permissions.find { it.permName == AUTH_BUILTIN_USER }
        if (permissionUser != null) {
            updatePermission(permissionUser, unitType, push, set)
        }
        // 查看者
        val permissionViewer = permissions.find { it.permName == AUTH_BUILTIN_VIEWER }
        if (permissionViewer != null) {
            updatePermission(permissionViewer, unitType, !push, set)
        }
        return true
    }

    override fun addUnit(repoName: String, set: Set<String>, unitType: UnitType, push: Boolean): Boolean {
        // 加载内置权限
        val permissions = permissionService.listBuiltinPermission(BK_SOFTWARE, repoName)
        if (push) {
            // 使用者
            val permissionUser = permissions.find { it.permName == AUTH_BUILTIN_USER }
            if (permissionUser != null) {
                updatePermission(permissionUser, unitType, true, set)
            }
        } else {
            // 查看者
            val permissionViewer = permissions.find { it.permName == AUTH_BUILTIN_VIEWER }
            if (permissionViewer != null) {
                updatePermission(permissionViewer, unitType, true, set)
            }
        }
        return true
    }

    /**
     * [permission] 权限数据
     * [unitType]   单位：用户或部门
     * [action]     true: 添加， false: 删除
     * [set]        用户或部门 id 集合
     */
    private fun updatePermission(permission: Permission, unitType: UnitType, action: Boolean, set: Set<String>) {
        when (unitType) {
            UnitType.USER -> {
                permissionService.updatePermissionUser(
                    UpdatePermissionUserRequest(
                        permissionId = permission.id!!,
                        userId = merge(permission.users, set, action)
                    )
                )
            }
            UnitType.DEPARTMENT -> {
                permissionService.updatePermissionDepartment(
                    UpdatePermissionDepartmentRequest(
                        permissionId = permission.id!!,
                        departmentId = merge(permission.departments, set, action)
                    )
                )
            }
        }
    }

    override fun deleteUnit(repoName: String, useUnitDeleteRequest: UseUnitDeleteRequest): Boolean {
        // 加载内置权限
        val permissions = permissionService.listBuiltinPermission(BK_SOFTWARE, repoName)
        // 使用者
        val permissionUser = permissions.find { it.permName == AUTH_BUILTIN_USER }
        // 查看者
        val permissionViewer = permissions.find { it.permName == AUTH_BUILTIN_VIEWER }

        useUnitDeleteRequest.user.let {
            if (permissionUser != null) {
                updatePermission(permissionUser, UnitType.USER, false, it)
            }
            if (permissionViewer != null) {
                updatePermission(permissionViewer, UnitType.USER, false, it)
            }
        }

        useUnitDeleteRequest.department.let {
            if (permissionUser != null) {
                updatePermission(permissionUser, UnitType.DEPARTMENT, false, it)
            }
            if (permissionViewer != null) {
                updatePermission(permissionViewer, UnitType.DEPARTMENT, false, it)
            }
        }
        return true
    }

    /**
     *
     */
    private fun merge(list: List<String>, collection: Set<String>, type: Boolean): List<String> {
        val set = list.toMutableSet()
        if (type) set.addAll(collection) else set.removeAll(collection)
        return set.toList()
    }
}
