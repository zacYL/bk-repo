/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.replication.pojo.record

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.replication.pojo.task.objects.PackageConstraint
import com.tencent.bkrepo.replication.pojo.task.objects.PathConstraint
import com.tencent.bkrepo.replication.pojo.task.setting.ConflictStrategy
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("同步任务执行记录详情")
data class ReplicaRecordDetail(
    @ApiModelProperty("详情id")
    var id: String,
    @ApiModelProperty("记录id")
    var recordId: String,
    @ApiModelProperty("本地集群名称")
    var localCluster: String,
    @ApiModelProperty("远程集群名称")
    var remoteCluster: String,
    @ApiModelProperty("本地仓库名称")
    var localRepoName: String,
    @ApiModelProperty("仓库类型")
    val repoType: RepositoryType,
    @ApiModelProperty("包限制")
    var packageConstraint: PackageConstraint? = null,
    @ApiModelProperty("路径名称")
    var pathConstraint: PathConstraint? = null,
    @ApiModelProperty("制品名称")
    var artifactName: String? = null,
    @ApiModelProperty("包版本")
    var version: String? = null,
    @ApiModelProperty("冲突策略")
    var conflictStrategy: ConflictStrategy? = null,
    @ApiModelProperty("制品大小")
    var size: Long? = null,
    @ApiModelProperty("制品sha256")
    var sha256: String? = null,
    @ApiModelProperty("运行状态")
    var status: ExecutionStatus,
    @ApiModelProperty("同步进度")
    var progress: ReplicaProgress,
    @ApiModelProperty("开始时间")
    var startTime: LocalDateTime,
    @ApiModelProperty("结束时间")
    var endTime: LocalDateTime? = null,
    @ApiModelProperty("错误原因，未执行或执行成功则为null")
    var errorReason: String? = null
)
