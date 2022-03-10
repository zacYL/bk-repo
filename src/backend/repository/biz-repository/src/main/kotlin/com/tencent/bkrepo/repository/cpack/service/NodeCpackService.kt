package com.tencent.bkrepo.repository.cpack.service

import com.tencent.bkrepo.repository.cpack.pojo.node.service.NodeBatchDeleteRequest

interface NodeCpackService {
    fun nodeBatchDelete(nodeBatchDeleteRequest: NodeBatchDeleteRequest)

    fun countBatchDeleteNodes(nodeBatchDeleteRequest: NodeBatchDeleteRequest): Long
}
