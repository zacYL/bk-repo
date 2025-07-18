/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.repository.service

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.constant.DownloadInterceptorType
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.pojo.configuration.composite.CompositeConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.composite.ProxyChannelSetting
import com.tencent.bkrepo.common.artifact.pojo.configuration.composite.ProxyConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.local.LocalConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.common.storage.credentials.FileSystemCredentials
import com.tencent.bkrepo.repository.UT_PROJECT_ID
import com.tencent.bkrepo.repository.UT_REGION
import com.tencent.bkrepo.repository.UT_REPO_DISPLAY
import com.tencent.bkrepo.repository.UT_REPO_NAME
import com.tencent.bkrepo.repository.UT_STORAGE_CREDENTIALS_KEY
import com.tencent.bkrepo.repository.UT_USER
import com.tencent.bkrepo.common.metadata.config.RepositoryProperties
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import com.tencent.bkrepo.repository.pojo.credendials.StorageCredentialsCreateRequest
import com.tencent.bkrepo.repository.pojo.project.ProjectCreateRequest
import com.tencent.bkrepo.repository.pojo.project.ProjectUpdateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoUpdateRequest
import com.tencent.bkrepo.common.metadata.service.node.NodeService
import com.tencent.bkrepo.common.metadata.service.project.ProjectService
import com.tencent.bkrepo.common.metadata.service.repo.ProxyChannelService
import com.tencent.bkrepo.common.metadata.service.repo.RepositoryService
import com.tencent.bkrepo.common.metadata.service.repo.StorageCredentialService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.Duration

