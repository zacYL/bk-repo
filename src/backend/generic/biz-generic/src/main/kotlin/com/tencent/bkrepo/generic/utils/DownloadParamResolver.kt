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
    fun resolveMultiPathParam(commonPath: String, paths: String): List<String> {
        val pathList = splitParam(paths)
        return commonPath.takeIf { it != StringPool.ROOT }
            ?.let { pathList.map { "$commonPath${it.ensurePrefix(StringPool.SLASH)}" } }
            ?: pathList
    }

    /**
     * 解析请求参数中的多节点ID参数
     */
    fun resolveMultiNodeIdParam(projectId: String, ids: String, nodeClient: NodeClient): List<String> {
        val idList = splitParam(ids)
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
        return idList.map {
            if (ObjectId.isValid(it)) {
                nodeClient.getNodeFullPathById(projectId, it).data
                    ?: throw ErrorCodeException(GenericMessageCode.NODE_ID_NOT_FOUND, it)
            } else {
                throw ErrorCodeException(GenericMessageCode.NODE_ID_NOT_FOUND, it)
            }
        }
    }
}
