/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 Tencent.  All rights reserved.
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

package com.tencent.bkrepo.repository.api

import com.tencent.bkrepo.common.api.constant.REPOSITORY_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.metadata.pojo.node.NodeRestoreRequest
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.repository.pojo.node.NodeDeleteResult
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.node.NodeRestoreResult
import com.tencent.bkrepo.repository.pojo.node.NodeSizeInfo
import com.tencent.bkrepo.repository.pojo.node.service.NodeArchiveRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeCleanRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeCompressedRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeLinkRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveCopyRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeRenameRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeUnCompressedRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeUpdateAccessDateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeUpdateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodesDeleteRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * 资源节点服务接口
 */
@Tag(name = "节点服务接口")
@Primary
@FeignClient(REPOSITORY_SERVICE_NAME, contextId = "NodeClient", primary = false)
@RequestMapping("/service/node")
@Deprecated("replace with NodeService")
interface NodeClient {

    @Operation(summary = "根据路径查看节点详情")
    @GetMapping("/detail/{projectId}/{repoName}")
    fun getNodeDetail(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam fullPath: String,
    ): Response<NodeDetail?>

    @Operation(summary = "根据路径查看节点是否存在")
    @GetMapping("/exist/{projectId}/{repoName}")
    fun checkExist(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam fullPath: String,
    ): Response<Boolean>

    @Operation(summary = "列出仓库中已存在的节点")
    @PostMapping("/exist/list/{projectId}/{repoName}")
    fun listExistFullPath(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestBody fullPathList: List<String>,
    ): Response<List<String>>

    @PostMapping("/page/{projectId}/{repoName}")
    fun listNodePage(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam path: String,
        @RequestBody option: NodeListOption = NodeListOption(),
    ): Response<Page<NodeInfo>>

    @Operation(summary = "创建节点")
    @PostMapping("/create")
    fun createNode(@RequestBody nodeCreateRequest: NodeCreateRequest): Response<NodeDetail>

    @Operation(summary = "更新节点")
    @PostMapping("/update")
    fun updateNode(@RequestBody nodeUpdateRequest: NodeUpdateRequest): Response<Void>

    @Operation(summary = "更新节点访问时间")
    @PostMapping("/update/access/")
    fun updateNodeAccessDate(@RequestBody nodeUpdateAccessDateRequest: NodeUpdateAccessDateRequest): Response<Void>

    @Operation(summary = "重命名节点")
    @PostMapping("/rename")
    fun renameNode(@RequestBody nodeRenameRequest: NodeRenameRequest): Response<Void>

    @Operation(summary = "移动节点")
    @PostMapping("/move")
    fun moveNode(@RequestBody nodeMoveRequest: NodeMoveCopyRequest): Response<NodeDetail>

    @Operation(summary = "复制节点")
    @PostMapping("/copy")
    fun copyNode(@RequestBody nodeCopyRequest: NodeMoveCopyRequest): Response<NodeDetail>

    @Operation(summary = "删除节点")
    @DeleteMapping("/delete")
    fun deleteNode(@RequestBody nodeDeleteRequest: NodeDeleteRequest): Response<NodeDeleteResult>

    @Operation(summary = "删除节点")
    @DeleteMapping("/batch/delete")
    fun deleteNodes(@RequestBody nodesDeleteRequest: NodesDeleteRequest): Response<NodeDeleteResult>

    @Operation(summary = "恢复节点")
    @PostMapping("/restore")
    fun restoreNode(nodeRestoreRequest: NodeRestoreRequest): Response<NodeRestoreResult>

    @Operation(summary = "查询节点大小信息")
    @GetMapping("/size/{projectId}/{repoName}")
    fun computeSize(
        @Parameter(name = "所属项目", required = true)
        @PathVariable
        projectId: String,
        @Parameter(name = "仓库名称", required = true)
        @PathVariable
        repoName: String,
        @Parameter(name = "节点完整路径", required = true)
        @RequestParam
        fullPath: String,
        @Parameter(name = "估计值", required = false)
        @RequestParam
        estimated: Boolean = false,
    ): Response<NodeSizeInfo>

