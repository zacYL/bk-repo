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

dependencies {
    api(project(":git:api-git"))
    api(project(":nuget:api-nuget"))
    api(project(":rpm:api-rpm"))
    api(project(":composer:api-composer"))
    api(project(":helm:api-helm"))
    api(project(":npm:api-npm"))
    api(project(":pypi:api-pypi"))
    api(project(":docker:api-docker"))
    api(project(":maven:api-maven"))
    api(project(":repository:api-repository"))
    api(project(":scanner:api-scanner"))
    api(project(":common:common-job"))
    api(project(":common:common-mongo"))
    api(project(":common:common-stream"))
    api(project(":common:common-query:query-mongo"))
    api(project(":common:common-artifact:artifact-service"))
    testImplementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo")
    api(project(":common:common-devops:repository"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    implementation("org.quartz-scheduler:quartz")
}
