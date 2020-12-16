package com.tencent.bkrepo.repository.service.canway.conf

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CanwayAuthConf {
    @Value("\${bk.appCode:#{null}}")
    val appCode: String? = null
    @Value("\${bk.appSecret:#{null}}")
    val appSecret: String? = null
    @Value("\${devops.host:#{null}}")
    val devopsHost: String? = null
    @Value("\${bk.host:#{null}}")
    val bkHost: String? = null
}
