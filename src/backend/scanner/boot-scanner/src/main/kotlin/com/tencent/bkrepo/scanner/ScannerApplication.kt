package com.tencent.bkrepo.scanner

import com.tencent.bkrepo.common.service.condition.MicroService
import org.springframework.boot.runApplication

@MicroService
class ScannerApplication

fun main(args: Array<String>) {
    runApplication<ScannerApplication>(*args)
}
