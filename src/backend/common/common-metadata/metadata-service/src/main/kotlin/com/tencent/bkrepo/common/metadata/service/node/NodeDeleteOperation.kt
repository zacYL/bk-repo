/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bkrepo.common.metadata.service.node

import com.tencent.bkrepo.repository.pojo.node.NodeDeleteResult
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodesDeleteRequest
import java.time.LocalDateTime

/**
 * 节点删除接口
 */
interface NodeDeleteOperation {

    /**
     * 删除指定节点, 逻辑删除
     */
    fun deleteNode(deleteRequest: NodeDeleteRequest): NodeDeleteResult

    /**
     * 批量删除指定节点, 逻辑删除
     */
    fun deleteNodes(nodesDeleteRequest: NodesDeleteRequest): NodeDeleteResult

    /**
     * 统计批量删除指定节点总数
     */
    fun countDeleteNodes(nodesDeleteRequest: NodesDeleteRequest): Long

    /**
     * 根据fullpath删除对应节点
     * 不会进行已删除节点数据返回
     * 不会进行容量清理，需要自行进行容量清理
     */
    fun deleteByFullPathWithoutDecreaseVolume(projectId: String, repoName: String, fullPath: String, operator: String)

    /**
     * 根据全路径删除文件或者目录
     */
    fun deleteByPath(
        projectId: String,
        repoName: String,
        fullPath: String,
        operator: String,
        source: String? = null
    ): NodeDeleteResult

    /**
     * 根据全路径批量删除文件或者目录
     */
    fun deleteByPaths(projectId: String, repoName: String, fullPaths: List<String>, operator: String): NodeDeleteResult

    /**
     * 根据最后访问时间删除[date]之前的历史数据
     */
    fun deleteBeforeDate(
        projectId: String,
        repoName: String,
        date: LocalDateTime,
        operator: String,
        path: String,
        decreaseVolume: Boolean = true
    ): NodeDeleteResult

    /**
     * 删除旧node
     * 参数暂时保留，后续只保留nodeId，operator
     */
    fun deleteNodeById(
        projectId: String,
        repoName: String,
        fullPath: String,
        operator: String,
        nodeId: String
    ): NodeDeleteResult
}
