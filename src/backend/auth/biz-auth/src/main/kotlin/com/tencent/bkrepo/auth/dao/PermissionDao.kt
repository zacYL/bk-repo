package com.tencent.bkrepo.auth.dao

import com.tencent.bkrepo.auth.model.TPermission
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository


@Repository
class PermissionDao : SimpleMongoDao<TPermission>() {
    private fun buildIdQuery(id: String): Query {
        return Query.query(Criteria.where("_id").`is`(id))
    }

    fun findAllByResourceTypeAndPermNameAndUser(
        resourceType: ResourceType,
        permName: String,
        userId: String
    ): List<TPermission> {
        val query = Query.query(
            Criteria().andOperator(
                Criteria.where(TPermission::resourceType.name).`is`(resourceType),
                Criteria.where(TPermission::permName.name).`is`(permName),
                Criteria.where(TPermission::users.name).`is`(userId)
            )
        )
        return this.find(query)
    }

    fun findByIdIn(repoPathCollectionsIds: List<String>): List<TPermission> {
        val query = Query.query(
            Criteria().andOperator(
                Criteria.where("_id").`in`(repoPathCollectionsIds),
            )
        )
        return this.find(query)
    }

    fun findAllByResourceTypeAndPermNameAndRolesIn(
        resourceType: ResourceType,
        permName: String,
        roleIdList: List<String>
    ): List<TPermission> {
        val query = Query.query(
            Criteria().andOperator(
                Criteria.where(TPermission::resourceType.name).`is`(resourceType),
                Criteria.where(TPermission::permName.name).`is`(permName),
                Criteria.where(TPermission::roles.name).`in`(roleIdList)
            )
        )
        return this.find(query)
    }

    fun updateById(id: String, key: String, value: Any): Boolean {
        val update = Update()
        update.set(key, value)
        val query = buildIdQuery(id)
        val result = this.updateFirst(query, update)
        if (result.matchedCount == 1L) return true
        return false
    }

    fun updateById(id: String, update: Update): Boolean {
        val query = buildIdQuery(id)
        val result = this.updateFirst(query, update)
        if (result.matchedCount == 1L) return true
        return false
    }

    fun findFirstById(id: String): TPermission? {
        val query = buildIdQuery(id)
        return this.findOne(query)
    }

    fun deleteById(id: String): Boolean {
        val query = buildIdQuery(id)
        val result = this.remove(query)
        if (result.deletedCount == 1L) return true
        return false
    }

    fun findPermissionByProject(permName: String, projectId: String?, resourceType: ResourceType): TPermission? {
        val query = Query.query(
            Criteria().andOperator(
                Criteria.where(TPermission::permName.name).`is`(permName),
                Criteria.where(TPermission::projectId.name).`is`(projectId),
                Criteria.where(TPermission::resourceType.name).`is`(resourceType.name)
            )
        )
        return this.findOne(query)
    }

    fun findOneByPermName(
        projectId: String,
        repoName: String,
        permName: String,
        resourceType: ResourceType
    ): TPermission? {
        val query = Query.query(
            Criteria().andOperator(
                Criteria.where(TPermission::permName.name).`is`(permName),
                Criteria.where(TPermission::projectId.name).`is`(projectId),
                Criteria.where(TPermission::repos.name).`is`(repoName),
                Criteria.where(TPermission::resourceType.name).`is`(resourceType.name)
            )
        )
        return this.findOne(query)
    }

    fun listByRole(roles: List<String>): List<TPermission> {
        val query = Query(Criteria(TPermission::roles.name).`in`(roles))
        return this.find(query)
    }

    fun listByProjectIdAndUsers(projectId: String, userId: String): List<TPermission> {
        val query = Query.query(
            Criteria().andOperator(
                Criteria.where(TPermission::projectId.name).`is`(projectId),
                Criteria.where(TPermission::users.name).`is`(userId)
            )
        )
        return this.find(query)
    }


    fun listByProjectAndRoles(projectId: String, roles: List<String>): List<TPermission> {
        val query = Query.query(
            Criteria().andOperator(
                Criteria.where(TPermission::projectId.name).`is`(projectId),
                Criteria.where(TPermission::roles.name).`in`(roles)
            )
        )
        return this.find(query)
    }

