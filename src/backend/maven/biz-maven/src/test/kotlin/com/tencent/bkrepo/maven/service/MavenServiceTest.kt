package com.tencent.bkrepo.maven.service

import com.tencent.bkrepo.maven.service.impl.MavenServiceImpl
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ComponentScans
import org.springframework.test.context.TestPropertySource

@DisplayName("maven service test")
@DataMongoTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ComponentScans(
        ComponentScan("com.tencent.bkrepo.maven.service"), ComponentScan("com.tencent.bkrepo.maven.dao")
)
@TestPropertySource(locations = ["classpath:bootstrap-ut.properties"])
class MavenServiceTest(
        private val mavenService: MavenServiceImpl
) {
    @Test
    @DisplayName("test maven web file upload")
    fun `test maven web file upload`() {}
}
