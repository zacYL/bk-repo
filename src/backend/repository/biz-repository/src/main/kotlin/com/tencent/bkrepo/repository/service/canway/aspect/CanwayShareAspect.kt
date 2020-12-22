package com.tencent.bkrepo.repository.service.canway.aspect

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.pojo.share.ShareRecordCreateRequest
import com.tencent.bkrepo.repository.pojo.share.ShareRecordInfo
import com.tencent.bkrepo.repository.service.canway.bk.BkUserService
import com.tencent.bkrepo.repository.service.canway.conf.CanwayMailConf
import com.tencent.bkrepo.repository.service.canway.exception.CanwayPermissionException
import com.tencent.bkrepo.repository.service.canway.mail.CanwayMailTemplate
import com.tencent.bkrepo.repository.service.canway.pojo.FileShareInfo
import com.tencent.bkrepo.repository.service.canway.service.CanwayPermissionService
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
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

    @Autowired
    lateinit var canwayPermissionService: CanwayPermissionService

    @Around("execution(* com.tencent.bkrepo.repository.service.impl.ShareServiceImpl.create(..))")
    fun sendMail(point: ProceedingJoinPoint): Any {
        val args = point.args
        val userId = args.first() as String
        val artifactInfo = args[1] as ArtifactInfo
        val shareRecordCreateRequest = args[2] as ShareRecordCreateRequest
        val fileName = artifactInfo.getArtifactFullPath().split("/").last()
        val result = point.proceed(args)
        try {
            if (result != null) {
                val shareRecordInfo = result as ShareRecordInfo
                val downloadUrl = with(shareRecordInfo) {
                    "$bkrepoHost$shareUrl"
                }
                val fileShareInfo = FileShareInfo(
                    fileName = fileName,
                    // todo
                    md5 = "33",
                    projectId = artifactInfo.projectId,
                    repoName = artifactInfo.repoName,
                    downloadUrl = downloadUrl,
                    // todo
                    qrCodeBase64 = "ddd"
                )
                val shareUsers = shareRecordCreateRequest.authorizedUserList
                val receivers = mutableSetOf<String>()
                for (user in shareUsers) {
                    // 查询蓝鲸用户信息
                    val userData = bkUserService.getBkUserByUserId(userId)
                    userData.email?.let { receivers.add(it) }
                }
                sendMimeMail(userId, fileShareInfo, receivers.toTypedArray())
            }
        } catch (e: Exception) {
            logger.error(e.message)
        }
        return result
    }

    private fun sendSimpleMail(email: Array<String>, content: String) {
        val simpleMailMessage = SimpleMailMessage()
        simpleMailMessage.setFrom(sender)
        simpleMailMessage.setTo(*email)
        simpleMailMessage.setSubject("文件分享")
        simpleMailMessage.setText(content)
        mailSender.send(simpleMailMessage)
    }

    private fun sendMimeMail(userId: String, file: FileShareInfo, email: Array<String>) {
        val mailMessage = mailSender.createMimeMessage()
        val mimeMailMessage = MimeMessageHelper(mailMessage, true)
        mimeMailMessage.setFrom(sender)
        mimeMailMessage.setTo(email)
        val title = CanwayMailTemplate.getShareEmailTitle(userId, file.fileName)
        mimeMailMessage.setSubject(title)
        val body = CanwayMailTemplate.getShareEmailBody(file.projectId, title, userId, 1, listOf(file))
        mimeMailMessage.setText(body)
        mailSender.send(mailMessage)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayShareAspect::class.java)
    }
}
