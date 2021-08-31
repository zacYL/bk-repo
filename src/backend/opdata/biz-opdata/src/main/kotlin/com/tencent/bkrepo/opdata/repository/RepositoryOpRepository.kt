package com.tencent.bkrepo.opdata.repository

import com.tencent.bkrepo.opdata.model.TRepositoryOp
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface RepositoryOpRepository : MongoRepository<TRepositoryOp, String> {
    fun findByProjectIdAndRepoName(projectId: String, repoName: String): TRepositoryOp?
}
