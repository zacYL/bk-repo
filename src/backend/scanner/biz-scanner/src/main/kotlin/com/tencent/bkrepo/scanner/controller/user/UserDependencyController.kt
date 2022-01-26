package com.tencent.bkrepo.scanner.controller.user

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.scanner.model.TDependency
import com.tencent.bkrepo.scanner.service.DependencyService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dependency")
class UserDependencyController(
    private val dependencyService: DependencyService
) {

    @GetMapping("/{projectId}")
    fun info(
        @PathVariable projectId: String,
        @RequestParam repoName: String,
        @RequestParam packageKey: String,
        @RequestParam version: String
    ): Response<TDependency?> {
        val t = dependencyService.find(projectId, repoName, packageKey, version)
        return ResponseBuilder.success(t)
    }
}
