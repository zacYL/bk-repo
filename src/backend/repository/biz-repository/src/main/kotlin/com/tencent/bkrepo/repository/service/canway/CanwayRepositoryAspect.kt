package com.tencent.bkrepo.repository.service.canway

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.service.util.HttpUtils
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
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

    @Autowired
    lateinit var bkUserService: BkUserService

    private val devopsHost = canwayAuthConf.devopsHost!!.removeSuffix("/")

    @Around(value="execution(* com.tencent.bkrepo.repository.service.impl.RepositoryServiceImpl.createRepo(..))")
    fun beforeCreateRepo(point: ProceedingJoinPoint) {
        val args = point.args
        val repo = args.first() as RepoCreateRequest
        updateResource(repo, ciAddResourceApi)
        try {
            val result = point.proceed(args)
        } catch (exception: Exception) {
            if((exception as ErrorCodeException).messageCode.getKey() == "artifact.repository.existed") return
            updateResource(repo, ciDeleteResourceApi)
        }
        //todo
        //checkResult(result, repo)
    }

    private fun checkResult(result: Any, repo: RepoCreateRequest) {
        //添加失败，删除实例
        //
        updateResource(repo, ciDeleteResourceApi)
    }


    private fun updateResource(repo: RepoCreateRequest, api: String) {
        val resourceInstance = mutableListOf<BatchResourceInstance.Instance>()
        val userId = if(repo.operator == "anonymous") "admin" else repo.operator
        val belongInstance = repo.projectId
        val resource = ResourceRegisterInfo(repo.name, repo.name)
        resourceInstance.add(BatchResourceInstance.Instance(resource.resourceCode, resource.resourceName, null))
        val requestParam = BatchResourceInstance(
                //todo
                userId = userId,
                resourceCode = resourceCode,
                belongCode = belongCode,
                belongInstance = belongInstance,
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