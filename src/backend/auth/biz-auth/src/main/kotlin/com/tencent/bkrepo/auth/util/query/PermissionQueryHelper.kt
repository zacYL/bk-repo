package com.tencent.bkrepo.auth.util.query

import com.tencent.bkrepo.auth.model.TPermission
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

object PermissionQueryHelper {

    fun buildPermissionCheck(
        projectId: String,
        repoName: String,
        uid: String,
        action: String,
        resourceType: String,
        roles: List<String>
    ): Query {
        val criteria = Criteria()
        var celeriac = criteria.orOperator(
            Criteria.where(TPermission::users.name).`in`(uid),
            Criteria.where(TPermission::roles.name).`in`(roles)
        ).and(TPermission::resourceType.name).`is`(resourceType).and(TPermission::actions.name)
            .`in`(action.toString())
        if (resourceType != ResourceType.SYSTEM.toString()) {
            celeriac = celeriac.and(TPermission::projectId.name).`is`(projectId)
        }
        if (resourceType == ResourceType.REPO.toString()) {
            celeriac = celeriac.and(TPermission::repos.name).`is`(repoName)
        }
        return Query(celeriac)
    }
}
