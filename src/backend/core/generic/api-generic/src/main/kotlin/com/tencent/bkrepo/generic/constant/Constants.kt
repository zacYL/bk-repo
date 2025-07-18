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

package com.tencent.bkrepo.generic.constant

const val BKREPO_PREFIX = "X-BKREPO-"

const val HEADER_OVERWRITE = BKREPO_PREFIX + "OVERWRITE"
const val HEADER_SHA256 = BKREPO_PREFIX + "SHA256"
const val HEADER_MD5 = BKREPO_PREFIX + "MD5"
const val HEADER_EXPIRES = BKREPO_PREFIX + "EXPIRES"
const val HEADER_SIZE = BKREPO_PREFIX + "SIZE"
const val HEADER_UPLOAD_ID = BKREPO_PREFIX + "UPLOAD-ID"
const val HEADER_SEQUENCE = BKREPO_PREFIX + "SEQUENCE"
const val HEADER_OFFSET = BKREPO_PREFIX + "OFFSET"
const val HEADER_FILE_SIZE = BKREPO_PREFIX + "SIZE"
const val HEADER_OLD_FILE_PATH = BKREPO_PREFIX + "OLD-FILE-PATH"

const val BKREPO_META_PREFIX = "X-BKREPO-META-"
const val BKREPO_META = "X-BKREPO-META"

const val HEADER_UPLOAD_TYPE = "UPLOAD-TYPE"

/**
 * 分块上传标识
 */
const val CHUNKED_UPLOAD = "CHUNKED-UPLOAD"

/**
 * 分块上传uuid
 */
const val CHUNKED_UPLOAD_UUID = "CHUNKED-UPLOAD-UUID"
const val CHUNKED_UPLOAD_CLIENT = "CHUNKED-UPLOAD-CLIENT"

// block上传时直接写入文件指定位置
const val HEADER_BLOCK_APPEND = BKREPO_PREFIX + "BLOCK-APPEND"

/**
 * 分块上传版本后缀
 */
const val SEPARATE_UPLOAD = "SEPARATE-UPLOAD"

/**
 * 审计中心action定义
 */
const val USER_SHARE_CREATE_ACTION = "user_share_create"
const val USER_SHARE_DOWNLOAD_URL_CREATE_ACTION = "user_share_download_url_create"
