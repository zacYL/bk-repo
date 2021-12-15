package com.tencent.bkrepo.common.cpack.conf

import com.tencent.bkrepo.common.cpack.enums.MailNotifyType
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.context.annotation.Bean
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.stereotype.Component
import java.util.*

@ConfigurationProperties("mail")
@Component
data class CpackMailConf(

    var host: String = "smtp.exmail.qq.com",

    var port: Int = 465,

    var protocol: String = "smtp",

    var mailNotifyType: MailNotifyType? = MailNotifyType.CPACK,

    var username: String = "undefined",

    var password: String = "undefined",

    @NestedConfigurationProperty
    var smtp: Smtp = Smtp()

) {

    @Bean
    fun mailSender(): JavaMailSender {
        val mailSender = JavaMailSenderImpl()
        val prop = Properties().apply {
            setProperty("mail.smtp.auth", smtp.auth)
            setProperty("mail.smtp.starttls.enable", smtp.starttls.enable)
            setProperty("mail.smtp.starttls.required", smtp.starttls.required)
        }
        mailSender.javaMailProperties = prop
        mailSender.host = host
        mailSender.port = port
        mailSender.username = username
        mailSender.password = password
        mailSender.protocol = protocol
        mailSender.defaultEncoding = "UTF-8"
        return mailSender
    }
}

data class Smtp(
    var auth: String = "true",
    @NestedConfigurationProperty
    var starttls: Starttls = Starttls()
)

data class Starttls(
    val enable: String = "true",
    val required: String = "true"
)
