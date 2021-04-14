package com.tencent.bkrepo.repository.service.canway.aspect

import com.tencent.bkrepo.auth.api.ServicePermissionResource
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionUserRequest
import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import com.tencent.bkrepo.repository.service.canway.RESOURCECODE
import com.tencent.bkrepo.repository.service.canway.ACCESS
import com.tencent.bkrepo.repository.service.canway.BELONGCODE
import com.tencent.bkrepo.repository.service.canway.CREATE
import com.tencent.bkrepo.repository.service.canway.conf.CanwayDevopsConf
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
class CanwayRepositoryAspect {

    @Autowired
    lateinit var canwayDevopsConf: CanwayDevopsConf

    @Autowired
    lateinit var permissionService: ServicePermissionResource

    @Autowired
    lateinit var canwayPermissionService: CanwayPermissionService

    @Around(value = "execution(* com.tencent.bkrepo.repository.service.repo.impl.RepositoryServiceImpl.createRepo(..))")
    fun beforeCreateRepo(point: ProceedingJoinPoint): Any? {
        val args = point.args
        val repo = args.first() as RepoCreateRequest
        val request = HttpContextHolder.getRequest()
        val requestUserId = request.getAttribute(USER_KEY)?.let { ANONYMOUS_USER }
        val userId = if (ANONYMOUS_USER == requestUserId && repo.operator.isBlank()) {
            throw PermissionException()
        } else repo.operator

        val api = request.requestURI.removePrefix("/").removePrefix("web/")
        if (api.startsWith("api", ignoreCase = true)) {
            if (!canwayPermissionService.checkCanwayPermission(repo.projectId, repo.name, userId, CREATE))
                throw CanwayPermissionException()
        }
        logger.info("userId: $userId  ,   operator: ${repo.operator}")
        updateResource(repo.projectId, repo.name, userId, ciAddResourceApi)
        val result: Any?
        try {
            result = point.proceed(args)
        } catch (exception: Exception) {
            logger.warn("create repo aspect ${exception.message}")
            if (exception is ErrorCodeException && exception.messageCode != ArtifactMessageCode.REPOSITORY_EXISTED) {
                logger.warn("create repo aspect ${exception.message}")
                updateResource(repo.projectId, repo.name, repo.operator, ciDeleteResourceApi)
            }
            if (exception is ErrorCodeException && exception.messageCode == ArtifactMessageCode.REPOSITORY_EXISTED) {
                addUserIdToAdmin(userId, repo.projectId, repo.name)
            }
            throw exception
        }
        addUserIdToAdmin(userId, repo.projectId, repo.name)
        return result
    }

    private fun addUserIdToAdmin(userId: String, projectId: String, repoName: String) {
        val permissions = permissionService.listRepoBuiltinPermission(projectId, repoName).data
            ?: throw NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND, "Can not load buildin permission")
        logger.info("Found permission size: ${permissions.size}")
        for (permission in permissions) {
            logger.info("Current permission: $permission")
            if (permission.permName == "repo_admin") {
                logger.info("Update Permission: $permission User with $userId")
                permissionService.updatePermissionUser(UpdatePermissionUserRequest(permission.id!!, listOf(userId)))
            }
            return
        }
        throw NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND, "Can not load buildin admin permission")
    }

    @Around(value = "execution(* com.tencent.bkrepo.repository.service.repo.impl.RepositoryServiceImpl.listRepo(..))")
    fun afterListRepo(point: ProceedingJoinPoint): Any {
        val args = point.args
        val result = point.proceed(args)
        val projectId = args.first() as String
        val request = HttpContextHolder.getRequest()
        val api = request.requestURI.removePrefix("/").removePrefix("web/")
        if (api.startsWith("api", ignoreCase = true)) {
            result?.let {
                val userId = request.getAttribute(USER_KEY) ?: throw PermissionException()
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
        }
        return result
    }

    @Around(value = "execution(* com.tencent.bkrepo.repository.service.repo.impl.RepositoryServiceImpl.listRepoPage(..))")
    fun afterPageRepo(point: ProceedingJoinPoint): Any {
        val args = point.args
        val result = point.proceed(args)
        val projectId = args.first() as String
        val request = HttpContextHolder.getRequest()
        val api = request.requestURI.removePrefix("/").removePrefix("web/")
        if (api.startsWith("api", ignoreCase = true)) {
            result?.let {
                val userId = request.getAttribute(USER_KEY) ?: throw PermissionException()
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
        }
        return result
    }

    @Around(value = "execution(* com.tencent.bkrepo.repository.service.repo.impl.RepositoryServiceImpl.deleteRepo(..))")
    fun deleteRepo(point: ProceedingJoinPoint) {
        val args = point.args
        val repo = args.first() as RepoDeleteRequest
        updateResource(repo.projectId, repo.name, repo.operator, ciDeleteResourceApi)
        try {
            point.proceed(args)
        } catch (exception: Exception) {
            if (exception is ErrorCodeException) {
                if (exception.messageCode == ArtifactMessageCode.REPOSITORY_NOT_FOUND) {
                    updateResource(repo.projectId, repo.name, repo.operator, ciDeleteResourceApi)
                }
            }
            updateResource(repo.projectId, repo.name, repo.operator, ciAddResourceApi)
        }
    }

    private fun updateResource(projectId: String, repoName: String, operator: String, api: String) {
        val resourceInstance = mutableListOf<BatchResourceInstance.Instance>()
        val userId = if (operator == "anonymous") "admin" else operator
        val resource = ResourceRegisterInfo(repoName, repoName)
        resourceInstance.add(
            BatchResourceInstance.Instance(
                resource.resourceCode, resource.resourceName, null
            )
        )
        val requestParam = BatchResourceInstance(
            userId = userId,
            resourceCode = RESOURCECODE,
            belongCode = BELONGCODE,
            belongInstance = projectId,
            instances = resourceInstance
        )
        val requestParamStr = requestParam.toJsonString()
        val devopsHost = canwayDevopsConf.host
        val ciAddResourceUrl = "${devopsHost.removeSuffix("/")}$ci$api"
        CanwayHttpUtils.doPost(ciAddResourceUrl, requestParamStr).content
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayRepositoryAspect::class.java)
        const val ci = "/ms/permission"
        const val ciAddResourceApi = "/api/service/resource_instance/add"
        const val ciDeleteResourceApi = "/api/service/resource_instance/delete"
    }
}