@DisplayName("仓库服务测试")
@DataMongoTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RepositoryServiceTest @Autowired constructor(
    private val projectService: ProjectService,
    private val repositoryService: RepositoryService,
    private val storageCredentialService: StorageCredentialService,
    private val repositoryProperties: RepositoryProperties,
    private val proxyChannelService: ProxyChannelService
) : ServiceBaseTest() {

    @MockBean
    private lateinit var nodeService: NodeService

    private val storageCredentials = FileSystemCredentials().apply {
        key = UT_STORAGE_CREDENTIALS_KEY
        path = "test"
        cache.enabled = true
        cache.path = "cache-test"
        cache.expireDuration = Duration.ofHours(10)
    }

    @BeforeAll
    fun beforeAll() {
        initMock()
        if (!projectService.checkExist(UT_PROJECT_ID)) {
            val projectCreateRequest = ProjectCreateRequest(
                name = UT_PROJECT_ID,
                displayName = UT_REPO_NAME,
                description = UT_REPO_DISPLAY,
                createPermission = true,
                operator = UT_USER
            )
            projectService.createProject(projectCreateRequest)
        }
        val request = StorageCredentialsCreateRequest(UT_STORAGE_CREDENTIALS_KEY, storageCredentials, UT_REGION)
        storageCredentialService.create(UT_USER, request)
    }

    @BeforeEach
    fun beforeEach() {
        repositoryService.listRepo(UT_PROJECT_ID).forEach {
            repositoryService.deleteRepo(RepoDeleteRequest(UT_PROJECT_ID, it.name, operator = UT_USER))
        }
        repositoryProperties.defaultStorageCredentialsKey = null
    }

    @Test
    @DisplayName("测试列表查询")
    fun `test list query`() {
        assertEquals(0, repositoryService.listRepo(UT_PROJECT_ID).size)
        val size = 20
        repeat(size) { repositoryService.createRepo(createRequest("repo$it")) }
        assertEquals(size, repositoryService.listRepo(UT_PROJECT_ID).size)
    }

    @Test
    @DisplayName("测试分页查询")
    fun `test page query`() {
        assertEquals(0, repositoryService.listRepo(UT_PROJECT_ID).size)
        val size = 51L
        repeat(size.toInt()) { repositoryService.createRepo(createRequest("repo$it")) }
        // 兼容性测试
        var page = repositoryService.listRepoPage(UT_PROJECT_ID, 0, 10)
        assertEquals(10, page.records.size)
        assertEquals(size, page.totalRecords)
        assertEquals(6, page.totalPages)
        assertEquals(10, page.pageSize)
        assertEquals(1, page.pageNumber)

        // 测试第一页
        page = repositoryService.listRepoPage(UT_PROJECT_ID, 1, 10)
        assertEquals(10, page.records.size)
        assertEquals(size, page.totalRecords)
        assertEquals(6, page.totalPages)
        assertEquals(10, page.pageSize)
        assertEquals(1, page.pageNumber)

        page = repositoryService.listRepoPage(UT_PROJECT_ID, 6, 10)
        assertEquals(1, page.records.size)
        assertEquals(size, page.totalRecords)
        assertEquals(6, page.totalPages)
        assertEquals(10, page.pageSize)
        assertEquals(6, page.pageNumber)

        // 测试空页码
        page = repositoryService.listRepoPage(UT_PROJECT_ID, 7, 10)
        assertEquals(0, page.records.size)
        assertEquals(size, page.totalRecords)
        assertEquals(6, page.totalPages)
        assertEquals(10, page.pageSize)
        assertEquals(7, page.pageNumber)
    }

    @Test
    @DisplayName("测试判断仓库是否存在")
    fun `test check exist`() {
        repositoryService.createRepo(createRequest())
        assertTrue(repositoryService.checkExist(UT_PROJECT_ID, UT_REPO_NAME))
        assertTrue(repositoryService.checkExist(UT_PROJECT_ID, UT_REPO_NAME, RepositoryType.GENERIC.name))
        assertFalse(repositoryService.checkExist("", ""))
        assertFalse(repositoryService.checkExist(UT_PROJECT_ID, ""))
        assertFalse(repositoryService.checkExist("", UT_REPO_NAME))

        repositoryService.deleteRepo(RepoDeleteRequest(UT_PROJECT_ID, UT_REPO_NAME, operator = SYSTEM_USER))
        assertFalse(repositoryService.checkExist(UT_PROJECT_ID, UT_REPO_NAME))
    }

    @Test
    @DisplayName("测试仓库名称校验")
    fun `test repo name`() {
        repositoryService.createRepo(createRequest("a..."))
        assertThrows<ErrorCodeException> { repositoryService.createRepo(createRequest("...")) }
        assertThrows<ErrorCodeException> { repositoryService.createRepo(createRequest("...a")) }
    }

    @Test
    @DisplayName("测试创建同名仓库")
    fun `should throw exception when repo name exists`() {
        repositoryService.createRepo(createRequest())
        assertThrows<ErrorCodeException> { repositoryService.createRepo(createRequest()) }
    }

    @Test
    @DisplayName("测试使用指定storage key创建仓库")
    fun `test create with specific storage key`() {
        val request = createRequest("repo-specific-storage-key", UT_STORAGE_CREDENTIALS_KEY)
        repositoryService.createRepo(request)
        val repository =
            repositoryService.getRepoDetail(UT_PROJECT_ID, "repo-specific-storage-key", RepositoryType.GENERIC.name)!!
        assertEquals("repo-specific-storage-key", repository.name)
        assertEquals(RepositoryType.GENERIC, repository.type)
        assertEquals(RepositoryCategory.LOCAL, repository.category)
        assertEquals(true, repository.public)
        assertEquals(UT_PROJECT_ID, repository.projectId)
        assertEquals("simple description", repository.description)
        assertEquals(storageCredentials, repository.storageCredentials)
        assertEquals(UT_STORAGE_CREDENTIALS_KEY, repository.storageCredentials!!.key)

        assertThrows<ErrorCodeException> { repositoryService.createRepo(createRequest("repo-specific-storage-key")) }
    }

    @Test
    @DisplayName("测试使用空storage key创建仓库")
    fun `test create with null storage key`() {
        assertNull(repositoryProperties.defaultStorageCredentialsKey)
        repositoryService.createRepo(createRequest())
        val repository = repositoryService.getRepoDetail(UT_PROJECT_ID, UT_REPO_NAME, RepositoryType.GENERIC.name)!!
        assertNull(repository.storageCredentials)
    }

    @Test
    @DisplayName("测试使用默认storage key创建仓库")
    fun `test create with default storage key`() {
        repositoryProperties.defaultStorageCredentialsKey = UT_STORAGE_CREDENTIALS_KEY
        repositoryService.createRepo(createRequest("repo-default-storage-key"))
        val repository =
            repositoryService.getRepoDetail(UT_PROJECT_ID, "repo-default-storage-key", RepositoryType.GENERIC.name)!!
        val dbCredential = repository.storageCredentials
        assertEquals(storageCredentials, dbCredential)
    }

    @Test
    @DisplayName("测试使用项目指定的storage key创建仓库")
    fun `test create with project storage key`() {
        val repoName = "repo-project-storage-key"
        projectService.updateProject(UT_PROJECT_ID, ProjectUpdateRequest(credentialsKey = UT_STORAGE_CREDENTIALS_KEY))
        repositoryService.createRepo(createRequest(repoName))
        val repository = repositoryService.getRepoDetail(UT_PROJECT_ID, repoName, RepositoryType.GENERIC.name)!!
        assertEquals(UT_STORAGE_CREDENTIALS_KEY, repository.storageCredentials!!.key!!)
        projectService.updateProject(UT_PROJECT_ID, ProjectUpdateRequest(useDefaultCredentialsKey = true))
    }

    @Test
    @DisplayName("测试使用不存在的storage key创建仓库")
    fun `should throw exception when storage key nonexistent`() {
        val request = createRequest("repo-non-exist-credentials-key", "non-exist-credentials-key")
        assertThrows<ErrorCodeException> { repositoryService.createRepo(request) }
    }

    @Test
    @DisplayName("测试更新仓库信息")
    fun `test update repository info`() {
        repositoryService.createRepo(createRequest())
        val updateRequest = RepoUpdateRequest(
            projectId = UT_PROJECT_ID,
            name = UT_REPO_NAME,
            public = false,
            description = "updated description",
            operator = UT_USER
        )
        repositoryService.updateRepo(updateRequest)
        val repository = repositoryService.getRepoDetail(UT_PROJECT_ID, UT_REPO_NAME)!!
        assertEquals(false, repository.public)
        assertEquals("updated description", repository.description)
    }

    @Test
    @DisplayName("测试使用不同类型的仓库更新配置")
    fun `should throw exception when update with different configuration type`() {
        repositoryService.createRepo(createRequest())
        val updateRequest = RepoUpdateRequest(
            projectId = UT_PROJECT_ID,
            name = UT_REPO_NAME,
            public = false,
            description = "updated description",
            configuration = RemoteConfiguration(),
            operator = UT_USER
        )
        assertThrows<ErrorCodeException> { repositoryService.updateRepo(updateRequest) }
    }

    @Test
    @DisplayName("测试更新composite类型仓库配置")
    fun `test update composite repo configuration`() {
        val publicChannel = ProxyChannelSetting(public = true, name = "public1", url = "http://url1")
        val publicChannel2 = ProxyChannelSetting(public = true, name = "public1", url = "http://url2")

        val privateChannel1 = ProxyChannelSetting(public = false, name = "private1", url = "http://url1")
        val privateChannel2 = ProxyChannelSetting(public = false, name = "private2", url = "http://url2")
        val privateChannel3 = ProxyChannelSetting(public = false, name = "private3", url = "http://url3")
        val privateChannel4 = ProxyChannelSetting(public = false, name = "private1", url = "http://url4")

        // 测试代理名字重复，抛出PARAMETER_INVALID
        var proxyConfiguration = ProxyConfiguration(channelList = listOf(publicChannel, publicChannel2))
        var configuration = CompositeConfiguration(proxy = proxyConfiguration)
        var createRequest = RepoCreateRequest(
            projectId = UT_PROJECT_ID,
            name = UT_REPO_NAME,
            type = RepositoryType.GENERIC,
            category = RepositoryCategory.COMPOSITE,
            public = true,
            description = "simple description",
            configuration = configuration,
            operator = UT_USER
        )
        assertThrows<ErrorCodeException> { repositoryService.createRepo(createRequest) }

        // 正常创建 public1 private2
        proxyConfiguration = ProxyConfiguration(channelList = listOf(publicChannel, privateChannel2))
        configuration = CompositeConfiguration(proxy = proxyConfiguration)
        createRequest = RepoCreateRequest(
            projectId = UT_PROJECT_ID,
            name = UT_REPO_NAME,
            type = RepositoryType.GENERIC,
            category = RepositoryCategory.COMPOSITE,
            public = true,
            description = "simple description",
            configuration = configuration,
            operator = UT_USER
        )
        repositoryService.createRepo(createRequest)
        var repoDetail = repositoryService.getRepoDetail(UT_PROJECT_ID, UT_REPO_NAME, "GENERIC")
        var compositeConfiguration = (repoDetail!!.configuration as CompositeConfiguration)
        assertEquals(2, compositeConfiguration.proxy.channelList.size)
        assertEquals("public1", compositeConfiguration.proxy.channelList[0].name)
        assertEquals("http://url1", compositeConfiguration.proxy.channelList[0].url)
        assertEquals(true, compositeConfiguration.proxy.channelList[0].public)
        assertEquals("private2", compositeConfiguration.proxy.channelList[1].name)
        assertEquals("http://url2", compositeConfiguration.proxy.channelList[1].url)
        // 检查私有代理仓库是否创建
        var privateProxyRepo1 = proxyChannelService.queryProxyChannel(
            UT_PROJECT_ID, UT_REPO_NAME, RepositoryType.GENERIC, "public1"
        )
        assertNotNull(privateProxyRepo1)
        var privateProxyRepo2 = proxyChannelService.queryProxyChannel(
            UT_PROJECT_ID, UT_REPO_NAME, RepositoryType.GENERIC, "private2"
        )
        assertNotNull(privateProxyRepo2)

        // 更新 public1 private3
        proxyConfiguration = ProxyConfiguration(channelList = listOf(publicChannel, privateChannel3))
        configuration = CompositeConfiguration(proxy = proxyConfiguration)
        var updateRequest = RepoUpdateRequest(
            projectId = UT_PROJECT_ID,
            name = UT_REPO_NAME,
            public = false,
            configuration = configuration,
            operator = UT_USER
        )

        repositoryService.updateRepo(updateRequest)
        // 检查配置
        repoDetail = repositoryService.getRepoDetail(UT_PROJECT_ID, UT_REPO_NAME, "GENERIC")
        assertNotNull(repoDetail)
        assertEquals(false, repoDetail!!.public)
        assertEquals("simple description", repoDetail.description)
        compositeConfiguration = (repoDetail.configuration as CompositeConfiguration)
        assertEquals(2, compositeConfiguration.proxy.channelList.size)
        assertEquals("public1", compositeConfiguration.proxy.channelList[0].name)
        assertEquals("http://url1", compositeConfiguration.proxy.channelList[0].url)
        assertEquals("private3", compositeConfiguration.proxy.channelList[1].name)
        assertEquals("http://url3", compositeConfiguration.proxy.channelList[1].url)
        // 检查 private2删除，private3创建
        privateProxyRepo1 = proxyChannelService.queryProxyChannel(
            UT_PROJECT_ID, UT_REPO_NAME, RepositoryType.GENERIC, "public1"
        )
        assertNotNull(privateProxyRepo1)
        privateProxyRepo2 = proxyChannelService.queryProxyChannel(
            UT_PROJECT_ID, UT_REPO_NAME, RepositoryType.GENERIC, "private2"
        )
        assertNull(privateProxyRepo2)
        val privateProxyRepo3 = proxyChannelService.queryProxyChannel(
            UT_PROJECT_ID, UT_REPO_NAME, RepositoryType.GENERIC, "private3"
        )
        assertNotNull(privateProxyRepo3)

        // 更新 public1 public2，1 2同名不同url，报错
        proxyConfiguration = ProxyConfiguration(channelList = listOf(publicChannel, publicChannel2))
        configuration = CompositeConfiguration(proxy = proxyConfiguration)
        updateRequest = RepoUpdateRequest(
            projectId = UT_PROJECT_ID,
            name = UT_REPO_NAME,
            public = false,
            configuration = configuration,
            operator = UT_USER
        )
        assertThrows<ErrorCodeException> { repositoryService.updateRepo(updateRequest) }

        // 更新 public1 public1，报错
        proxyConfiguration = ProxyConfiguration(channelList = listOf(publicChannel, publicChannel))
        configuration = CompositeConfiguration(proxy = proxyConfiguration)
        updateRequest = RepoUpdateRequest(
            projectId = UT_PROJECT_ID,
            name = UT_REPO_NAME,
            public = false,
            configuration = configuration,
            operator = UT_USER
        )
        assertThrows<ErrorCodeException> { repositoryService.updateRepo(updateRequest) }
    }

    @Test
    @DisplayName("测试删除仓库")
    fun `test delete repository`() {
        repositoryService.createRepo(createRequest("test1"))
        repositoryService.createRepo(createRequest("test2"))
        repositoryService.deleteRepo(RepoDeleteRequest(UT_PROJECT_ID, "test1", operator = SYSTEM_USER))
        assertNull(repositoryService.getRepoDetail(UT_PROJECT_ID, "test1"))

        assertThrows<ErrorCodeException> {
            repositoryService.deleteRepo(RepoDeleteRequest(UT_PROJECT_ID, "", operator = SYSTEM_USER))
        }
        assertThrows<ErrorCodeException> {
            repositoryService.deleteRepo(RepoDeleteRequest(UT_PROJECT_ID, "test1", operator = SYSTEM_USER))
        }

        assertNotNull(repositoryService.getRepoDetail(UT_PROJECT_ID, "test2"))
    }

    @Test
    @DisplayName("测试更新仓库存储凭据")
    fun `test update repository credentials`() {
        // create storage key
        val newStorageKey = "$UT_STORAGE_CREDENTIALS_KEY-2"
        val request = StorageCredentialsCreateRequest(
            newStorageKey, storageCredentials.copy(key = newStorageKey), UT_REGION
        )
        storageCredentialService.create(UT_USER, request)

        // create repo
        var repo = repositoryService.createRepo(createRequest("test-update", UT_STORAGE_CREDENTIALS_KEY))
        repo = repositoryService.getRepoDetail(repo.projectId, repo.name)!!
        assertEquals(UT_STORAGE_CREDENTIALS_KEY, repo.storageCredentials?.key)

        // update
        repositoryService.updateStorageCredentialsKey(repo.projectId, repo.name, newStorageKey)
        repo = repositoryService.getRepoDetail(repo.projectId, repo.name)!!
        assertEquals(newStorageKey, repo.storageCredentials?.key)
        assertEquals(UT_STORAGE_CREDENTIALS_KEY, repo.oldCredentialsKey)

        // unset old
        repositoryService.unsetOldStorageCredentialsKey(repo.projectId, repo.name)
        repo = repositoryService.getRepoDetail(repo.projectId, repo.name)!!
        assertEquals(null, repo.oldCredentialsKey)

        // clean
        repositoryService.deleteRepo(RepoDeleteRequest(repo.projectId, repo.name, operator = UT_USER))
        storageCredentialService.delete(newStorageKey)
    }

    private fun createRequest(name: String = UT_REPO_NAME, storageCredentialsKey: String? = null): RepoCreateRequest {
        val configuration = LocalConfiguration()
        val type = DownloadInterceptorType.WEB
        val rules = mapOf(
            "filename" to "*.apk",
            "metadata" to "key: value"
        )
        configuration.settings["interceptors"] = listOf(
            mapOf(
                "type" to type,
                "rules" to rules
            )
        )
        return RepoCreateRequest(
            projectId = UT_PROJECT_ID,
            name = name,
            type = RepositoryType.GENERIC,
            category = RepositoryCategory.LOCAL,
            public = true,
            description = "simple description",
            configuration = configuration,
            storageCredentialsKey = storageCredentialsKey,
            operator = UT_USER
        )
    }
}
