/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.common.devops.pojo.response

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目-显示模型")
data class DevopsProject(
    @ApiModelProperty("主键ID")
    val id: Long,
    @ApiModelProperty("类别Id")
    val typeId: String = "2",
    @ApiModelProperty("模板id")
    val templateId: String?,
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("项目名称")
    val projectName: String,
    @ApiModelProperty("项目代码")
    val projectCode: String,
    @ApiModelProperty("项目类型")
    val projectType: Int?,
    @ApiModelProperty("创建时间")
    val createdAt: String?,
    @ApiModelProperty("创建人")
    val creator: String?,
    @ApiModelProperty("描述")
    val description: String?,
    @ApiModelProperty("项目标识")
    val englishName: String,
    @ApiModelProperty("自定义英文标识")
    val englishNameCustom: String,
    @ApiModelProperty("extra")
    val extra: String?,
    @ApiModelProperty("kind")
    val kind: Int?,
    @ApiModelProperty("logo地址")
    val logoAddr: String?,
    @ApiModelProperty("修改时间")
    val updatedAt: String?,
    @ApiModelProperty("启用")
    val enabled: Boolean?,
    @ApiModelProperty("项目是否被收藏")
    val star: Boolean?,
    @ApiModelProperty("父级项目标识")
    val parentCode: String? = null,
    @ApiModelProperty("子项目")
    val children: List<DevopsProject> = mutableListOf(),
    @ApiModelProperty("状态")
    val status: String,
    @ApiModelProperty("管理员字符串")
    val managersStr: String,
    @ApiModelProperty("权限")
    val auth: String?,
    @ApiModelProperty("所属部门Id")
    val deptId: String?,
    @ApiModelProperty("所属部门名字")
    val deptName: String?
)
