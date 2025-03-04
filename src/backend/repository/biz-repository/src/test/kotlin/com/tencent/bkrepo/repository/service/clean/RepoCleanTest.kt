package com.tencent.bkrepo.repository.service.clean

import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.pojo.configuration.clean.CleanStatus
import com.tencent.bkrepo.common.artifact.pojo.configuration.clean.RepositoryCleanStrategy
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.util.RepoCleanRuleUtils
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@DisplayName("仓库清理测试")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RepoCleanTest {

    private val cleanStrategy = RepositoryCleanStrategy(
        status = CleanStatus.WAITING,
        autoClean = false,
        reserveDays = 30,
        rule = Rule.NestedRule(
            rules = mutableListOf(
                Rule.QueryRule(
                    field = "projectId",
                    value = "test",
                    operation = OperationType.EQ
                ),
                Rule.QueryRule(
                    field = "repoName",
                    value = "t333",
                    operation = OperationType.EQ
                ),
                Rule.NestedRule(
                    rules = mutableListOf(
                        Rule.NestedRule(
                            rules = mutableListOf(
                                Rule.QueryRule(
                                    field = "path",
                                    value = "/",
                                    operation = OperationType.EQ
                                ),
                                Rule.QueryRule(
                                    field = "reserveDays",
                                    value = 1024L,
                                    operation = OperationType.LTE
                                ),
                                Rule.NestedRule(
                                    rules = mutableListOf(
                                        Rule.QueryRule(
                                            field = "name",
                                            value = "test",
                                            operation = OperationType.EQ
                                        ),
                                        Rule.QueryRule(
                                            field = "metadata.pipelineId",
                                            value = "dddd",
                                            operation = OperationType.MATCH
                                        ),
                                    ),
                                    relation = Rule.NestedRule.RelationType.OR
                                )
                            ),
                            relation = Rule.NestedRule.RelationType.AND
                        ),
                        Rule.NestedRule(
                            rules = mutableListOf(
                                Rule.QueryRule(
                                    field = "path",
                                    value = "/a/b/c",
                                    operation = OperationType.EQ
                                ),
                                Rule.QueryRule(
                                    field = "reserveDays",
                                    value = 30L,
                                    operation = OperationType.LTE
                                ),
                                Rule.NestedRule(
                                    rules = mutableListOf(
                                        Rule.QueryRule(
                                            field = "name",
                                            value = "xxxx",
                                            operation = OperationType.EQ
                                        ),
                                        Rule.QueryRule(
                                            field = "metadata.pipelineId",
                                            value = "dddd",
                                            operation = OperationType.MATCH
                                        ),
                                    ),
                                    relation = Rule.NestedRule.RelationType.OR
                                )
                            ),
                            relation = Rule.NestedRule.RelationType.AND
                        ),
                    ),
                    relation = Rule.NestedRule.RelationType.OR
                )
            ),
            relation = Rule.NestedRule.RelationType.AND
        )
    )

    private val cleanStrategy01 = RepositoryCleanStrategy(
        status = CleanStatus.WAITING,
        autoClean = true,
        rule = Rule.NestedRule(
            rules = mutableListOf(
                Rule.QueryRule(
                    field = "projectId",
                    value = "s93fc1",
                    operation = OperationType.EQ
                ),
                Rule.QueryRule(
                    field = "repoName",
                    value = "jc_migrate_test6",
                    operation = OperationType.EQ
                ),
                Rule.NestedRule(
                    rules = mutableListOf(
                        Rule.NestedRule(
                            rules = mutableListOf(
                                Rule.QueryRule(
                                    field = "path",
                                    value = "/",
                                    operation = OperationType.REGEX
                                ),
                                Rule.QueryRule(
                                    field = "reserveDays",
                                    value = 2L,
                                    operation = OperationType.LTE
                                ),
                                Rule.NestedRule(
                                    rules = mutableListOf(),
                                    relation = Rule.NestedRule.RelationType.OR
                                )
                            ),
                            relation = Rule.NestedRule.RelationType.AND
                        ),
                        Rule.NestedRule(
                            rules = mutableListOf(
                                Rule.QueryRule(
                                    field = "path",
                                    value = "/CTest/aa",
                                    operation = OperationType.REGEX
                                ),
                                Rule.QueryRule(
                                    field = "reserveDays",
                                    value = 2L,
                                    operation = OperationType.LTE
                                ),
                                Rule.NestedRule(
                                    rules = mutableListOf(),
                                    relation = Rule.NestedRule.RelationType.OR
                                )
                            ),
                            relation = Rule.NestedRule.RelationType.AND
                        ),
                        Rule.NestedRule(
                            rules = mutableListOf(
                                Rule.QueryRule(
                                    field = "path",
                                    value = "/CTest",
                                    operation = OperationType.REGEX
                                ),
                                Rule.QueryRule(
                                    field = "reserveDays",
                                    value = 1L,
                                    operation = OperationType.LTE
                                ),
                                Rule.NestedRule(
                                    rules = mutableListOf(
                                        Rule.QueryRule(
                                            field = "metadata.name",
                                            value = "jackson-datatype-jsr310-2.13.0.jar",
                                            operation = OperationType.EQ
                                        ),
                                        Rule.QueryRule(
                                            field = "metadata.name",
                                            value = "*connector*",
                                            operation = OperationType.MATCH
                                        ),
                                        Rule.QueryRule(
                                            field = "metadata.version",
                                            value = ".*.([4-9]|[1-9][0-9]).*",
                                            operation = OperationType.REGEX
                                        ),
                                    ),
                                    relation = Rule.NestedRule.RelationType.OR
                                )
                            ),
                            relation = Rule.NestedRule.RelationType.AND
                        ),
                        Rule.NestedRule(
                            rules = mutableListOf(
                                Rule.QueryRule(
                                    field = "path",
                                    value = "/Jolie",
                                    operation = OperationType.REGEX
                                ),
                                Rule.QueryRule(
                                    field = "reserveDays",
                                    value = 1L,
                                    operation = OperationType.LTE
                                ),
                                Rule.NestedRule(
                                    rules = mutableListOf(
                                        Rule.QueryRule(
                                            field = "id",
                                            value = "null",
                                            operation = OperationType.NE
                                        )
                                    ),
                                    relation = Rule.NestedRule.RelationType.OR
                                )
                            ),
                            relation = Rule.NestedRule.RelationType.AND
                        ),
                        Rule.NestedRule(
                            rules = mutableListOf(
                                Rule.QueryRule(
                                    field = "path",
                                    value = "/CPack",
                                    operation = OperationType.REGEX
                                ),
                                Rule.QueryRule(
                                    field = "reserveDays",
                                    value = 1L,
                                    operation = OperationType.LTE
                                ),
                                Rule.NestedRule(
                                    rules = mutableListOf(
                                        Rule.QueryRule(
                                            field = "name",
                                            value = "admin-server-0.0.1-SNAPSHOT.jar",
                                            operation = OperationType.EQ
                                        ),
                                        Rule.QueryRule(
                                            field = "name",
                                            value = "*bksdk*",
                                            operation = OperationType.MATCH
                                        ),
                                        Rule.QueryRule(
                                            field = "name",
                                            value = ".*.war",
                                            operation = OperationType.REGEX
                                        ),
                                    ),
                                    relation = Rule.NestedRule.RelationType.OR
                                )
                            ),
                            relation = Rule.NestedRule.RelationType.AND
                        ),
                    ),
                    relation = Rule.NestedRule.RelationType.OR
                )
            ),
            relation = Rule.NestedRule.RelationType.AND
        )
    )

    private val nodeInfo = NodeInfo(
        createdBy = "Jolie",
        createdDate = "2022-08-18T11:45:22.706",
        lastModifiedBy = "ericwu",
        lastModifiedDate = "2022-08-19T17:32:10.803",
        recentlyUseDate = "2022-08-19T14:13:34.741",
        lastAccessDate = "2022-08-18T11:45:22.706",
        folder = false,
        path = "/Jolie/文件扫描格式测试/",
        name = "1.tar",
        fullPath = "/Jolie/文件扫描格式测试/1.tar",
        size = 618,
        sha256 = "a9399e2a7608e2f8c78e57d7a5d80030607b316051bc7fa2a1b141d1437c1020",
        md5 = "dab0bb5bbf5e4396cc02b57b11b78931",
        metadata = emptyMap<String, Any>(),
        nodeMetadata = listOf(),
        projectId = "w6675d",
        repoName = "jc_generic_test"
    )

    private val nodeLists = listOf<NodeInfo>()

    @Test
    @DisplayName("测试将清理策略打平方法")
    fun `flatten repo cleanStrategy`() {
        val flattenRule = RepoCleanRuleUtils.flattenRule(cleanStrategy01)
        println(flattenRule?.size)
        println(flattenRule?.toJsonString())
    }
}
