package com.tencent.bkrepo.common.metadata.util

import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.metadata.model.TNode
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where

object NodeDeleteHelper {
    fun buildCriteria(
        projectId: String,
        repoName: String,
        fullPath: String,
    ): Criteria {
        val normalizedFullPath = PathUtils.normalizeFullPath(fullPath)
        val normalizedPath = PathUtils.toPath(normalizedFullPath)
        val escapedPath = PathUtils.escapeRegex(normalizedPath)
        val criteria = where(TNode::projectId).isEqualTo(projectId)
            .and(TNode::repoName).isEqualTo(repoName)
            .and(TNode::deleted).isEqualTo(null)
            .orOperator(
                where(TNode::fullPath).regex("^$escapedPath"),
                where(TNode::fullPath).isEqualTo(normalizedFullPath)
            )
        return criteria
    }

    fun buildDeleteCriteria(
        projectId: String,
        repoName: String,
        existsPaths: List<String>,
        userAuthPaths: List<String>? = null
    ): Criteria? {
        // 检查路径是否存在及是否可删除
        if (existsPaths.isEmpty() || userAuthPaths?.isEmpty() == true) {
            return null
        }
        val criteria = where(TNode::projectId).isEqualTo(projectId)
            .and(TNode::repoName).isEqualTo(repoName)
            .and(TNode::deleted).isEqualTo(null)
            .and(TNode::folder).isEqualTo(false)

        if (userAuthPaths == null) {
            criteria.orOperator(buildFullPathOrChildrenCriteria(existsPaths))
        } else {
            val userAuthPathCriteria = Criteria().orOperator(buildFullPathOrChildrenCriteria(userAuthPaths))
            val existsPathCriteria = Criteria().orOperator(buildFullPathOrChildrenCriteria(existsPaths))
            criteria.andOperator(userAuthPathCriteria, existsPathCriteria)
        }

        return criteria
    }

    fun buildFullPathOrChildrenCriteria(fullPaths: List<String>): List<Criteria> {
        val normalizedFullPaths = fullPaths.map { PathUtils.normalizeFullPath(it) }
        val orOperation = mutableListOf(
            where(TNode::fullPath).inValues(normalizedFullPaths)
        )
        normalizedFullPaths.forEach {
            val normalizedPath = PathUtils.toPath(it)
            val escapedPath = PathUtils.escapeRegex(normalizedPath)
            orOperation.add(where(TNode::fullPath).regex("^$escapedPath"))
        }
        return orOperation
    }
}
