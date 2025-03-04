package com.tencent.bkrepo.repository.service

import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.metadata.service.whitelist.RemotePackageWhitelistService
import com.tencent.bkrepo.repository.pojo.whitelist.CreateRemotePackageWhitelistRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ComponentScans
import org.springframework.test.context.TestPropertySource

@DisplayName("远程代理制品白名单测试")
@DataMongoTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ComponentScans(
    *[
        ComponentScan("com.tencent.bkrepo.repository.service", "com.tencent.bkrepo.common.metadata"),
        ComponentScan("com.tencent.bkrepo.repository.dao")
    ]
)
@TestPropertySource(locations = ["classpath:bootstrap-ut.properties"])
class RemotePackageWhitelistServiceTest @Autowired constructor(
    private val remotePackageWhitelistService: RemotePackageWhitelistService
) {

    @Test
    @DisplayName("测试批量创建远程代理制品白名单")
    fun `batch create remote package whitelist`() {
        val request = listOf(
            CreateRemotePackageWhitelistRequest(
                type = RepositoryType.DOCKER,
                packageKey = "com.tencent.bkrepo:bkrepo",
                versions = null
            ),
            CreateRemotePackageWhitelistRequest(
                type = RepositoryType.MAVEN,
                packageKey = "com.tencent.bkrepo:bkrepo",
                versions = null
            ),
            CreateRemotePackageWhitelistRequest(
                type = RepositoryType.NPM,
                packageKey = "com.tencent.bkrepo:bkrepo",
                versions = null
            )
        )
        val result = remotePackageWhitelistService.batchWhitelist(request)
        Assertions.assertEquals(2, result)
    }

    @Test
    @DisplayName("测试新建远程代理制品白名单")
    fun `create remote package whitelist`() {
        val request = CreateRemotePackageWhitelistRequest(
            packageKey = "com.alibaba:fastjson",
            versions = listOf("1.2.3", "1.2.4"),
            type = RepositoryType.MAVEN
        )
        remotePackageWhitelistService.createWhitelist(request)
        val whitelists = remotePackageWhitelistService.page(
            type = RepositoryType.MAVEN,
            packageKey = "com.alibaba:fastjson",
            regex = false,
            version = null,
            pageNumber = null,
            pageSize = null
        )
        Assertions.assertEquals(whitelists.records.size, 1)
        println(whitelists.records[0].toJsonString())
    }

    @Test
    @DisplayName("测试搜索远程代理制品白名单")
    fun `page remote package whitelist`() {
        val whitelists = remotePackageWhitelistService.page(
            type = RepositoryType.MAVEN,
            packageKey = "com.alibaba:fastjson",
            regex = false,
            version = null,
            pageNumber = null,
            pageSize = null
        )
        Assertions.assertEquals(whitelists.records.size, 0)
    }

}
