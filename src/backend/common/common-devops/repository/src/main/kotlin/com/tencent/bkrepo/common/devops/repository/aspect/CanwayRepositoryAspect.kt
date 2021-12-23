package com.tencent.bkrepo.common.devops.repository.aspect

import com.tencent.bkrepo.auth.constant.BK_SOFTWARE
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.devops.api.BELONGCODE
import com.tencent.bkrepo.common.devops.api.RESOURCECODE
import com.tencent.bkrepo.common.devops.api.conf.DevopsConf
import com.tencent.bkrepo.common.devops.api.enums.CanwayPermissionType
import com.tencent.bkrepo.common.devops.api.exception.CanwayPermissionException
import com.tencent.bkrepo.common.devops.api.pojo.BatchResourceInstance
import com.tencent.bkrepo.common.devops.api.pojo.ResourceRegisterInfo
import com.tencent.bkrepo.common.devops.api.util.http.CanwayHttpUtils
import com.tencent.bkrepo.common.devops.repository.TENANTID
import com.tencent.bkrepo.common.devops.repository.service.CanwayPermissionService
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoDeleteRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@Aspect
class CanwayRepositoryAspect(
    devopsConf: DevopsConf
) {

    @Autowired
    lateinit var canwayPermissionService: CanwayPermissionService

    private val devopsHost = devopsConf.devopsHost.removeSuffix("/")

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    @Around(value = "execution(* com.tencent.bkrepo.repository.service.repo.impl.RepositoryServiceImpl.createRepo(..))")
    fun beforeCreateRepo(point: ProceedingJoinPoint): Any? {
        val args = point.args
        val repo = args.first() as RepoCreateRequest
        // 兼容处理
        val tenantId = try {
            val tenant = HttpContextHolder.getRequest().getHeader(TENANTID)
            if (tenant.isNullOrBlank()) "bk_ci" else tenant
        } catch (e: Exception) {
            "bk_ci"
        }
        logger.info("tenantId: [$tenantId]")
        val projectId = repo.projectId
        if (projectId == BK_SOFTWARE) {
            return point.proceed(args)
        } else {
            val userId = repo.operator
            if (!canwayPermissionService.checkCanwayPermission(
                    repo.projectId, repo.name, userId, CanwayPermissionType.CREATE, tenantId
                )
            ) throw CanwayPermissionException()
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
                throw exception
            }
            return result
        }
    }

    @Around(value = "execution(* com.tencent.bkrepo.repository.service.repo.impl.RepositoryServiceImpl.deleteRepo(..))")
    fun deleteRepo(point: ProceedingJoinPoint) {
        val args = point.args
        val repo = args.first() as RepoDeleteRequest
        if (repo.projectId == BK_SOFTWARE) {
            point.proceed(args)
        } else {
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
