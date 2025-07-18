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

package com.tencent.bkrepo.auth.controller.user

import com.tencent.bkrepo.auth.constant.AUTH_API_ACCOUNT_PREFIX
import com.tencent.bkrepo.auth.controller.OpenResource
import com.tencent.bkrepo.auth.pojo.account.Account
import com.tencent.bkrepo.auth.pojo.account.CreateAccountRequest
import com.tencent.bkrepo.auth.pojo.account.UpdateAccountRequest
import com.tencent.bkrepo.auth.pojo.enums.CredentialStatus
import com.tencent.bkrepo.auth.pojo.oauth.AuthorizationGrantType
import com.tencent.bkrepo.auth.pojo.token.CredentialSet
import com.tencent.bkrepo.auth.service.AccountService
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.metadata.annotation.LogOperate
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import io.swagger.v3.oas.annotations.Operation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(AUTH_API_ACCOUNT_PREFIX)
class AccountController @Autowired constructor(
    private val accountService: AccountService,
    permissionService: PermissionService
) : OpenResource(permissionService) {

    @Operation(summary = "查询所有账号")
    @GetMapping("/list")
    @LogOperate(type = "ACCOUNT_LIST")
    fun listAccount(): Response<List<Account>> {
        preCheckPlatformPermission()
        preCheckUserAdmin()
        val accountList = accountService.listAccount()
        return ResponseBuilder.success(accountList)
    }

    @Operation(summary = "查询拥有的账号")
    @GetMapping("/own/list")
    fun listOwnAccount(): Response<List<Account>> {
        preCheckPlatformPermission()
        val userId = SecurityUtils.getUserId()
        return ResponseBuilder.success(accountService.listOwnAccount(userId))
    }

    @Operation(summary = "查询已授权账号")
    @GetMapping("/authorized/list")
    @PutMapping("/update")
    fun listAuthorizedAccount(): Response<List<Account>> {
        preCheckPlatformPermission()
        val userId = SecurityUtils.getUserId()
        return ResponseBuilder.success(accountService.listAuthorizedAccount(userId))
    }

    @Operation(summary = "根据appId查询账号")
    @GetMapping("/detail/{appId}")
    fun getAccountDetail(@PathVariable appId: String): Response<Account> {
        preCheckPlatformPermission()
        val userId = SecurityUtils.getUserId()
        return ResponseBuilder.success(accountService.findAccountByAppId(appId, userId))
    }

    @Operation(summary = "创建账号")
    @PostMapping("/create")
    @LogOperate(type = "ACCOUNT_CREATE")
    fun createAccount(@RequestBody request: CreateAccountRequest): Response<Account> {
        preCheckPlatformPermission()
        preCheckGrantTypes(request.authorizationGrantTypes)
        val owner = SecurityUtils.getUserId()
        return ResponseBuilder.success(accountService.createAccount(request, owner))
    }

    @Operation(summary = "更新账号")
    @PutMapping("/update")
    @LogOperate(type = "ACCOUNT_UPDATE")
    fun updateAccount(@RequestBody request: UpdateAccountRequest): Response<Boolean> {
        preCheckPlatformPermission()
        preCheckGrantTypes(request.authorizationGrantTypes)
        val owner = SecurityUtils.getUserId()
        accountService.updateAccount(request, owner)
        return ResponseBuilder.success(true)
    }

    @Operation(summary = "删除账号")
    @DeleteMapping("/delete/{appId}")
    @LogOperate(type = "ACCOUNT_DELETE")
    fun deleteAccount(@PathVariable appId: String): Response<Boolean> {
        preCheckPlatformPermission()
        val userId = SecurityUtils.getUserId()
        accountService.deleteAccount(appId, userId)
        return ResponseBuilder.success(true)
    }

    @Operation(summary = "卸载账号")
    @DeleteMapping("/uninstall/{appId}")
    fun uninstallAccount(@PathVariable appId: String): Response<Boolean> {
        preCheckPlatformPermission()
        val userId = SecurityUtils.getUserId()
        accountService.uninstallAccount(appId, userId)
        return ResponseBuilder.success(true)
    }

    @Operation(summary = "获取账户下的ak/sk对")
    @GetMapping("/credential/list/{appId}")
    fun getCredential(@PathVariable appId: String): Response<List<CredentialSet>> {
        preCheckPlatformPermission()
        val userId = SecurityUtils.getUserId()
        val credential = accountService.listCredentials(appId, userId)
        return ResponseBuilder.success(credential)
    }

    @Operation(summary = "创建ak/sk对")
    @PostMapping("/credential/{appId}")
    @LogOperate(type = "KEYS_CREATE")
    fun createCredential(
        @PathVariable appId: String,
        @RequestParam type: AuthorizationGrantType?
    ): Response<CredentialSet> {
        preCheckPlatformPermission()
        val userId = SecurityUtils.getUserId()
        val result = accountService.createCredential(appId, type ?: AuthorizationGrantType.PLATFORM, userId)
        return ResponseBuilder.success(result)
    }

    @Operation(summary = "删除ak/sk对")
    @DeleteMapping("/credential/{appId}/{accesskey}")
    @LogOperate(type = "KEYS_DELETE")
    fun deleteCredential(@PathVariable appId: String, @PathVariable accesskey: String): Response<Boolean> {
        preCheckPlatformPermission()
        val userId = SecurityUtils.getUserId()
        val result = accountService.deleteCredential(appId, accesskey, userId)
        return ResponseBuilder.success(result)
    }

    @Operation(summary = "更新ak/sk对状态")
    @PutMapping("/credential/{appId}/{accesskey}/{status}")
    @LogOperate(type = "KEYS_UPDATE")
    fun updateCredential(
        @PathVariable appId: String,
        @PathVariable accesskey: String,
        @PathVariable status: CredentialStatus
    ): Response<Boolean> {
        preCheckPlatformPermission()
        preCheckUserAdmin()
        accountService.updateCredentialStatus(appId, accesskey, status)
        return ResponseBuilder.success(true)
    }

    @Operation(summary = "校验ak/sk")
    @GetMapping("/credential/{accesskey}/{secretkey}")
    fun checkCredential(@PathVariable accesskey: String, @PathVariable secretkey: String): Response<String?> {
        preCheckPlatformPermission()
        preCheckUserAdmin()
        val result = accountService.checkCredential(accesskey, secretkey)
        return ResponseBuilder.success(result)
    }
}
