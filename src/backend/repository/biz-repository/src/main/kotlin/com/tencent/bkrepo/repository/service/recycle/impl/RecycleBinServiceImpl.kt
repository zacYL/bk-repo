package com.tencent.bkrepo.repository.service.recycle.impl

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.constant.EXPIRED_DELETED_NODE
import com.tencent.bkrepo.common.artifact.constant.ROOT_DELETED_NODE
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.model.TMetadata
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.service.recycle.RecycleBinService
import com.tencent.bkrepo.repository.util.NodeQueryHelper.nodeTreeCriteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId

@Service
class RecycleBinServiceImpl(
    private val nodeDao: NodeDao
) : RecycleBinService {
    override fun irreversibleDelete(artifactInfo: ArtifactInfo, deletedId: Long) {
        with(artifactInfo) {
            val deletedTime = Instant.ofEpochMilli(deletedId).atZone(ZoneId.systemDefault()).toLocalDateTime()
            nodeDao.updateMulti(
                Query(nodeTreeCriteria(projectId, repoName, getArtifactFullPath(), deletedTime)),
                Update().pull(TNode::metadata.name, Query(where(TMetadata::key).isEqualTo(ROOT_DELETED_NODE)))
                    .push(
                        TNode::metadata.name,
                        TMetadata(key = EXPIRED_DELETED_NODE, value = true, system = true, display = false)
                    )
            )
        }
    }
}
