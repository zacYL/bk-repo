package com.tencent.bkrepo.auth

const val ci = "/ms/permission"
const val ciApi = "/api"

// ci 相关cookie
const val ciTenant = "X-DEVOPS-TENANT-ID"
const val ciProject = "X-DEVOPS-PROJECT-ID"

// 在 canway 权限中心注册资源所属类型: project 代表项目一级
const val BELONGCODE = "project"
// 在 canway 权限中心注册的资源名
const val RESOURCECODE = "bkrepo"
