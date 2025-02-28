package com.tencent.bkrepo.oci.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.nio.file.Path

interface DescriptorPath {

    @get:JsonIgnore
    val path: Path?

}