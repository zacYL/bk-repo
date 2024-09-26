/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2024 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.go.constant

const val REPO_TYPE = "GO"

// Field
const val PACKAGE_KEY = "packageKey"
const val VERSION = "version"
const val EXTENSION = "extension"
const val README_KEY = "readme"
const val GO_MOD_KEY = "goMod"

// Filename Definition
const val GO_MOD = "go.mod"
const val VERSION_LIST = ".versionList"
const val LATEST_VERSION_INFO = ".latest"
const val README_FILE = ".readme"

const val CLIENT_NAME = "bk"
const val CLIENT_ARCHIVE_PATH = "cli/$CLIENT_NAME.zip"

// Header
const val HEADER_OVERWRITE = "X-BKREPO-OVERWRITE"
const val HEADER_PUBLIC = "X-BKREPO-REPO-PUBLIC"
const val HEADER_VERSION = "X-BKREPO-VERSION"
const val HEADER_CLI_LATEST = "X-BKREPO-GO-CLI-LATEST"
const val HEADER_UPLOAD_VALIDATE = "X-BKREPO-GO-UPLOAD-VALIDATE"

// Threshold
const val GO_MOD_SIZE_THRESHOLD = 16 * 1024
const val README_SIZE_THRESHOLD = GO_MOD_SIZE_THRESHOLD

// Regex
const val WINDOWS_RESERVED_FILE_NAME_REGEX = "((?i)CON|PRN|AUX|NUL|COM\\d|LPT\\d)"
const val MODULE_PREFIX_REGEX = "^(?!$WINDOWS_RESERVED_FILE_NAME_REGEX\\.)[a-z0-9]+[a-z0-9-]*\\.[a-z0-9-.]*[a-z0-9-]\$"
const val MODULE_ELEMENT_REGEX =
    "^(?!$WINDOWS_RESERVED_FILE_NAME_REGEX\\.)(?![\\w-.~]*~\\d+\\.)[\\w-~][\\w-.~]*(?<!\\.)\$"
const val GENERIC_MAJOR_VERSION_SUFFIX_REGEX = "^/v(?!1\$)[1-9]\\d*\$"
const val GOPKG_MAJOR_VERSION_SUFFIX_REGEX = "^\\.v(0|[1-9]\\d*)\$"
const val CANONICAL_VERSION_REGEX = "v\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9-.]+)?(\\+incompatible)?"

// other
const val PATH_DELIMITER = "/@v/"
const val GOPKG_IN = "gopkg.in"
const val ARCHIVE = "ARCHIVE"
const val VERSION_PREFIX = "v"
const val LATEST = "latest"
const val LATEST_VERSION_PATH = "/@latest"
const val INCOMPATIBLE_SUFFIX = "+incompatible"
