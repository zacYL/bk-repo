package com.tencent.bkrepo.generic.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeltaSyncServiceTest {

    @Test
    fun `sqlLiteral should accept ci identifiers`() {
        assertEquals("e-123abc", DeltaSyncService.sqlLiteral("e-123abc"))
        assertEquals("b-456def", DeltaSyncService.sqlLiteral("b-456def"))
        assertEquals("apk", DeltaSyncService.sqlLiteral("apk"))
        assertEquals("tar.gz", DeltaSyncService.sqlLiteral("tar.gz"))
    }

    @Test
    fun `sqlLiteral should reject disallowed characters`() {
        assertNull(DeltaSyncService.sqlLiteral(""))
        assertNull(DeltaSyncService.sqlLiteral("e-1'x"))
        assertNull(DeltaSyncService.sqlLiteral("apk;x"))
        assertNull(DeltaSyncService.sqlLiteral("a b"))
        assertNull(DeltaSyncService.sqlLiteral("x\"y"))
    }

    @Test
    fun `buildHistoryUploadSql should embed only safe literals`() {
        val sql = DeltaSyncService.buildHistoryUploadSql(
            table = "bkbase_table",
            now = 1L,
            maxBandwidth = 50,
            taskId = "e-123",
            fileType = "apk",
            buildId = "b-456",
        )
        assertNotNull(sql)
        assertTrue(sql!!.contains("taskId = 'e-123'"))
        assertTrue(sql.contains("fileType = 'apk'"))
        assertTrue(sql.contains("buildId != 'b-456'"))
    }

    @Test
    fun `buildHistoryUploadSql should skip query on unsafe input`() {
        assertNull(
            DeltaSyncService.buildHistoryUploadSql(
                table = "bkbase_table",
                now = 1L,
                maxBandwidth = 50,
                taskId = "e-123'",
                fileType = "apk",
                buildId = "b-456",
            )
        )
    }
}
