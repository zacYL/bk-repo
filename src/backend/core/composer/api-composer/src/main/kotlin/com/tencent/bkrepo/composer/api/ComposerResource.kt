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

package com.tencent.bkrepo.composer.api

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.composer.artifact.ComposerArtifactInfo
import com.tencent.bkrepo.composer.artifact.ComposerArtifactInfo.Companion.COMPOSER_DEPLOY
import com.tencent.bkrepo.composer.artifact.ComposerArtifactInfo.Companion.COMPOSER_INSTALL
import com.tencent.bkrepo.composer.artifact.ComposerArtifactInfo.Companion.COMPOSER_JSON
import com.tencent.bkrepo.composer.artifact.ComposerArtifactInfo.Companion.COMPOSER_PACKAGES
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping

@Tag(name = "composer http协议接口")
interface ComposerResource {

    @Operation(summary = "install")
    @GetMapping(COMPOSER_INSTALL)
    fun installRequire(
        @ArtifactPathVariable composerArtifactInfo: ComposerArtifactInfo
    )

    @Operation(summary = "packages.json")
    @GetMapping(COMPOSER_PACKAGES, produces = [MediaType.APPLICATION_JSON_VALUE])
    fun packages(@ArtifactPathVariable composerArtifactInfo: ComposerArtifactInfo)

    @Operation(summary = "%package%.json")
    @GetMapping(COMPOSER_JSON, produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getJson(@ArtifactPathVariable composerArtifactInfo: ComposerArtifactInfo)

    @Operation(summary = "deploy")
    @PutMapping(COMPOSER_DEPLOY, produces = [MediaType.APPLICATION_JSON_VALUE])
    fun deploy(@ArtifactPathVariable composerArtifactInfo: ComposerArtifactInfo, file: ArtifactFile)
}