    fun listByUserId(userId: String): List<TPermission> {
        val query = Query(Criteria(TPermission::users.name).`is`(userId))
        return this.find(query)
    }

    fun listByResourceAndRepo(resourceType: ResourceType, projectId: String, repoName: String): List<TPermission> {
        val query = Query.query(
            Criteria().andOperator(
                Criteria.where(TPermission::projectId.name).`is`(projectId),
                Criteria.where(TPermission::resourceType.name).`is`(resourceType.name),
                Criteria.where(TPermission::repos.name).`is`(repoName)
            )
        )
        return this.find(query)
    }


    fun listByResourceAndProject(resourceType: ResourceType, projectId: String): List<TPermission> {
        val query = Query.query(
            Criteria().andOperator(
                Criteria.where(TPermission::projectId.name).`is`(projectId),
                Criteria.where(TPermission::resourceType.name).`is`(resourceType.name)
            )
        )
        return this.find(query)
    }

    fun listByResourceAndProjectAndRoles(
        resourceType: ResourceType,
        projectId: String,
        roles: List<String>
    ): List<TPermission> {
        val query = Query.query(
            Criteria().andOperator(
                Criteria.where(TPermission::projectId.name).`is`(projectId),
                Criteria.where(TPermission::resourceType.name).`is`(resourceType.name),
                Criteria.where(TPermission::roles.name).`in`(roles)
            )
        )
        return this.find(query)
    }

    fun listByResourceAndProjectAndUserId(
        resourceType: ResourceType,
        projectId: String,
        userId: String
    ): List<TPermission> {
        val query = Query.query(
            Criteria().andOperator(
                Criteria.where(TPermission::projectId.name).`is`(projectId),
                Criteria.where(TPermission::resourceType.name).`is`(resourceType.name),
                Criteria.where(TPermission::users.name).`is`(userId)
            )
        )
        return this.find(query)
    }

    fun listPermissionInRepo(
        projectId: String,
        repoName: String,
        uid: String,
        roles: List<String>
    ): List<TPermission> {
        val criteria = Criteria()
        val celeriac = criteria.orOperator(
            Criteria.where(TPermission::users.name).`is`(uid),
            Criteria.where(TPermission::roles.name).`in`(roles)
        ).and(TPermission::projectId.name).`is`(projectId).and(TPermission::repos.name).`is`(repoName)
        return this.find(Query.query(celeriac))
    }

    fun listPermissionByUserRoles(
        projectId: String,
        uid: String,
        roles: List<String>
    ): List<TPermission> {
        val criteria = Criteria()
        val celeriac = criteria.orOperator(
            Criteria.where(TPermission::users.name).`is`(uid),
            Criteria.where(TPermission::roles.name).`in`(roles)
        ).and(TPermission::projectId.name).`is`(projectId)
        return this.find(Query.query(celeriac))
    }

    fun listInPermission(
        projectId: String,
        repoName: String,
        uid: String,
        resourceType: ResourceType,
        roles: List<String>
    ): List<TPermission> {
        val criteria = Criteria()
        val celeriac = criteria.orOperator(
            Criteria.where(TPermission::users.name).`is`(uid),
            Criteria.where(TPermission::roles.name).`in`(roles)
        ).and(TPermission::resourceType.name).`is`(resourceType.name).and(TPermission::projectId.name).`is`(projectId)
            .and(TPermission::repos.name).`is`(repoName)
        return this.find(Query.query(celeriac))
    }

    fun listNoPermission(
        projectId: String,
        repoName: String,
        uid: String,
        resourceType: ResourceType,
        roles: List<String>
    ): List<TPermission> {
        val criteria = Criteria()
        val celeriac = criteria.andOperator(
            Criteria.where(TPermission::users.name).ne(uid),
            Criteria.where(TPermission::roles.name).nin(roles)
        ).and(TPermission::resourceType.name).`is`(resourceType.name).and(TPermission::projectId.name).`is`(projectId)
            .and(TPermission::repos.name).`is`(repoName)
        return this.find(Query.query(celeriac))
    }
}