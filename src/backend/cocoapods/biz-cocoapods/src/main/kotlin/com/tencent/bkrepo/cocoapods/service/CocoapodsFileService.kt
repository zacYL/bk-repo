package com.tencent.bkrepo.cocoapods.service

import com.tencent.bkrepo.cocoapods.pojo.artifact.CocoapodsArtifactInfo
import com.tencent.bkrepo.cocoapods.utils.PathUtil
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import org.springframework.stereotype.Service

@Service
class CocoapodsFileService(
    private val nodeClient: NodeClient
){
    fun deleteFile(artifactInfo: CocoapodsArtifactInfo) {
        with(artifactInfo){
            //删除包文件:"$orgName/$name/$version/$fileName"
            var request = NodeDeleteRequest(projectId, repoName,
                PathUtil.generateFullPath(artifactInfo),SecurityUtils.getUserId())
            nodeClient.deleteNode(request)
            //删除specs文件:".specs/$name/$version/$name.podspecs"
            request = NodeDeleteRequest(projectId, repoName,
                PathUtil.generateSpecsPath(artifactInfo),SecurityUtils.getUserId())
            nodeClient.deleteNode(request)
        }
    }
}