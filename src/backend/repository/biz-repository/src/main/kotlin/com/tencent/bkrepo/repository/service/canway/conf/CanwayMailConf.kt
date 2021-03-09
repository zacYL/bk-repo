package com.tencent.bkrepo.repository.service.canway.conf

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.stereotype.Component
import java.util.Properties

@ConfigurationProperties("mail")
@Component
data class CanwayMailConf(

    var host: String = "smtp.exmail.qq.com",

    var port: Int = 465,

    var protocol: String = "smtps",

    var username: String = "undefined",

    var password: String = "undefined",

    var smtpAuth: String = "true",

    var smtpTlsEnable: String = "true",

    var smtpTlsRequired: String = "true",

    var bkrepo: String = "undefined"
) {

    @Bean
    fun mailSender(): JavaMailSender {
        var mailSender = JavaMailSenderImpl()
        var prop = Properties().apply {
            setProperty("mail.smtp.auth", smtpAuth)
            setProperty("mail.smtp.starttls.enable", smtpTlsEnable)
            setProperty("mail.smtp.starttls.required", smtpTlsRequired)
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