    @Operation(summary = "查询文件节点数量")
    @GetMapping("/file/{projectId}/{repoName}")
    fun countFileNode(
        @Parameter(name = "所属项目", required = true)
        @PathVariable
        projectId: String,
        @Parameter(name = "仓库名称", required = true)
        @PathVariable
        repoName: String,
        @Parameter(name = "节点完整路径", required = true)
        @RequestParam
        path: String,
    ): Response<Long>

    @Operation(summary = "自定义查询节点，如不关注总记录数请使用queryWithoutCount")
    @PostMapping("/search")
    fun search(@RequestBody queryModel: QueryModel): Response<Page<Map<String, Any?>>>

    @Operation(summary = "自定义查询节点，不计算总记录数")
    @PostMapping("/queryWithoutCount")
    fun queryWithoutCount(@RequestBody queryModel: QueryModel): Response<Page<Map<String, Any?>>>

    @Deprecated("replace with listNodePage")
    @Operation(summary = "列表查询指定目录下所有节点")
    @GetMapping("/list/{projectId}/{repoName}")
    fun listNode(
        @Parameter(name = "所属项目", required = true)
        @PathVariable
        projectId: String,
        @Parameter(name = "仓库名称", required = true)
        @PathVariable
        repoName: String,
        @Parameter(name = "所属目录", required = true)
        @RequestParam
        path: String,
        @Parameter(name = "是否包含目录", required = false)
        @RequestParam
        includeFolder: Boolean = true,
        @Parameter(name = "是否深度查询文件", required = false)
        @RequestParam
        deep: Boolean = false,
        @Parameter(name = "是否包含元数据", required = false)
        @RequestParam
        includeMetadata: Boolean = false,
    ): Response<List<NodeInfo>>

    @Operation(summary = "按sha256分页查询节点")
    @PostMapping("/page", params = ["sha256"])
    fun listPageNodeBySha256(
        @RequestParam("sha256", required = true) sha256: String,
        @RequestBody nodeListOption: NodeListOption
    ): Response<Page<NodeInfo>>

    @Operation(summary = "查询已删除节点")
    @GetMapping("/deleted/detail/{projectId}/{repoName}")
    fun getDeletedNodeDetail(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam fullPath: String,
    ): Response<List<NodeDetail>>

    @Operation(summary = "通过sha256查询已删除节点")
    @GetMapping("/deletedBySha256/detail/{projectId}/{repoName}")
    fun getDeletedNodeDetailBySha256(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam sha256: String,
    ): Response<NodeDetail?>

    /**
     * 归档文件成功通知
     * */
    @Operation(summary = "归档节点")
    @PutMapping("/archive/")
    fun archiveNode(@RequestBody nodeArchiveRequest: NodeArchiveRequest): Response<Void>

    /**
     * 恢复文件成功通知
     * */
    @Operation(summary = "恢复节点")
    @PutMapping("/archive/restore/")
    fun restoreNode(@RequestBody nodeArchiveRequest: NodeArchiveRequest): Response<Void>

    /**
     * 归档文件成功通知
     * */
    @Operation(summary = "压缩节点")
    @PutMapping("/compress/")
    fun compressedNode(@RequestBody nodeCompressedRequest: NodeCompressedRequest): Response<Void>

    /**
     * 恢复文件成功通知
     * */
    @Operation(summary = "解压节点")
    @PutMapping("/uncompress/")
    fun uncompressedNode(@RequestBody nodeUnCompressedRequest: NodeUnCompressedRequest): Response<Void>

    @Operation(summary = "清理最后访问时间早于{date}的文件节点")
    @DeleteMapping("/clean")
    fun cleanNodes(@RequestBody nodeCleanRequest: NodeCleanRequest): Response<NodeDeleteResult>

    @Operation(summary = "创建软链接")
    @PostMapping("/link")
    fun link(@RequestBody nodeLinkRequest: NodeLinkRequest): Response<NodeDetail>
}
