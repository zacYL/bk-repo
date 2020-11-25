/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.auth.pojo.permission

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("创建权限请求")
data class CreatePermissionRequest(
    @ApiModelProperty("资源类型")
    val resourceType: ResourceType = ResourceType.REPO,
    @ApiModelProperty("项目ID")
    val projectId: String? = null,
    @ApiModelProperty("权限名")
    val permName: String,
    @ApiModelProperty("关联仓库名")
    val repos: List<String> = emptyList(),
    @ApiModelProperty("匹配路径")
    val includePattern: List<String> = emptyList(),
    @ApiModelProperty("不匹配路径")
    val excludePattern: List<String> = emptyList(),
    @ApiModelProperty("绑定用户")
    val users: List<String> = emptyList(),
    @ApiModelProperty("绑定角色")
    val roles: List<String> = emptyList(),
    @ApiModelProperty("绑定部门")
    val departments: List<String> = emptyList(),
    @ApiModelProperty("权限动作")
    val actions: List<PermissionAction> = emptyList(),
    @ApiModelProperty("创建人")
    val createBy: String,
    @ApiModelProperty("修改人")
    val updatedBy: String
)
