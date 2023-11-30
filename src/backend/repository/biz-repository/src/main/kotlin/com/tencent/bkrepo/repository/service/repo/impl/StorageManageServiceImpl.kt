package com.tencent.bkrepo.repository.service.repo.impl

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.credentials.StorageType
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.dao.RepositoryDao
import com.tencent.bkrepo.repository.model.TRepository
import com.tencent.bkrepo.repository.pojo.storage.DirectionType
import com.tencent.bkrepo.repository.pojo.storage.RepoLogicStoragePojo
import com.tencent.bkrepo.repository.pojo.storage.RepoStorageInfoParam
import com.tencent.bkrepo.repository.pojo.storage.StoragePojo
import com.tencent.bkrepo.repository.service.repo.ProjectService
import com.tencent.bkrepo.repository.service.repo.StorageManageService
import com.tencent.bkrepo.repository.util.FileSizeUtils
import org.springframework.stereotype.Service
import java.io.File

/**
 * 存储管理实现类
 */
@Service
class StorageManageServiceImpl(
    private val storageProperties: StorageProperties,
    private val projectService: ProjectService,
    private val nodeDao: NodeDao,
    private val repositoryDao: RepositoryDao
) : StorageManageService {
    override fun info(): StoragePojo {
        require(storageProperties.type == StorageType.FILESYSTEM) {
            "Only support filesystem storage"
        }
        val path = storageProperties.filesystem.path
        val file = File(path)
        val total = file.totalSpace
        val available = file.freeSpace
        val used = total - available
        return StoragePojo(
            path = path,
            total = FileSizeUtils.formatFileSize(total),
            used = FileSizeUtils.formatFileSize(used),
            available = FileSizeUtils.formatFileSize(available),
            message = StorageType.FILESYSTEM.name
        )
    }

    override fun infoRepos(
        repoStorageInfoParam: RepoStorageInfoParam
    ): Page<RepoLogicStoragePojo> {
        with(repoStorageInfoParam) {
            val repoList = repositoryDao.findAll()
            val repoLogicStorageList = mutableListOf<RepoLogicStoragePojo>()
            if (projectId.isNullOrEmpty()) {
                val projectList = projectService.listProject()
                projectList.forEach { project ->
                    repoLogicStorageList.addAll(
                        repoStatistics(
                            projectId = project.name,
                            repoList = repoList
                        )
                    )
                }
            } else {
                repoLogicStorageList.addAll(
                    repoStatistics(
                        projectId = projectId!!,
                        repoList = repoList
                    )
                )
            }
            val records = when (direction) {
                DirectionType.REPO_SIZE_DESC -> repoLogicStorageList.sortedByDescending { it.size.toLong() }
                DirectionType.REPO_SIZE_ASC -> repoLogicStorageList.sortedBy { it.size.toLong() }
                DirectionType.FILE_NUMBER_DESC -> repoLogicStorageList.sortedByDescending { it.count }
                DirectionType.FILE_NUMBER_ASC -> repoLogicStorageList.sortedBy { it.count }
                else -> repoLogicStorageList.sortedByDescending { it.size.toLong() }
            }

            val pageRequest = Pages.ofRequest(pageNumber, pageSize)
            val start = (pageNumber - 1) * pageSize
            val end = if ((start + pageSize) > records.size) records.size else start + pageSize
            val resultRecords = records.subList(start, end).onEach {
                it.size = FileSizeUtils.formatFileSize(it.size.toLong())
            }
            return Pages.ofResponse(pageRequest, records.size.toLong(), resultRecords)
        }
    }

    private fun repoStatistics(projectId: String, repoList: List<TRepository>): List<RepoLogicStoragePojo> {
        val nodeList = nodeDao.findFileNode(projectId)
        val projectRepoList = repoList.filter {
            it.projectId == projectId && it.deleted == null
        }
        return projectRepoList.map { repo ->
            with(repo) {
                val repoNodeList = nodeList.filter {
                    it.projectId == projectId && it.repoName == name
                }
                RepoLogicStoragePojo(
                    projectId = repo.projectId,
                    repoName = name,
                    type = type,
                    size = (repoNodeList.sumOf { it.size }).toString(),
                    count = repoNodeList.count().toLong()
                )
            }
        }
    }
}
