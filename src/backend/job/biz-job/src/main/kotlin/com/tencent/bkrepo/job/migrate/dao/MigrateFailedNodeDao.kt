/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2024 Tencent.  All rights reserved.
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

package com.tencent.bkrepo.job.migrate.dao

import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import com.tencent.bkrepo.common.mongo.constant.MIN_OBJECT_ID
import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import com.tencent.bkrepo.job.migrate.Constant.MAX_MIGRATE_FAILED_RETRY_TIMES
import com.tencent.bkrepo.job.migrate.model.TMigrateFailedNode
import org.bson.types.ObjectId
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class MigrateFailedNodeDao : SimpleMongoDao<TMigrateFailedNode>() {
    fun page(projectId: String, repoName: String, pageRequest: PageRequest): List<TMigrateFailedNode> {
        val criteria = buildCriteria(projectId, repoName)
        return find(Query(criteria).with(pageRequest))
    }

    fun iterate(
        projectId: String?,
        repoName: String?,
        fullPath: String?,
        consumer: (failedNode: TMigrateFailedNode) -> Unit
    ) {
        val criteria = Criteria()
        projectId?.let { criteria.and(TMigrateFailedNode::projectId.name).isEqualTo(it) }
        repoName?.let { criteria.and(TMigrateFailedNode::repoName.name).isEqualTo(it) }
        fullPath?.let { criteria.and(TMigrateFailedNode::fullPath.name).isEqualTo(it) }
        val query = Query(criteria)

        var lastId = ObjectId(MIN_OBJECT_ID)
        do {
            val newQuery = Query.of(query)
                .addCriteria(Criteria.where(ID).gt(lastId))
                .limit(DEFAULT_BATCH_SIZE)
                .with(Sort.by(ID).ascending())
            val data = find(newQuery)
            if (data.isEmpty()) {
                break
            }
            data.forEach { consumer(it) }
            lastId = ObjectId(data.last().id!!)
        } while (data.size == DEFAULT_BATCH_SIZE)
    }

    fun findOneToRetry(
        projectId: String,
        repoName: String,
        maxRetryTimes: Int = MAX_MIGRATE_FAILED_RETRY_TIMES
    ): TMigrateFailedNode? {
        val criteria = buildCriteria(projectId, repoName)
            .and(TMigrateFailedNode::retryTimes.name).lt(maxRetryTimes)
            .and(TMigrateFailedNode::migrating.name).isEqualTo(false)
        val update = Update()
            .inc(TMigrateFailedNode::retryTimes.name, 1)
            .set(TMigrateFailedNode::lastModifiedDate.name, LocalDateTime.now())
            .set(TMigrateFailedNode::migrating.name, true)
        val query = Query(criteria).with(Sort.by(Sort.Order.asc(TMigrateFailedNode::retryTimes.name)))
        return findAndModify(query, update, FindAndModifyOptions().returnNew(true), TMigrateFailedNode::class.java)
    }

    fun existsFailedNode(projectId: String, repoName: String): Boolean {
        val criteria = buildCriteria(projectId, repoName)
        return exists(Query(criteria))
    }

    fun existsFailedNode(nodeId: String): Boolean {
        return exists(Query(TMigrateFailedNode::nodeId.isEqualTo(nodeId)))
    }

    fun existsRetryableNode(
        projectId: String,
        repoName: String,
        maxRetryTimes: Int = MAX_MIGRATE_FAILED_RETRY_TIMES
    ): Boolean {
        val criteria = buildCriteria(projectId, repoName)
            .and(TMigrateFailedNode::retryTimes.name).lt(maxRetryTimes)
            .and(TMigrateFailedNode::migrating.name).isEqualTo(false)
        return exists(Query(criteria))
    }

    fun resetMigrating(failedNodeId: String) {
        updateFirst(
            Query(Criteria.where(ID).isEqualTo(failedNodeId)),
            Update.update(TMigrateFailedNode::migrating.name, false)
        )
    }

    fun resetRetryCount(projectId: String, repoName: String): UpdateResult {
        val update = Update.update(TMigrateFailedNode::retryTimes.name, 0)
        return updateMulti(Query(buildCriteria(projectId, repoName)), update)
    }

    fun resetRetryCount(failedNodeId: String?): UpdateResult {
        val update = Update.update(TMigrateFailedNode::retryTimes.name, 0)
        return updateFirst(Query(Criteria.where(ID).isEqualTo(failedNodeId)), update)
    }

    fun remove(projectId: String, repoName: String): DeleteResult {
        val criteria = buildCriteria(projectId, repoName)
        return remove(Query(criteria))
    }

    fun remove(failedNodeId: String): DeleteResult {
        return remove(Query(Criteria.where(ID).isEqualTo(failedNodeId)))
    }

    private fun buildCriteria(projectId: String, repoName: String): Criteria {
        val criteria = Criteria
            .where(TMigrateFailedNode::projectId.name).isEqualTo(projectId)
            .and(TMigrateFailedNode::repoName.name).isEqualTo(repoName)
        return criteria
    }

    companion object {
        private const val DEFAULT_BATCH_SIZE = 20
    }
}
