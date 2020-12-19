package com.tencent.bkrepo.repository.service.canway.mail

import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMailMessage
import org.springframework.mail.javamail.MimeMessageHelper

class CanwayMailUtils(
        val mailSender: JavaMailSender
) {

    private val mailMessage = mailSender.createMimeMessage()

    private fun mimeMailMessage() {
        val mimeMailMessage = MimeMessageHelper(mailMessage, true)


    }
}