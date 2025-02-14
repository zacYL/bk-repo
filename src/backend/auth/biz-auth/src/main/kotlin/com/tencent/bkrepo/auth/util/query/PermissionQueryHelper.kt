package com.tencent.bkrepo.auth.util.query

import com.tencent.bkrepo.auth.model.TPermission
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

object PermissionQueryHelper {

    fun buildPermissionCheck(
        projectId: String,
        repoName: String,
        uid: String,
        action: PermissionAction,
        resourceType: ResourceType,
        roles: List<String>,
        departments: List<String>? = null
    ): Query {
        val criteria = Criteria()
        // userid roles departments 三者之间是或的关系
        val orUserQuery = mutableListOf<Criteria>().apply {
            add(Criteria.where(TPermission::users.name).`in`(uid))
            if (roles.isNotEmpty()) add(Criteria.where(TPermission::roles.name).`in`(roles))
            if (!departments.isNullOrEmpty()) add(Criteria.where(TPermission::departments.name).`in`(departments))
        }
        var celeriac = criteria.orOperator(*orUserQuery.toTypedArray())
            .and(TPermission::resourceType.name).`is`(resourceType)
            .and(TPermission::actions.name).`in`(action)
        if (resourceType != ResourceType.SYSTEM) {
            celeriac = celeriac.and(TPermission::projectId.name).`is`(projectId)
        }
        if (resourceType == ResourceType.REPO) {
            celeriac = celeriac.and(TPermission::repos.name).`is`(repoName)
        }
        return Query(celeriac)
    }

    fun buildProjectPermissionCheck(
        projectId: String,
        uid: String,
        action: PermissionAction,
        roles: List<String>
    ): Query {
        val criteria = Criteria()
        val celeriac = criteria.orOperator(
            Criteria.where(TPermission::users.name).`in`(uid),
            Criteria.where(TPermission::roles.name).`in`(roles)
        ).and(TPermission::resourceType.name).`is`(ResourceType.PROJECT)
            .and(TPermission::actions.name).`in`(action)
            .and(TPermission::projectId.name).`is`(projectId)
        return Query(celeriac)
    }

    fun buildProjectUserCheck(
        projectId: String,
        uid: String,
        roles: List<String>
    ): Query {
        val criteria = Criteria()
        val celeriac = criteria.orOperator(
            Criteria.where(TPermission::users.name).`in`(uid),
            Criteria.where(TPermission::roles.name).`in`(roles)
        ).and(TPermission::resourceType.name).`is`(ResourceType.PROJECT)
            .and(TPermission::projectId.name).`is`(projectId)
        return Query(celeriac)
    }
}
