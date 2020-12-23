package com.tencent.bkrepo.repository.service.canway.aspect

import com.tencent.bkrepo.auth.api.ServicePermissionResource
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionUserRequest
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.exception.SystemException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.security.exception.AccessDeniedException
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import com.tencent.bkrepo.repository.service.canway.RESOURCECODE
import com.tencent.bkrepo.repository.service.canway.ACCESS
import com.tencent.bkrepo.repository.service.canway.BELONGCODE
import com.tencent.bkrepo.repository.service.canway.CREATE
import com.tencent.bkrepo.repository.service.canway.bk.BkUserService
import com.tencent.bkrepo.repository.service.canway.conf.CanwayAuthConf
import com.tencent.bkrepo.repository.service.canway.exception.CanwayPermissionException
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
import org.springframework.stereotype.Service
import java.lang.Exception

@Aspect
@Service
class CanwayRepositoryAspect(
    canwayAuthConf: CanwayAuthConf
) {

    @Autowired
    lateinit var permissionService: ServicePermissionResource

    @Autowired
    lateinit var canwayPermissionService: CanwayPermissionService

    private val devopsHost = canwayAuthConf.devopsHost!!.removeSuffix("/")

    @Around(value = "execution(* com.tencent.bkrepo.repository.service.impl.RepositoryServiceImpl.createRepo(..))")
    fun beforeCreateRepo(point: ProceedingJoinPoint): Any? {
        val args = point.args
        val repo = args.first() as RepoCreateRequest
        val request = HttpContextHolder.getRequest()
        val userId = try {
            request.getAttribute(USER_KEY)
        } catch (e: Exception) {
            repo.operator
        } ?: throw AccessDeniedException()

        val api = request.requestURI.removePrefix("/").removePrefix("web/")
        if (api.startsWith("api", ignoreCase = true)) {
            if (!canwayPermissionService.checkCanwayPermission(repo.projectId, repo.name, repo.operator, CREATE))
                throw CanwayPermissionException()
        }
        updateResource(repo.projectId, repo.name, repo.operator, ciAddResourceApi)
        try {
            val result =  point.proceed(args)
            addUserIdToAdmin(userId as String, repo.projectId, repo.name)
            return result
        } catch (exception: Exception) {
            if ((exception as ErrorCodeException).messageCode.getKey() == "artifact.repository.existed") return null
            updateResource(repo.projectId, repo.name, repo.operator, ciDeleteResourceApi)
        }
        return null
    }

    private fun addUserIdToAdmin(userId: String, projectId: String, repoName: String) {
        val permissions = permissionService.listRepoBuiltinPermission(projectId, repoName).data?:
        throw SystemException(CommonMessageCode.RESOURCE_NOT_FOUND, "Can not load buildin permission")
        for (permission in permissions) {
            if (permission.permName == "repo_admin") {
                permissionService.updatePermissionUser(
                        UpdatePermissionUserRequest(permission.id!!, listOf(userId))
                )
            }
            return
        }
        throw SystemException(CommonMessageCode.RESOURCE_NOT_FOUND, "Can not load buildin admin permission")
    }

    @Around(value = "execution(* com.tencent.bkrepo.repository.service.impl.RepositoryServiceImpl.listRepo(..))")
    fun afterListRepo(point: ProceedingJoinPoint): Any {
        val args = point.args
        val result = point.proceed(args)
        val projectId = args.first() as String
        val request = HttpContextHolder.getRequest()
        val api = request.requestURI.removePrefix("/").removePrefix("web/")
        if (api.startsWith("api", ignoreCase = true)) {
            if (result != null) {
                val userId = request.getAttribute(USER_KEY) ?: throw AccessDeniedException()
                val resultRepos = mutableListOf<RepositoryInfo>()
                val repoInfos = result as List<RepositoryInfo>
                val canwayPermissionResponse = canwayPermissionService.getCanwayPermissionInstance(
                    projectId = projectId,
                    operator = userId as String,
                    action = ACCESS,
                    belongCode = BELONGCODE,
                    resourceCode = RESOURCECODE
                )
                val hasPermissionRepos = canwayPermissionResponse?.instanceCodes?.first()?.resourceInstance ?: setOf()
                for (repo in repoInfos) {
                    if (hasPermissionRepos.contains(repo.name)) resultRepos.add(repo)
                }
                return resultRepos
            }
            return result
        }
        return result
    }

    @Around(value = "execution(* com.tencent.bkrepo.repository.service.impl.RepositoryServiceImpl.listRepoPage(..))")
    fun afterPageRepo(point: ProceedingJoinPoint): Any {
        val args = point.args
        val result = point.proceed(args)
        val projectId = args.first() as String
        val request = HttpContextHolder.getRequest()
        val api = request.requestURI.removePrefix("/").removePrefix("web/")
        if (api.startsWith("api", ignoreCase = true)) {
            if (result != null) {
                val userId = request.getAttribute(USER_KEY) ?: throw AccessDeniedException()
                val resultRepos = mutableListOf<RepositoryInfo>()
                val page = (result as Page<RepositoryInfo>)
                val repoInfos = page.records
                val canwayPermissionResponse = canwayPermissionService.getCanwayPermissionInstance(
                    projectId = projectId,
                    operator = userId as String,
                    action = ACCESS,
                    belongCode = BELONGCODE,
                    resourceCode = RESOURCECODE
                )
                val hasPermissionRepos = canwayPermissionResponse?.instanceCodes?.first()?.resourceInstance ?: setOf()
                for (repo in repoInfos) {
                    if (hasPermissionRepos.contains(repo.name)) {
                        resultRepos.add(
                            repo.copy(
                                hasPermission = true
                            )
                        )
                    } else {
                        resultRepos.add(repo)
                    }
                }
                return result.copy(
                    records = resultRepos
                )
            }
            return result
        }
        return result
    }

    @Around(value = "execution(* com.tencent.bkrepo.repository.service.impl.RepositoryServiceImpl.deleteRepo(..))")
    fun deleteRepo(point: ProceedingJoinPoint): Any? {
        val args = point.args
        val repo = args.first() as RepoDeleteRequest
        val request = HttpContextHolder.getRequest()
        val api = request.requestURI.removePrefix("/").removePrefix("web/")
        if (api.startsWith("api", ignoreCase = true)) {
            try {
                return point.proceed(args)
            } catch (exception: Exception) {
                if ((exception as ErrorCodeException).messageCode == ArtifactMessageCode.REPOSITORY_NOT_FOUND) return null
                updateResource(repo.projectId, repo.name, repo.operator, ciDeleteResourceApi)
            }
            updateResource(repo.projectId, repo.name, repo.operator, ciDeleteResourceApi)
        }
        return null
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
