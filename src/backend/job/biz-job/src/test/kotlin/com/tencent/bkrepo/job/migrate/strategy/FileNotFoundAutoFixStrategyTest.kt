package com.tencent.bkrepo.job.migrate.strategy

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream
import com.tencent.bkrepo.archive.CompressStatus
import com.tencent.bkrepo.archive.model.TCompressFile
import com.tencent.bkrepo.archive.repository.ArchiveFileDao
import com.tencent.bkrepo.archive.repository.CompressFileDao
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.stream.artifactStream
import com.tencent.bkrepo.common.metadata.dao.node.NodeDao
import com.tencent.bkrepo.common.metadata.service.file.FileReferenceService
import com.tencent.bkrepo.common.metadata.service.repo.RepositoryService
import com.tencent.bkrepo.common.metadata.service.repo.StorageCredentialService
import com.tencent.bkrepo.common.storage.config.StorageProperties
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.common.storage.credentials.FileSystemCredentials
import com.tencent.bkrepo.job.batch.utils.RepositoryCommonUtils
import com.tencent.bkrepo.job.migrate.dao.ArchiveMigrateFailedNodeDao
import com.tencent.bkrepo.job.migrate.dao.MigrateFailedNodeDao
import com.tencent.bkrepo.job.migrate.utils.MigrateTestUtils
import com.tencent.bkrepo.job.migrate.utils.MigrateTestUtils.createNode
import com.tencent.bkrepo.job.migrate.utils.MigrateTestUtils.insertFailedNode
import com.tencent.bkrepo.job.migrate.utils.MigrateTestUtils.removeNodes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.query.Query
import org.springframework.test.context.TestPropertySource
import java.time.LocalDateTime

@DisplayName("文件找不到错误自动修复策略测试")
@DataMongoTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(
    FileNotFoundAutoFixStrategy::class,
    StorageProperties::class,
    RepositoryCommonUtils::class,
    MigrateFailedNodeDao::class,
    ArchiveMigrateFailedNodeDao::class,
    NodeDao::class,
    ArchiveFileDao::class,
    CompressFileDao::class,
)
@ComponentScan(basePackages = ["com.tencent.bkrepo.common.metadata"])
@TestPropertySource(locations = ["classpath:bootstrap-ut.properties"])
class FileNotFoundAutoFixStrategyTest @Autowired constructor(
    private val nodeDao: NodeDao,
    private val compressFileDao: CompressFileDao,
    private val strategy: FileNotFoundAutoFixStrategy,
    private val migrateFailedNodeDao: MigrateFailedNodeDao,
    private val archiveMigrateFailedNodeDao: ArchiveMigrateFailedNodeDao,
    repositoryCommonUtils: RepositoryCommonUtils
) {
    @MockBean
    private lateinit var storageCredentialService: StorageCredentialService

    @MockBean
    private lateinit var fileReferenceService: FileReferenceService

    @MockBean
    private lateinit var repositoryService: RepositoryService

    @MockBean
    private lateinit var storageService: StorageService

    @BeforeEach
    fun beforeEach() {
        whenever(storageCredentialService.list(anyOrNull()))
            .thenReturn(listOf(FileSystemCredentials()))
        whenever(storageCredentialService.findByKey(anyString()))
            .thenReturn(FileSystemCredentials())
        whenever(fileReferenceService.increment(anyString(), anyOrNull(), any()))
            .thenReturn(true)
        whenever(repositoryService.getRepoDetail(anyString(), anyString(), anyOrNull()))
            .thenReturn(MigrateTestUtils.buildRepo())
        whenever(storageService.exist(anyString(), anyOrNull())).thenReturn(false)
        migrateFailedNodeDao.remove(Query())
        archiveMigrateFailedNodeDao.remove(Query())
        nodeDao.removeNodes()
    }

    @Test
    fun testNodeExists() {
        whenever(storageService.exist(anyString(), anyOrNull())).thenReturn(true)
        val node = migrateFailedNodeDao.insertFailedNode()
        assertTrue(strategy.fix(node))
    }

    @Test
    fun testNodeCompressedOrArchived() {
        // node compressed or archived
        var node = nodeDao.createNode(archived = true, compressed = true)
        val failedNode = migrateFailedNodeDao.insertFailedNode(node.fullPath)
        assertFalse(strategy.fix(failedNode))

        // exists compressed or archived record
        nodeDao.removeNodes()
        node = nodeDao.createNode()
        compressFileDao.insert(
            TCompressFile(
                id = null,
                lastModifiedBy = "",
                lastModifiedDate = LocalDateTime.now(),
                createdBy = "",
                createdDate = LocalDateTime.now(),
                sha256 = node.sha256!!,
                baseSha256 = "",
                status = CompressStatus.COMPLETED,
                storageCredentialsKey = null,
                uncompressedSize = node.size
            )
        )
        assertFalse(strategy.fix(failedNode))
        compressFileDao.remove(Query())
    }

    @Test
    fun testCopyFromOtherStorage() {
        whenever(storageService.load(anyString(), any(), anyOrNull()))
            .thenReturn(ByteInputStream(ByteArray(1), 1).artifactStream(Range.full(1)))
        doNothing().whenever(storageService).copy(anyString(), anyOrNull(), anyOrNull())
        val node = nodeDao.createNode()
        val failedNode = migrateFailedNodeDao.insertFailedNode(node.fullPath, nodeId = node.id!!)
        assertTrue(strategy.fix(failedNode))
        verify(fileReferenceService, times(1)).increment(any(), anyOrNull(), any())
    }

    @Test
    fun testArchiveMigrateFailedNode() {
        whenever(storageService.load(anyString(), any(), anyOrNull())).thenReturn(null)
        val node = nodeDao.createNode()
        val failedNode = migrateFailedNodeDao.insertFailedNode(node.fullPath, nodeId = node.id!!)
        assertTrue(strategy.fix(failedNode))
        assertEquals(0, migrateFailedNodeDao.count(Query()))
        assertEquals(1, archiveMigrateFailedNodeDao.count(Query()))
    }
}
