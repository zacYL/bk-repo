/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */

package com.tencent.bkrepo.common.storage.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration

@DisplayName("ReceiveProperties 默认值")
class ReceivePropertiesTest {

    @Test
    fun `default blockExpireTime is 12 hours`() {
        val properties = ReceiveProperties()

        assertEquals(Duration.ofHours(12), properties.blockExpireTime)
    }
}
