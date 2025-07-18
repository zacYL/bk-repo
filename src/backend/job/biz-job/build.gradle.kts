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
dependencies {
    implementation(project(":job:api-job"))
    implementation(project(":common:common-service:service-servlet"))
    implementation(project(":common:common-job"))
    implementation(project(":common:common-security"))
    implementation(project(":common:common-storage:storage-service"))
    implementation(project(":common:common-stream"))
    implementation(project(":common:common-redis"))
    implementation(project(":common:common-query:query-mongo"))
    implementation(project(":common:common-mongo-reactive"))
    implementation(project(":common:common-artifact:artifact-service"))
    implementation(project(":common:common-artifact:artifact-cache"))
    implementation(project(":common:common-lock"))
    implementation(project(":repository:api-repository"))
    implementation(project(":core:helm:api-helm"))
    implementation(project(":core:oci:api-oci"))
    implementation(project(":core:maven:api-maven"))
    implementation(project(":replication:api-replication"))
    implementation(project(":archive:api-archive"))
    implementation(project(":common:common-archive:archive-service"))
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("com.tencent.devops:devops-schedule-common")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation("io.mockk:mockk")
}
