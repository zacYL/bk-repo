package com.tencent.bkrepo.common.metadata.service.node.impl

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.util.concurrent.UncheckedExecutionException
import com.tencent.bkrepo.auth.api.ServicePermissionClient
import com.tencent.bkrepo.common.metadata.condition.SyncCondition
import com.tencent.bkrepo.common.metadata.service.node.NodePermissionService
import com.tencent.bkrepo.common.service.cluster.condition.DefaultCondition
import com.tencent.bkrepo.repository.pojo.node.UserAuthPathOption
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
@Conditional(SyncCondition::class, DefaultCondition::class)
class NodePermissionServiceImpl(
    private val permissionClient: ServicePermissionClient,
) : NodePermissionService {

    private val userAuthPathCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .refreshAfterWrite(30L, TimeUnit.SECONDS)
        .expireAfterWrite(60L, TimeUnit.SECONDS)
        .build(CacheLoader.from<UserAuthPathOption, Map<String, List<String>>> { getUserAuthPath(it) })

    override fun getUserAuthPathCache(option: UserAuthPathOption): Map<String, List<String>> {
        return try {
            userAuthPathCache.get(option)
        } catch (e: UncheckedExecutionException) {
            throw e.cause ?: e
        }
    }

    private fun getUserAuthPath(option: UserAuthPathOption): Map<String, List<String>> {
        with(option) {
            return permissionClient.getAuthRepoPaths(userId, projectId, repoNames, action).data ?: emptyMap()
        }
    }
}
