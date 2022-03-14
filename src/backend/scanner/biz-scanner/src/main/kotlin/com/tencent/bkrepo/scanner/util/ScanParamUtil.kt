package com.tencent.bkrepo.scanner.util

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType

/**
 * 参数检查
 */
object ScanParamUtil {

    fun checkParam(
        repoType: RepositoryType,
        artifactName: String,
        packageKey: String?,
        version: String?,
        fullPath: String?
    ) {
        when (repoType) {
            RepositoryType.MAVEN -> {
                if (packageKey.isNullOrEmpty() || version.isNullOrEmpty()) {
                    throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "packageKey or version")
                }
            }

            RepositoryType.GENERIC -> {
                if (fullPath.isNullOrEmpty()) {
                    throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "fullPath[$fullPath]")
                }

                //只支持ipa/apk类型包
                if (!artifactName.endsWith(".apk") && !artifactName.endsWith(".ipa")) {
                    throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "name[$artifactName]")
                }
            }
            else -> throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "repoType[${repoType.name}]")
        }
    }
}
