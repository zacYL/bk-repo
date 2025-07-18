/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.common.mongo.reactive.dao

import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.MongoCollectionUtils
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import reactor.core.publisher.Flux
import java.lang.reflect.ParameterizedType

abstract class AbstractMongoReactiveDao<E> : MongoReactiveDao<E> {

    @Suppress("UNCHECKED_CAST")
    protected open val classType = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<E>

    /**
     * 集合名称
     */
    protected open val collectionName: String by lazy { determineCollectionName() }

    suspend fun findOne(query: Query): E? {
        return findOne(query, classType)
    }

    suspend fun find(query: Query): List<E> {
        return find(query, classType)
    }

    suspend fun findAll(): List<E> {
        return findAll(classType)
    }

    suspend fun stream(query: Query): Flux<E> {
        return stream(query, classType)
    }

    override suspend fun <T> stream(query: Query, clazz: Class<T>): Flux<T> {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao stream: [$query] [$clazz]")
        }
        return determineReactiveMongoOperations()
            .find(query, clazz, determineCollectionName(query))
    }

    override suspend fun <T> find(query: Query, clazz: Class<T>): List<T> {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao find: [$query] [$clazz]")
        }
        return determineReactiveMongoOperations()
            .find(query, clazz, determineCollectionName(query))
            .collectList().awaitSingle()
    }

    override suspend fun <T> findAll(clazz: Class<T>): List<T> {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao findAll: [$clazz]")
        }
        return determineReactiveMongoOperations()
            .findAll(clazz, determineCollectionName())
            .collectList().awaitSingle()
    }

    override suspend fun updateFirst(query: Query, update: Update): UpdateResult {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao updateFirst: [$query], [$update]")
        }
        return determineReactiveMongoOperations()
            .updateFirst(query, update, determineCollectionName(query))
            .awaitSingle()
    }

    override suspend fun updateMulti(query: Query, update: Update): UpdateResult {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao updateMulti: [$query], [$update]")
        }
        return determineReactiveMongoOperations()
            .updateMulti(query, update, determineCollectionName(query))
            .awaitSingle()
    }

    override suspend fun <T> findOne(query: Query, clazz: Class<T>): T? {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao findOne: [$query] [$clazz]")
        }
        return determineReactiveMongoOperations()
            .findOne(query, clazz, determineCollectionName(query))
            .awaitSingleOrNull()
    }

    override suspend fun save(entity: E): E {
        return determineReactiveMongoOperations()
            .save(entity, determineCollectionName(entity))
            .awaitSingle()
    }

    override suspend fun insert(entity: E): E {
        return determineReactiveMongoOperations()
            .insert(entity, determineCollectionName(entity))
            .awaitSingle()
    }

    override suspend fun insert(entityCollection: Collection<E>): Collection<E> {
        return determineReactiveMongoOperations()
            .insert(entityCollection, determineCollectionName(entityCollection.first()))
            .collectList().awaitSingle()
    }

    override suspend fun remove(query: Query): DeleteResult {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao remove: [$query]")
        }
        return determineReactiveMongoOperations()
            .remove(query, classType, determineCollectionName(query))
            .awaitSingle()
    }

    override suspend fun upsert(query: Query, update: Update): UpdateResult {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao upsert: [$query], [$update]")
        }
        val mongoOperations = determineReactiveMongoOperations()
        val collectionName = determineCollectionName(query)
        return try {
            mongoOperations.upsert(query, update, collectionName).awaitSingle()
        } catch (exception: DuplicateKeyException) {
            // retry because upsert operation is not atomic
            logger.warn("Upsert error[DuplicateKeyException]: " + exception.message.orEmpty())
            determineReactiveMongoOperations().upsert(query, update, collectionName).awaitSingle()
        }
    }

    override suspend fun count(query: Query): Long {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao count: [$query]")
        }
        val mongoOperations = determineReactiveMongoOperations()
        val collectName = determineCollectionName(query)
        return mongoOperations.count(query, collectName).awaitSingle()
    }

    override suspend fun exists(query: Query): Boolean {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao exists: [$query]")
        }
        return determineReactiveMongoOperations().exists(query, determineCollectionName(query)).awaitSingle()
    }

    override suspend fun <T> findAndModify(
        query: Query,
        update: Update,
        options: FindAndModifyOptions,
        clazz: Class<T>
    ): T? {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao findAndModify: [$query], [$update]")
        }
        return determineReactiveMongoOperations()
            .findAndModify(query, update, options, clazz, determineCollectionName(query)).awaitSingle()
    }

    override suspend fun <O> aggregate(aggregation: Aggregation, outputType: Class<O>): MutableList<O> {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo aggregate: [$aggregation]")
        }
        return determineReactiveMongoOperations().aggregate(
            aggregation,
            determineCollectionName(aggregation),
            outputType
        ).collectList().awaitSingle()
    }

    protected open fun determineCollectionName(): String {
        var collectionName: String? = null
        if (classType.isAnnotationPresent(Document::class.java)) {
            val document = classType.getAnnotation(Document::class.java)
            collectionName = if (document.collection.isNotBlank()) document.collection else document.value
        }

        return if (collectionName.isNullOrEmpty()) {
            MongoCollectionUtils.getPreferredCollectionName(classType)
        } else collectionName
    }

    abstract fun determineReactiveMongoOperations(): ReactiveMongoTemplate

    abstract fun determineCollectionName(query: Query): String

    abstract fun determineCollectionName(entity: E): String

    abstract fun determineCollectionName(aggregation: Aggregation): String

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractMongoReactiveDao::class.java)

        /**
         * mongodb 默认id字段
         */
        const val ID = "_id"
    }
}
