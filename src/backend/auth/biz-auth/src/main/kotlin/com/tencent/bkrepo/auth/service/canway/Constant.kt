package com.tencent.bkrepo.auth.service.canway

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction

const val ci = "/ms/permission"
const val ciApi = "/api"

// ci 相关cookie
const val ciTenant = "X-DEVOPS-TENANT-ID"
const val ciProject = "X-DEVOPS-PROJECT-ID"

// 在 canway 权限中心注册的资源名
const val ciResourceCode = "bkrepo"
// 在 canway 权限中心注册资源所属类型: project 代表项目一级
const val ciBelongCode = "project"

const val REPO_ADMIN = "制品库-管理员"
const val REPO_VIEWER = "制品库-查看者"
const val REPO_USER = "制品库-使用者"

const val USER_TYPE = "G_USER"
