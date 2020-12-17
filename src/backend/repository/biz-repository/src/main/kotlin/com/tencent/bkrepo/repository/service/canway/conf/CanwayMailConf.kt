package com.tencent.bkrepo.repository.service.canway.conf

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.util.Properties

@Configuration
class CanwayMailConf(
    @Value("\${mail.host:smtp.exmail.qq.com}")
    val mailHost: String,

    @Value("\${mail.port:465}")
    val mailPort: Int,

    @Value("\${mail.protocol:smtps}")
    val mailProtocol: String,

    @Value("\${mail.username:undefined}")
    val mailUsername: String,

    @Value("\${mail.password:undefined}")
    val mailPassword: String,

    @Value("\${mail.smtp.auth:true}")
    val mailSmtpAuth: String,

    @Value("\${mail.smtp.starttls.enable:true}")
    val mailSmtpTlsEnable: String,

    @Value("\${mail.smtp.starttls.required:true}")
    val mailSmtpTlsRequired: String,

    @Value("\${bkrepo.host:undefined}")
    val bkrepoHost: String
) {

    @Bean
    fun mailSender(): JavaMailSender {
        val mailSender = JavaMailSenderImpl()
        val prop = Properties().apply {
            setProperty("mail.smtp.auth", mailSmtpAuth)
            setProperty("mail.smtp.starttls.enable", mailSmtpTlsEnable)
            setProperty("mail.smtp.starttls.required", mailSmtpTlsRequired)
        }
        mailSender.javaMailProperties = prop
        mailSender.host = mailHost
        mailSender.port = mailPort
        mailSender.username = mailUsername
        mailSender.password = mailPassword
        mailSender.protocol = mailProtocol
        mailSender.defaultEncoding = "UTF-8"
        return mailSender
    }
}
