package com.tencent.bkrepo.repository.service.canway.aspect

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.security.exception.AccessDeniedException
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoDeleteRequest
import com.tencent.bkrepo.repository.service.canway.*
import com.tencent.bkrepo.repository.service.canway.conf.CanwayAuthConf
import com.tencent.bkrepo.repository.service.canway.http.CanwayHttpUtils
import com.tencent.bkrepo.repository.service.canway.pojo.BatchResourceInstance
import com.tencent.bkrepo.repository.service.canway.pojo.ResourceRegisterInfo
import com.tencent.bkrepo.repository.service.canway.service.CanwayPermissionService
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.Exception

@Aspect
@Component
class CanwayRepositoryAspect(
    canwayAuthConf: CanwayAuthConf
) {

    @Autowired
    lateinit var canwayPermissionService: CanwayPermissionService

    private val devopsHost = canwayAuthConf.devopsHost!!.removeSuffix("/")

    @Around(value = "execution(* com.tencent.bkrepo.repository.service.impl.RepositoryServiceImpl.createRepo(..))")
    fun beforeCreateRepo(point: ProceedingJoinPoint) {
        val args = point.args
        val repo = args.first() as RepoCreateRequest
        if (!canwayPermissionService.checkCanwayPermission(repo.projectId, repo.name, repo.operator, CREATE))
            throw AccessDeniedException()
        updateResource(repo.projectId, repo.name, repo.operator, ciAddResourceApi)
        try {
            point.proceed(args)
        } catch (exception: Exception) {
            if ((exception as ErrorCodeException).messageCode.getKey() == "artifact.repository.existed") return
            updateResource(repo.projectId, repo.name, repo.operator, ciDeleteResourceApi)
        }
        // todo
        // checkResult(result, repo)
    }

    @Around(value = "execution(* com.tencent.bkrepo.repository.service.impl.RepositoryServiceImpl.deleteRepo(..))")
    fun deleteRepo(point: ProceedingJoinPoint) {
        val args = point.args
        val repo = args.first() as RepoDeleteRequest
        if (!canwayPermissionService.checkCanwayPermission(repo.projectId, repo.name, repo.operator, DELETE))
            throw AccessDeniedException()
        try {
            point.proceed(args)
        } catch (exception: Exception) {
            if ((exception as ErrorCodeException).messageCode.getKey() == "artifact.repository.notfound") return
            updateResource(repo.projectId, repo.name, repo.operator, ciDeleteResourceApi)
        }
        updateResource(repo.projectId, repo.name, repo.operator, ciDeleteResourceApi)
    }

    @Around(value = "execution(* com.tencent.bkrepo.repository.service.impl.RepositoryServiceImpl.updateRepo(..))")
    fun beforeRepo(point: ProceedingJoinPoint) {
        val args = point.args
        val repo = args.first() as RepoDeleteRequest
        if (!canwayPermissionService.checkCanwayPermission(repo.projectId, repo.name, repo.operator, MANAGE)) throw AccessDeniedException()
        point.proceed(args)
    }

    private fun updateResource(projectId: String, repoName: String, operator: String, api: String) {
        val resourceInstance = mutableListOf<BatchResourceInstance.Instance>()
        val userId = if (operator == "anonymous") "admin" else operator
        val resource = ResourceRegisterInfo(repoName, repoName)
        resourceInstance.add(BatchResourceInstance.Instance(resource.resourceCode, resource.resourceName, null))
        val requestParam = BatchResourceInstance(
            userId = userId,
            resourceCode = RESOURCECODE,
            belongCode = BELONGCODE,
            belongInstance = projectId,
            instances = resourceInstance
        )
        val requestParamStr = requestParam.toJsonString()
        val ciAddResourceUrl = "$devopsHost$ci$api"
        CanwayHttpUtils.doPost(ciAddResourceUrl, requestParamStr).content
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayRepositoryAspect::class.java)
        const val ci = "/ms/permission"
        const val ciAddResourceApi = "/api/service/resource_instance/add"
        const val ciDeleteResourceApi = "/api/service/resource_instance/delete"
    }
}
