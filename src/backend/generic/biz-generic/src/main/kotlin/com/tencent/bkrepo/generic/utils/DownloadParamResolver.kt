package com.tencent.bkrepo.generic.utils

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.constant.ensurePrefix
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.generic.constant.GenericMessageCode
import com.tencent.bkrepo.repository.api.NodeClient
import org.bson.types.ObjectId

/**
 * 下载长参数解析工具
 */
object DownloadParamResolver {
    /**
     * 解析请求参数中的多路径参数
     */
    fun resolveMultiPathParam(commonPath: String, multiPath: String): List<String> {
        val pathList = splitParam(multiPath)
        return commonPath.takeIf { it != StringPool.ROOT }
            ?.let { pathList.map { "$commonPath${it.ensurePrefix(StringPool.SLASH)}" } }
            ?: pathList
    }

    /**
     * 解析请求参数中的多节点ID参数
     */
    fun resolveMultiNodeIdParam(projectId: String, multiId: String, nodeClient: NodeClient): List<String> {
        val idList = splitParam(multiId)
        return queryFullPath(projectId, idList, nodeClient)
    }

    /**
     * 分离单个参数
     */
    @Throws(ErrorCodeException::class)
    private fun splitParam(param: String): List<String> {
        if (!param.endsWith(">")) {
            throw ErrorCodeException(GenericMessageCode.DOWNLOAD_URL_TRUNCATED)
        }
        return param.ensurePrefix("<").removeSurrounding("<", ">").split(":").distinct()
    }

    /**
     * 将节点ID转换为节点路径
     */
    private fun queryFullPath(projectId: String, idList: List<String>, nodeClient: NodeClient): List<String> {
        checkId(idList)
        val fullPathMap = nodeClient.listFullPathById(projectId, idList).data
            ?: throw ErrorCodeException(GenericMessageCode.NODE_ID_NOT_FOUND)
        val notExistId = idList subtract fullPathMap.keys
        if (notExistId.isNotEmpty()) {
            throw ErrorCodeException(GenericMessageCode.NODE_ID_NOT_FOUND, notExistId.first())
        }
        return fullPathMap.values.toList()
    }

    /**
     * 校验ID格式
     */
    private fun checkId(id: List<String>) {
        id.forEach {
            if (!ObjectId.isValid(it)) throw ErrorCodeException(GenericMessageCode.NODE_ID_INVALID, it)
        }
    }
}
