package com.tencent.bkrepo.common.cpack

import com.tencent.bkrepo.common.cpack.conf.CpackConf
import com.tencent.bkrepo.common.cpack.conf.CpackMailConf
import com.tencent.bkrepo.common.cpack.controller.NotifyController
import com.tencent.bkrepo.common.cpack.service.impl.MailServiceImpl
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    CpackMailConf::class,
    NotifyController::class,
    MailServiceImpl::class,
    CpackConf::class
)
class CpackAutoConf
