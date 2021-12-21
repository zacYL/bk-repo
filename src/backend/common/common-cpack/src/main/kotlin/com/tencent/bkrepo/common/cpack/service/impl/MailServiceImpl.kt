package com.tencent.bkrepo.common.cpack.service.impl

import com.tencent.bkrepo.auth.api.ServiceUserResource
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.cpack.conf.CpackMailConf
import com.tencent.bkrepo.common.cpack.enums.MailNotifyType
import com.tencent.bkrepo.common.cpack.mail.FileShareMailTemplate
import com.tencent.bkrepo.common.cpack.pojo.FileShareInfo
import com.tencent.bkrepo.common.cpack.service.NotifyService
import com.tencent.bkrepo.common.devops.api.conf.DevopsConf
import com.tencent.bkrepo.common.devops.api.pojo.BkMailMessage
import com.tencent.bkrepo.common.devops.api.pojo.DevopsMailMessage
import com.tencent.bkrepo.common.devops.api.util.http.CanwayHttpUtils
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.TemporaryTokenClient
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class MailServiceImpl(
    private val mailSender: JavaMailSender,
    private val nodeClient: NodeClient,
    private val temporaryTokenClient: TemporaryTokenClient,
    private val userService: ServiceUserResource,
    private val mailConf: CpackMailConf
) : NotifyService {

    @Autowired(required = false)
    lateinit var devopsConf: DevopsConf

    override fun fileShare(userId: String, content: String) {
        val artifactInfo = resolveArtifactInfo(content)
        val nodeDetail =
            nodeClient.getNodeDetail(
                artifactInfo.projectId,
                artifactInfo.repoName,
                artifactInfo.getArtifactFullPath()
            ).data ?: throw RuntimeException("$artifactInfo is delete")
        val fileShareInfo = FileShareInfo(
            fileName = nodeDetail.name,
            md5 = nodeDetail.md5 ?: "Can not found md5 value",
            projectId = artifactInfo.projectId,
            repoName = artifactInfo.repoName,
            downloadUrl = content,
            qrCodeBase64 = if (nodeDetail.name.endsWith("apk") || nodeDetail.name.endsWith("ipa")) {
                FileShareMailTemplate.getQRCodeBase64(content)
            } else "null"
        )

        val token = content.substringAfterLast("?token=").removeSuffix("&download=true")
        if (token.isBlank()) {
            throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_INVALID)
        }
        val temporaryToken = temporaryTokenClient.getTokenInfo(token).data
            ?: throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_INVALID)
        val shareUsers = temporaryToken.authorizedUserList
        val receivers = mutableSetOf<String>()
        for (shareUserId in shareUsers) {
            val user = userService.userInfoById(shareUserId).data!!
            user.email?.let { receivers.add(it) }
        }
        val expireDays = temporaryToken.expireDate
        when (mailConf.mailNotifyType) {
            MailNotifyType.CPACK -> sendMimeMail(userId, fileShareInfo, receivers.toList(), expireDays)
            MailNotifyType.BK -> bkMailMessage(userId, fileShareInfo, receivers.toList(), expireDays)
            MailNotifyType.DEVOPS -> devopsMailMessage(userId, fileShareInfo, receivers.toList(), expireDays)
        }
    }

    override fun fileShare(userId: String, content: String, users: List<String>) {
        val artifactInfo = resolveArtifactInfo(content)
        val nodeDetail =
            nodeClient.getNodeDetail(
                artifactInfo.projectId,
                artifactInfo.repoName,
                artifactInfo.getArtifactFullPath()
            ).data ?: throw RuntimeException("$artifactInfo is delete")
        val fileShareInfo = FileShareInfo(
            fileName = nodeDetail.name,
            md5 = nodeDetail.md5 ?: "Can not found md5 value",
            projectId = artifactInfo.projectId,
            repoName = artifactInfo.repoName,
            downloadUrl = content,
            qrCodeBase64 = if (nodeDetail.name.endsWith("apk") || nodeDetail.name.endsWith("ipa")) {
                FileShareMailTemplate.getQRCodeBase64(content)
            } else "null"
        )

        val token = content.substringAfterLast("?token=").removeSuffix("&download=true")
        if (token.isBlank()) {
            throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_INVALID)
        }
        val temporaryToken = temporaryTokenClient.getTokenInfo(token).data
            ?: throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_INVALID)
        val shareUsers = temporaryToken.authorizedUserList
        val receivers = mutableSetOf<String>()
        //链接中分享人为空时，取传递过来的参数
        if (shareUsers.isEmpty()) {
            for (shareUserId in users) {
                val user = userService.userInfoById(shareUserId).data!!
                user.email?.let { receivers.add(it) }
            }
        }
        val expireDays = temporaryToken.expireDate
        when (mailConf.mailNotifyType) {
            MailNotifyType.CPACK -> sendMimeMail(userId, fileShareInfo, receivers.toList(), expireDays)
            MailNotifyType.BK -> bkMailMessage(userId, fileShareInfo, receivers.toList(), expireDays)
            MailNotifyType.DEVOPS -> devopsMailMessage(userId, fileShareInfo, receivers.toList(), expireDays)
        }
    }

    private fun sendMimeMail(sender: String, file: FileShareInfo, receivers: List<String>, expireDays: String?) {
        val user = userService.detail(sender).data!!
        require(receivers.isNotEmpty())
        val mailMessage = mailSender.createMimeMessage()
        val mimeMailMessage = MimeMessageHelper(mailMessage, true)
        mimeMailMessage.setFrom(mailConf.username)
        mimeMailMessage.setTo(receivers.toTypedArray())
        val title = FileShareMailTemplate.getShareEmailTitle(user.name, file.fileName)
        mimeMailMessage.setSubject(title)
        val body = FileShareMailTemplate.getShareEmailBody(file.projectId, title, sender, expireDays, listOf(file))
        mimeMailMessage.setText(body, true)
        mailSender.send(mailMessage)
    }

    private fun bkMailMessage(chname: String, file: FileShareInfo, receivers: List<String>, expireDays: String?) {
        val title = FileShareMailTemplate.getShareEmailTitle(chname, file.fileName)
        val bkMailMessage = BkMailMessage(
            bkAppCode = devopsConf.appCode,
            bkAppSecret = devopsConf.appSecret,
            bkUsername = chname,
            receiverUsername = StringUtils.join(receivers, ","),
            title = title,
            content = FileShareMailTemplate.getShareEmailBody(file.projectId, title, chname, expireDays, listOf(file))
        )
        val requestUrl = "${devopsConf.bkHost.removeSuffix("/")}$bkMailApi"
        CanwayHttpUtils.doPost(requestUrl, bkMailMessage.toJsonString()).content
    }

    private fun devopsMailMessage(userId: String, file: FileShareInfo, receivers: List<String>, expireDays: String?) {
        val title = FileShareMailTemplate.getShareEmailTitle(userId, file.fileName)
        val devopsMailMessage = DevopsMailMessage(
            receivers = receivers.toSet(),
            body = FileShareMailTemplate.getShareEmailBody(file.projectId, title, userId, expireDays, listOf(file)),
            title = title,
            sender = userId
        )
        val requestUrl = "${devopsConf.devopsHost.removeSuffix("/")}$devopsMailApi"
        CanwayHttpUtils.doPost(requestUrl, devopsMailMessage.toJsonString()).content
    }

    private fun resolveArtifactInfo(content: String): ArtifactInfo {
        val pathList = content.substringAfterLast("?redirect=")
            .substringBeforeLast("?token=").trim('/').split('/')
        val projectId = pathList[4]
        val repoName = pathList[5]
        val artifactUri = StringUtils.join(pathList.subList(6, pathList.size), '.')
        return ArtifactInfo(projectId, repoName, artifactUri)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MailServiceImpl::class.java)
        const val devopsMailApi = "/ms/notify/api/service/notifies/email/"
        const val bkMailApi = "/api/c/compapi/cmsi/send_mail/"
    }
}
