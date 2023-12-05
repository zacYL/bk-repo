package com.tencent.bkrepo.repository.job

import com.tencent.bkrepo.repository.job.base.CenterNodeJob
import com.tencent.bkrepo.repository.service.repo.StorageManageService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 仓库存储大小计算
 */
@Component
class RepoStorageComputationJob(
    private val storageManageService: StorageManageService
) : CenterNodeJob() {
    @Scheduled(cron = "0 0 1/2 * * ?") // 1点开始，2小时执行一次
    override fun start() {
        super.start()
    }

    override fun run() {
        storageManageService.updateRepoStorageCache()
    }

    override fun getLockAtMostFor(): Duration = Duration.ofDays(1)
}
