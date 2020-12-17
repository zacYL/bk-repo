package com.tencent.bkrepo.repository.service.canway.aspect

import com.tencent.bkrepo.repository.pojo.share.ShareRecordCreateRequest
import com.tencent.bkrepo.repository.pojo.share.ShareRecordInfo
import com.tencent.bkrepo.repository.service.canway.bk.BkUserService
import com.tencent.bkrepo.repository.service.canway.conf.CanwayMailConf
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Aspect
@Component
class CanwayShareAspect(
    canwayMailConf: CanwayMailConf,
    val bkUserService: BkUserService,
    val mailSender: JavaMailSender
) {

    private val sender = canwayMailConf.mailUsername
    private val bkrepoHost = canwayMailConf.bkrepoHost

    @Around("execution(* com.tencent.bkrepo.repository.service.impl.ShareServiceImpl.create(..))")
    fun sendMail(point: ProceedingJoinPoint) {
        val args = point.args
        val userId = args.first() as String
        val request = args[2] as ShareRecordCreateRequest
        val result = point.proceed(args)
        try {
            if (result != null) {
                val shareRecordInfo = result as ShareRecordInfo
                val downloadUrl = with(shareRecordInfo) {
                    "$bkrepoHost$shareUrl"
                }
                val shareUsers = request.authorizedUserList
                val receivers = mutableSetOf<String>()
                for (user in shareUsers) {
                    // 查询蓝鲸用户信息
                    val userData = bkUserService.getBkUserByUserId(userId)
                    userData.email?.let { receivers.add(it) }
                }
                sendSimpleMail(receivers.toTypedArray(), downloadUrl)
            }
        } catch (e: Exception) {
            logger.error(e.message)
        }
    }

    private fun sendSimpleMail(email: Array<String>, content: String) {
        val simpleMailMessage = SimpleMailMessage()
        simpleMailMessage.setFrom(sender)
        simpleMailMessage.setTo(*email)
        simpleMailMessage.setSubject("文件分享")
        simpleMailMessage.setText(content)
        mailSender.send(simpleMailMessage)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayShareAspect::class.java)
    }
}
