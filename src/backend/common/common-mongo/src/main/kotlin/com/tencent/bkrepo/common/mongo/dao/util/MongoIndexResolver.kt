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

package com.tencent.bkrepo.common.mongo.dao.util

import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.IndexDefinition

/**
 * mongo db 索引解析
 */
object MongoIndexResolver {

    fun resolveIndexFor(clazz: Class<*>): List<IndexDefinition> {
        val indexDefinitions = mutableListOf<IndexDefinition>()

        clazz.getAnnotation(CompoundIndexes::class.java)?.run {
            this.value.forEach { indexDefinitions.add(resolveCompoundIndexDefinition(it)) }
        }

        clazz.getAnnotation(CompoundIndex::class.java)?.run {
            indexDefinitions.add(resolveCompoundIndexDefinition(this))
        }

        return indexDefinitions
    }

    private fun resolveCompoundIndexDefinition(index: CompoundIndex): IndexDefinition {
        val indexDefinition = CompoundIndexDefinition(resolveCompoundIndexKeyFromStringDefinition(index.def))

        if (!index.useGeneratedName) {
            indexDefinition.named(index.name)
        }

        if (index.unique) {
            indexDefinition.unique()
        }

        if (index.sparse) {
            indexDefinition.sparse()
        }

        if (index.background) {
            indexDefinition.background()
        }

        return indexDefinition
    }

    private fun resolveCompoundIndexKeyFromStringDefinition(keyDefinitionString: String): org.bson.Document {
        if (keyDefinitionString.isBlank()) {
            throw InvalidDataAccessApiUsageException("Cannot create index on root level for empty keys.")
        }
        return org.bson.Document.parse(keyDefinitionString)
    }
}
