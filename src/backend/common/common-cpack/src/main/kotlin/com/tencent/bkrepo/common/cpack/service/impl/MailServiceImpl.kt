package com.tencent.bkrepo.common.cpack.service.impl

import com.tencent.bkrepo.auth.api.ServiceUserResource
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.exception.ArtifactNotFoundException
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.cpack.conf.CpackConf
import com.tencent.bkrepo.common.cpack.conf.CpackMailConf
import com.tencent.bkrepo.common.cpack.enums.MailNotifyType
import com.tencent.bkrepo.common.cpack.mail.MailTemplate
import com.tencent.bkrepo.common.cpack.mail.QRUtils
import com.tencent.bkrepo.common.cpack.pojo.FileShareInfo
import com.tencent.bkrepo.common.cpack.service.NotifyService
import com.tencent.bkrepo.common.devops.conf.DevopsConf
import com.tencent.bkrepo.common.devops.pojo.BkMailMessage
import com.tencent.bkrepo.common.devops.pojo.DevopsMailMessage
import com.tencent.bkrepo.common.devops.util.http.SimpleHttpUtils
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.api.TemporaryTokenClient
import org.apache.commons.lang3.StringUtils
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
    private val mailConf: CpackMailConf,
    private val projectClient: ProjectClient,
    private val cpackConf: CpackConf
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
            ).data ?: throw ArtifactNotFoundException(artifactInfo.getArtifactFullPath())
        val projectDisplayName = projectClient.getProjectInfo(artifactInfo.projectId).data!!.displayName
        val fileShareInfo = FileShareInfo(
            fileName = nodeDetail.name,
            md5 = nodeDetail.md5 ?: "Can not found md5 value",
            projectId = projectDisplayName,
            repoName = artifactInfo.repoName,
            downloadUrl = content,
            qrCodeBase64 = if (nodeDetail.name.endsWith("apk") || nodeDetail.name.endsWith("ipa")) {
                QRUtils.getQRCodeBase64(content)
            } else null
        )

        val token = content.substringAfterLast("&token=").removeSuffix("&download=true")
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
        val sender = userService.detail(userId).data!!.name
        when (mailConf.mailNotifyType) {
            MailNotifyType.CPACK -> sendMimeMail(sender, fileShareInfo, receivers.toList(), expireDays)
            MailNotifyType.BK -> bkMailMessage(sender, fileShareInfo, receivers.toList(), expireDays)
            MailNotifyType.DEVOPS -> devopsMailMessage(sender, fileShareInfo, receivers.toList(), expireDays)
        }
    }

    override fun fileShare(userId: String, content: String, users: List<String>) {
        val artifactInfo = resolveArtifactInfo(content)
        val nodeDetail =
            nodeClient.getNodeDetail(
                artifactInfo.projectId,
                artifactInfo.repoName,
                artifactInfo.getArtifactFullPath()
            ).data ?: throw throw ArtifactNotFoundException(artifactInfo.getArtifactFullPath())
        val projectDisplayName = projectClient.getProjectInfo(artifactInfo.projectId).data!!.displayName
        val fileShareInfo = FileShareInfo(
            fileName = nodeDetail.name,
            md5 = nodeDetail.md5 ?: "Can not found md5 value",
            projectId = projectDisplayName,
            repoName = artifactInfo.repoName,
            downloadUrl = content,
            qrCodeBase64 = if (nodeDetail.name.endsWith("apk") || nodeDetail.name.endsWith("ipa")) {
                QRUtils.getQRCodeBase64(content)
            } else null
        )

        val token = content.substringAfterLast("&token=").removeSuffix("&download=true")
        if (token.isBlank()) {
            throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_INVALID)
        }
        val temporaryToken = temporaryTokenClient.getTokenInfo(token).data
            ?: throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_INVALID)
        val shareUsers = temporaryToken.authorizedUserList
        val receivers = mutableSetOf<String>()
        // 链接中分享人为空时，取传递过来的参数
        if (shareUsers.isEmpty()) {
            for (shareUserId in users) {
                val user = userService.userInfoById(shareUserId).data!!
                user.email?.let { receivers.add(it) }
            }
        }
        val expireDays = temporaryToken.expireDate
        val sender = userService.detail(userId).data!!.name
        when (mailConf.mailNotifyType) {
            MailNotifyType.CPACK -> sendMimeMail(sender, fileShareInfo, receivers.toList(), expireDays)
            MailNotifyType.BK -> bkMailMessage(sender, fileShareInfo, users, expireDays)
            MailNotifyType.DEVOPS -> devopsMailMessage(sender, fileShareInfo, users, expireDays)
        }
    }

    override fun newAccountMessage(userId: String, cnName: String, receivers: List<String>) {
        val mainBody = MailTemplate.newAccountHtml(
            cnName = cnName,
            userId = userId,
            url = cpackConf.host
        )
        textMimeMail(receivers, "【CPACK】账号创建通知", mainBody)
    }

    override fun resetPwdMessage(userId: String, cnName: String, receivers: List<String>) {
        val mainBody = MailTemplate.resetPwdHtml(
            cnName = cnName,
            userId = userId,
            url = cpackConf.host
        )
        textMimeMail(receivers, "【CPACK】账号密码重置通知", mainBody)
    }

    private fun textMimeMail(receivers: List<String>, title: String, mainBody: String) {
        require(receivers.isNotEmpty())
        val mailMessage = mailSender.createMimeMessage()
        val mimeMailMessage = MimeMessageHelper(mailMessage, true)
        mimeMailMessage.setFrom(mailConf.username)
        mimeMailMessage.setTo(receivers.toTypedArray())
        mimeMailMessage.setSubject(title)
        val body = MailTemplate.mailCommonHtml2(
            nest = MailTemplate.mainBodyHtml(mainBody)
        )
        mimeMailMessage.setText(body, true)
        mailSender.send(mailMessage)
    }

    private fun sendMimeMail(sender: String, file: FileShareInfo, receivers: List<String>, expireDays: String?) {
        require(receivers.isNotEmpty())
        val mailMessage = mailSender.createMimeMessage()
        val mimeMailMessage = MimeMessageHelper(mailMessage, true)
        mimeMailMessage.setFrom(mailConf.username)
        mimeMailMessage.setTo(receivers.toTypedArray())
        val title = "【CPACK】${sender}与你共享${file.fileName}文件"
        mimeMailMessage.setSubject(title)
        val body = MailTemplate.mailCommonHtml(
            cnName = sender,
            projectId = file.projectId,
            nest = MailTemplate.mainBodyHtml(
                MailTemplate.fileTableHtml(
                    cnName = sender,
                    projectId = file.projectId,
                    expireDays = expireDays,
                    shareFileList = listOf(file)
                )
            )
        )
        mimeMailMessage.setText(body, true)
        mailSender.send(mailMessage)
    }

    private fun bkMailMessage(sender: String, file: FileShareInfo, receivers: List<String>, expireDays: String?) {
        val title = "【CPACK】${sender}与你共享${file.fileName}文件"
        val bkMailMessage = BkMailMessage(
            bkAppCode = devopsConf.appCode,
            bkAppSecret = devopsConf.appSecret,
            bkUsername = sender,
            receiverUsername = StringUtils.join(receivers, ","),
            title = title,
            content = MailTemplate.mailCommonHtml(
                cnName = sender,
                projectId = file.projectId,
                nest = MailTemplate.mainBodyHtml(
                    MailTemplate.fileTableHtml(
                        cnName = sender,
                        projectId = file.projectId,
                        expireDays = expireDays,
                        shareFileList = listOf(file)
                    )
                )
            )
        )
        val requestUrl = "${devopsConf.bkHost.removeSuffix("/")}$bkMailApi"
        SimpleHttpUtils.doPost(requestUrl, bkMailMessage.toJsonString()).content
    }

    private fun devopsMailMessage(sender: String, file: FileShareInfo, receivers: List<String>, expireDays: String?) {
        val title = "【CPACK】${sender}与你共享${file.fileName}文件"
        val devopsMailMessage = DevopsMailMessage(
            receivers = receivers.toSet(),
            title = title,
            sender = sender,
            body = MailTemplate.mailCommonHtml(
                cnName = sender,
                projectId = file.projectId,
                nest = MailTemplate.mainBodyHtml(
                    MailTemplate.fileTableHtml(
                        cnName = sender,
                        projectId = file.projectId,
                        expireDays = expireDays,
                        shareFileList = listOf(file)
                    )
                )
            )
        )
        val requestUrl = "${devopsConf.devopsHost.removeSuffix("/")}$devopsMailApi"
        SimpleHttpUtils.doPost(requestUrl, devopsMailMessage.toJsonString()).content
    }

    private fun resolveArtifactInfo(content: String): ArtifactInfo {
        val pathList = content.substringAfterLast("?redirect=")
            .substringBeforeLast("&token=").trim('/').split('/')
        val projectId = pathList[4]
        val repoName = pathList[5]
        val artifactUri = StringUtils.join(pathList.subList(6, pathList.size), '/')
        return ArtifactInfo(projectId, repoName, artifactUri)
    }

    companion object {
        const val devopsMailApi = "/ms/platform/api/service/notifies/email/"
        const val bkMailApi = "/api/c/compapi/cmsi/send_mail/"
    }
}
