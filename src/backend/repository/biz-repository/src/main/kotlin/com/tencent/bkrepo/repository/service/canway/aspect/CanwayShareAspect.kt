package com.tencent.bkrepo.repository.service.canway.aspect

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.exception.ArtifactNotFoundException
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.pojo.share.ShareRecordCreateRequest
import com.tencent.bkrepo.repository.pojo.share.ShareRecordInfo
import com.tencent.bkrepo.repository.service.canway.bk.BkUserService
import com.tencent.bkrepo.repository.service.canway.conf.CanwayDevopsConf
import com.tencent.bkrepo.repository.service.canway.conf.CanwayMailConf
import com.tencent.bkrepo.repository.service.canway.mail.CanwayMailTemplate
import com.tencent.bkrepo.repository.service.canway.pojo.FileShareInfo
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Aspect
@Component
class CanwayShareAspect(
    canwayMailConf: CanwayMailConf,
    canwayDevopsConf: CanwayDevopsConf,
    val bkUserService: BkUserService,
    val mailSender: JavaMailSender,
    val nodeClient: NodeClient
) {

    private val sender = canwayMailConf.username
    private val bkrepoHost = canwayDevopsConf.host

    @Around("execution(* com.tencent.bkrepo.repository.service.impl.ShareServiceImpl.create(..))")
    fun sendMail(point: ProceedingJoinPoint): Any {
        val args = point.args
        val userId = args.first() as String
        // 获取用户中文名
        val senderBk = bkUserService.getBkUserByUserId(userId)
        val chname = senderBk.chname ?: senderBk.bk_username
        val artifactInfo = args[1] as ArtifactInfo
        val node = nodeClient.getNodeDetail(
            artifactInfo.projectId, artifactInfo.repoName,
            artifactInfo.getArtifactFullPath()
        ).data ?: throw ArtifactNotFoundException("Can not found artifact")
        val shareRecordCreateRequest = args[2] as ShareRecordCreateRequest
        val expireDays = shareRecordCreateRequest.expireSeconds / 86400
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
                    md5 = node.md5 ?: "Can not found md5 value",
                    projectId = artifactInfo.projectId,
                    repoName = artifactInfo.repoName,
                    downloadUrl = downloadUrl,
                    qrCodeBase64 = if (fileName.endsWith("apk") || fileName.endsWith("ipa")) {
                        CanwayMailTemplate.getQRCodeBase64(downloadUrl)
                    } else "null"
                )
                val shareUsers = shareRecordCreateRequest.authorizedUserList
                val receivers = mutableSetOf<String>()
                for (user in shareUsers) {
                    // 查询蓝鲸用户信息
                    val userData = bkUserService.getBkUserByUserId(user)
                    userData.email?.let { receivers.add(it) }
                }
                sendMimeMail(chname, fileShareInfo, receivers.toTypedArray(), expireDays.toInt())
            }
        } catch (e: Exception) {
            logger.error(e.message)
        }
        return result
    }

    private fun sendMimeMail(userId: String, file: FileShareInfo, email: Array<String>, days: Int) {
        val mailMessage = mailSender.createMimeMessage()
        val mimeMailMessage = MimeMessageHelper(mailMessage, true)
        mimeMailMessage.setFrom(sender)
        mimeMailMessage.setTo(email)
        val title = CanwayMailTemplate.getShareEmailTitle(userId, file.fileName)
        mimeMailMessage.setSubject(title)
        val body = CanwayMailTemplate.getShareEmailBody(file.projectId, title, userId, days, listOf(file))
        mimeMailMessage.setText(body, true)
        mailSender.send(mailMessage)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayShareAspect::class.java)
    }
}
