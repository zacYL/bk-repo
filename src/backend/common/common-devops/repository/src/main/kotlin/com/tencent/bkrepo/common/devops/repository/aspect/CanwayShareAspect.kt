package com.tencent.bkrepo.common.devops.repository.aspect

import com.tencent.bkrepo.auth.api.ServiceUserResource
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.exception.ArtifactNotFoundException
import com.tencent.bkrepo.common.devops.api.conf.DevopsConf
import com.tencent.bkrepo.common.devops.api.conf.CanwayMailConf
import com.tencent.bkrepo.common.devops.api.pojo.BkMailMessage
import com.tencent.bkrepo.common.devops.api.pojo.DevopsMailMessage
import com.tencent.bkrepo.common.devops.repository.mail.CanwayMailTemplate
import com.tencent.bkrepo.common.devops.api.pojo.FileShareInfo
import com.tencent.bkrepo.common.devops.api.service.BkUserService
import com.tencent.bkrepo.common.devops.api.util.http.CanwayHttpUtils
import com.tencent.bkrepo.common.devops.api.repository
import com.tencent.bkrepo.common.devops.api.web
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.pojo.share.ShareRecordCreateRequest
import com.tencent.bkrepo.repository.pojo.share.ShareRecordInfo
import org.apache.commons.lang.StringUtils
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper

@Aspect
class CanwayShareAspect(
    canwayMailConf: CanwayMailConf,
    devopsConf: DevopsConf,
    private val bkUserService: BkUserService,
    private val mailSender: JavaMailSender,
    private val nodeClient: NodeClient,
    private val userClient: ServiceUserResource
) {

    private val sender = canwayMailConf.username
    private val bkrepoHost = devopsConf.bkrepoHost
    private val appCode = devopsConf.appCode
    private val appSecret = devopsConf.appSecret
    private val bkHost = devopsConf.bkHost
    private val devopsHost = devopsConf.devopsHost

    @Value("\${deploy.mode:devops}")
    lateinit var mailMode: String

    @Suppress("TooGenericExceptionCaught", "NestedBlockDepth")
    @Around("execution(* com.tencent.bkrepo.repository.service.file.impl.ShareServiceImpl.create(..))")
    fun sendMail(point: ProceedingJoinPoint): Any {
        val args = point.args
        val userId = args.first() as String
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
                    "${bkrepoHost.removeSuffix("/")}$web$repository$shareUrl"
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
                if (mailMode == "devops") {
                    devopsMailMessage(userId, fileShareInfo, shareUsers, expireDays.toInt())
                } else {
                    val receivers = mutableSetOf<String>()
                    for (user in shareUsers) {
                        // 查询蓝鲸用户信息
                        val userData = bkUserService.getBkUserByUserId(user)
                        userData.email?.let { receivers.add(it) }
                    }
                    // 获取用户中文名
                    val senderBk = bkUserService.getBkUserByUserId(userId)
                    val chname = senderBk.chname ?: senderBk.bk_username
                    shareMail(chname, fileShareInfo, shareUsers, expireDays.toInt(), mailMode)
                }

            }
        } catch (e: Exception) {
            logger.error(e.message)
        }
        return result
    }

    private fun shareMail(chname: String, file: FileShareInfo, receivers: List<String>, days: Int, mailMode: String) {
        if (mailMode != "local") {
            bkMailMessage(chname, file, receivers, days)
        } else {
            sendMimeMail(chname, file, receivers, days)
        }
    }

    private fun devopsMailMessage(userId: String, file: FileShareInfo, receivers: List<String>, days: Int) {
        logger.info("devops message api, used by $userId")
        val title = CanwayMailTemplate.getShareEmailTitle(userId, file.fileName)
        val devopsMailMessage = DevopsMailMessage(
            receivers = receivers.toSet(),
            body = CanwayMailTemplate.getShareEmailBody(file.projectId, title, userId, days, listOf(file)),
            title = title,
            sender = userId
        )
        val requestUrl = "${devopsHost.removeSuffix("/")}$devopsMailApi"
        CanwayHttpUtils.doPost(requestUrl, devopsMailMessage.toJsonString()).content
    }

    private fun bkMailMessage(chname: String, file: FileShareInfo, receivers: List<String>, days: Int) {
        logger.info("bk message api, used by $chname")
        val title = CanwayMailTemplate.getShareEmailTitle(chname, file.fileName)
        val bkMailMessage = BkMailMessage(
            bkAppCode = appCode,
            bkAppSecret = appSecret,
            bkUsername = chname,
            receiverUsername = StringUtils.join(receivers, ","),
            title = title,
            content = CanwayMailTemplate.getShareEmailBody(file.projectId, title, chname, days, listOf(file))
        )
        val requestUrl = "${bkHost.removeSuffix("/")}$bkMailApi"
        CanwayHttpUtils.doPost(requestUrl, bkMailMessage.toJsonString()).content
    }

    private fun sendMimeMail(chname: String, file: FileShareInfo, receivers: List<String>, days: Int) {
        logger.info("bkrepo mimeMail api, used by $chname")
        val receiverMails = mutableSetOf<String>()
        for (user in receivers) {
            // 查询蓝鲸用户信息
            val userData = userClient.userInfoById(user).data!!
            userData.email?.let { receiverMails.add(it) }
        }
        require(receiverMails.isNotEmpty())
        val mailMessage = mailSender.createMimeMessage()
        val mimeMailMessage = MimeMessageHelper(mailMessage, true)
        mimeMailMessage.setFrom(sender)
        mimeMailMessage.setTo(receiverMails.toTypedArray())
        val title = CanwayMailTemplate.getShareEmailTitle(chname, file.fileName)
        mimeMailMessage.setSubject(title)
        val body = CanwayMailTemplate.getShareEmailBody(file.projectId, title, chname, days, listOf(file))
        mimeMailMessage.setText(body, true)
        mailSender.send(mailMessage)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayShareAspect::class.java)
        const val bkMailApi = "/api/c/compapi/cmsi/send_mail/"
        const val devopsMailApi = "/ms/notify/api/service/notifies/email/"
    }
}
