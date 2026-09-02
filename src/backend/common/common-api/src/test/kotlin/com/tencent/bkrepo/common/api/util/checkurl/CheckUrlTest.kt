package com.tencent.bkrepo.common.api.util.checkurl

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.net.MalformedURLException

class CheckUrlTest {

    @Test
    fun `accepts https url with default subhost rules`() {
        val config = UrlCheckConfig(
            schemes = listOf("https"),
            rules = listOf("example.com"),
            mode = "subhost",
        )
        assertDoesNotThrow { CheckUrl.checkUrl("https://repo.example.com", config) }
    }

    @Test
    fun `rejects invalid scheme`() {
        val config = UrlCheckConfig(
            schemes = listOf("https"),
            rules = listOf(".*"),
            mode = "regex",
        )
        assertThrows(MalformedURLException::class.java) {
            CheckUrl.checkUrl("http://repo.example.com", config)
        }
    }

    @Test
    fun `empty rules accept dotted host without fabricating regex`() {
        assertDoesNotThrow { SecUrlValidator.validate("http://1.2.3.4", UrlCheckProperties()) }
    }

    @Test
    fun `empty rules reject hostname without dot`() {
        assertThrows(MalformedURLException::class.java) {
            SecUrlValidator.validate("http://localhost", UrlCheckProperties())
        }
    }

    @Test
    fun `configured subhost rules still accept matching host`() {
        val props = UrlCheckProperties(rules = listOf("example.com"), mode = "subhost")
        assertDoesNotThrow { SecUrlValidator.validate("https://repo.example.com", props) }
    }

    @Test
    fun `configured subhost rules reject non matching host`() {
        val props = UrlCheckProperties(rules = listOf("example.com"), mode = "subhost")
        assertThrows(MalformedURLException::class.java) {
            SecUrlValidator.validate("https://evil.example.net", props)
        }
    }
}
