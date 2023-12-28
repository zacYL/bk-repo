/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.pypi.artifact.repository

import com.tencent.bkrepo.common.api.exception.MethodNotAllowedException
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.virtual.VirtualRepository
import com.tencent.bkrepo.pypi.artifact.xml.Value
import com.tencent.bkrepo.pypi.constants.ELEMENT_SUFFIX
import com.tencent.bkrepo.pypi.constants.FILE_NAME_REGEX
import com.tencent.bkrepo.pypi.constants.PACKAGE_INDEX_TITLE
import com.tencent.bkrepo.pypi.constants.PSEUDO_MATCH_REGEX
import com.tencent.bkrepo.pypi.constants.PypiQueryType
import com.tencent.bkrepo.pypi.constants.QUERY_TYPE
import com.tencent.bkrepo.pypi.constants.SELECTOR_A
import com.tencent.bkrepo.pypi.constants.SIMPLE_PAGE_CONTENT
import com.tencent.bkrepo.pypi.constants.VERSION_INDEX_TITLE
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.springframework.stereotype.Component
import java.util.TreeSet

@Component
class PypiVirtualRepository : VirtualRepository() {

    /**
     * 整合多个仓库的内容。
     */
    @Suppress("UNCHECKED_CAST")
    override fun query(context: ArtifactQueryContext): String? {
        if (context.getAttribute<PypiQueryType>(QUERY_TYPE) == PypiQueryType.VERSION_DETAIL) {
            throw MethodNotAllowedException()
        }
        val artifactName = context.artifactInfo.getArtifactName().removePrefix("/")
        val pseudoSelector = if (artifactName.isBlank()) "" else String.format(PSEUDO_MATCH_REGEX, FILE_NAME_REGEX)
        val elementPages = (super.query(context) as List<String>)
            .map { Jsoup.parse(it).body().select(SELECTOR_A + pseudoSelector) }
            .takeIf { it.isNotEmpty() }
            ?: throw NotFoundException(ArtifactMessageCode.NODE_NOT_FOUND, artifactName)
        val compositePage = if (elementPages.size == 1) elementPages.first() else {
            val anchorSet = TreeSet<Element>(compareBy { it.text() })
            elementPages.forEach { anchorSet.addAll(it) }
            Elements(anchorSet)
        }
        val title =
            if (artifactName.isBlank()) PACKAGE_INDEX_TITLE else String.format(VERSION_INDEX_TITLE, artifactName)
        val content = compositePage.joinToString(ELEMENT_SUFFIX, postfix = "<br />")
        return String.format(SIMPLE_PAGE_CONTENT.trimIndent(), title, content)
    }

    override fun search(context: ArtifactSearchContext): List<Any> {
        val valueList: MutableList<Value> = mutableListOf()
        val virtualConfiguration = context.getVirtualConfiguration()
        val repoList = virtualConfiguration.repositoryList
        val traversedList = getTraversedList(context)
        for (repoIdentify in repoList) {
            if (repoIdentify in traversedList) {
                continue
            }
            traversedList.add(repoIdentify)
            val subRepoInfo = repositoryClient.getRepoDetail(context.projectId, repoIdentify.name).data!!
            val repository = ArtifactContextHolder.getRepository(subRepoInfo.category)
            val subContext = context.copy(subRepoInfo) as ArtifactSearchContext
            val subValueList = repository.search(subContext)
            subValueList.let {
                valueList.addAll(it as List<Value>)
            }
        }
        return valueList
    }
}
