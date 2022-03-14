package com.tencent.bkrepo.scanner.config.container

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ContainerTaskConfig {

    /**
     * docker host
     */
    @Value("\${container.api.host:}")
    var dockerHost: String = "unix:///var/run/docker.sock"

    /**
     * docker api version
     */
    @Value("\${container.api.version:}")
    var apiVerion: String = "1.23"

    /**
     * run docker entrypoint args
     */
    @Value("\${container.run.args:}")
    var args: String = ""

    /**
     * task image name
     */
    @Value("\${container.run.imageName:}")
    var imageName: String = ""

    /**
     * container work dir
     */
    @Value("\${container.run.dir:}")
    var containerDir: String = ""
}
