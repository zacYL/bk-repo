package com.tencent.bkrepo.repository.service.canway.aspect

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.security.exception.AccessDeniedException
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.service.canway.ACCESS
import com.tencent.bkrepo.repository.service.canway.CREATE
import com.tencent.bkrepo.repository.service.canway.bk.BkUserService
import com.tencent.bkrepo.repository.service.canway.conf.CanwayAuthConf
import com.tencent.bkrepo.repository.service.canway.service.CanwayPermissionService
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.Exception

@Aspect
@Service
class CanwayPackageAspect(
        val bkUserService: BkUserService
) {

    @Autowired
    lateinit var canwayPermissionService: CanwayPermissionService

    @Around(value = "execution(* com.tencent.bkrepo.repository.service.impl.PackageServiceImpl.listPackagePage(..))")
    fun beforeCreateRepo(point: ProceedingJoinPoint) {
        val args = point.args
        val projectId = args.first() as String
        val repoName = args[1] as String
        val userId = bkUserService.getBkUser()
        if (!canwayPermissionService.checkCanwayPermission(projectId, repoName, userId, ACCESS))
            throw AccessDeniedException()
        point.proceed(args)
    }
}