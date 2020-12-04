package com.tencent.bkrepo.repository.service.canway

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.service.util.HttpUtils
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoDeleteRequest
import com.tencent.bkrepo.repository.service.canway.bk.BkUserService
import com.tencent.bkrepo.repository.service.canway.conf.CanwayAuthConf
import com.tencent.bkrepo.repository.service.canway.pojo.BatchResourceInstance
import com.tencent.bkrepo.repository.service.canway.pojo.ResourceRegisterInfo
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
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

    private val devopsHost = canwayAuthConf.devopsHost!!.removeSuffix("/")

    @Around(value="execution(* com.tencent.bkrepo.repository.service.impl.RepositoryServiceImpl.createRepo(..))")
    fun beforeCreateRepo(point: ProceedingJoinPoint) {
        val args = point.args
        val repo = args.first() as RepoCreateRequest
        updateResource(repo.projectId, repo.name, repo.operator, ciAddResourceApi)
        try {
            val result = point.proceed(args)
        } catch (exception: Exception) {
            if ((exception as ErrorCodeException).messageCode.getKey() == "artifact.repository.existed") return
            updateResource(repo.projectId, repo.name, repo.operator, ciDeleteResourceApi)
        }
    }

    @Around(value="execution(* com.tencent.bkrepo.repository.service.impl.RepositoryServiceImpl.deleteRepo(..))")
    fun afterDeleteRepo(point: ProceedingJoinPoint) {
        val args = point.args
        val repo = args.first() as RepoDeleteRequest
        try {
            val result = point.proceed(args)
        } catch (exception: Exception) {
            if((exception as ErrorCodeException).messageCode.getKey() == "artifact.repository.notfound") return
            updateResource(repo.projectId, repo.name, repo.operator, ciDeleteResourceApi)
        }
        updateResource(repo.projectId, repo.name, repo.operator, ciDeleteResourceApi)

    }

    private fun updateResource(project: String, repo: String, operator: String, api: String) {
        val resourceInstance = mutableListOf<BatchResourceInstance.Instance>()
        val userId = if(operator == "anonymous") "admin" else operator
        val resource = ResourceRegisterInfo(repo, repo)
        resourceInstance.add(BatchResourceInstance.Instance(resource.resourceCode, resource.resourceName, null))
        val requestParam = BatchResourceInstance(
                userId = userId,
                resourceCode = resourceCode,
                belongCode = belongCode,
                belongInstance = project,
                instances = resourceInstance
        )
        val requestParamStr = requestParam.toJsonString()
        val body = RequestBody.create(mediaType, requestParamStr)
        val ciAddResourceUrl = "$devopsHost$ci$api"
        val request = Request.Builder()
                .url(ciAddResourceUrl)
                .post(body)
                .build()
        HttpUtils.doRequest(OkHttpClient(), request, 3, mutableSetOf(200)).content
    }

    companion object{
        val logger: Logger = LoggerFactory.getLogger(CanwayRepositoryAspect::class.java)
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        const val ci = "/ms/permission"
        const val ciAddResourceApi = "/api/service/resource_instance/add"
        const val ciDeleteResourceApi = "/api/service/resource_instance/delete"
        const val resourceCode = "bkrepo"
        const val belongCode = "project"
    }
}