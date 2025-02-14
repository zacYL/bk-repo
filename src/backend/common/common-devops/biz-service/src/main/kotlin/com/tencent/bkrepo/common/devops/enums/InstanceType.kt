package com.tencent.bkrepo.common.devops.enums

enum class InstanceType(val value: Int)    {
    // CI 超级管理员
    SUPER(1),
    // CI 租户管理员
    TENANT(3),
    // CI 项目管理员
    PROJECT(2);
}