package com.tencent.bkrepo.repository.service.repo

import com.tencent.bkrepo.repository.pojo.storage.DiskCleanInfo

interface StorageCleanService {
    fun computeCleanDisk(): DiskCleanInfo

    fun executeDiskClean()
}
