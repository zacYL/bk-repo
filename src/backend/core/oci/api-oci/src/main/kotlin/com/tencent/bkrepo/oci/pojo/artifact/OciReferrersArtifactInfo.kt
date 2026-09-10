package com.tencent.bkrepo.oci.pojo.artifact

class OciReferrersArtifactInfo(
    projectId: String,
    repoName: String,
    packageName: String,
    val digest: String
) : OciArtifactInfo(projectId, repoName, packageName, "")
