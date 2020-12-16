package com.tencent.bkrepo.repository.service.canway.aspect

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect

//@Aspect
//class CanwayShareAspect {
//
//    @Around("execution(* com.tencent.bkrepo.repository.service.impl.ShareServiceImpl.create(..))")
//    fun sendMail(point: ProceedingJoinPoint) {
//        val args = point.args
//        val userId = args.first() as String
//        val artifactInfo = args[1] as ArtifactInfo
//        val request = args[2] as RepoCreateRequest
//    }
//}
