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

    private val nodeLists = listOf<NodeInfo>(

    )

    @Test
    @DisplayName("测试将清理策略打平方法")
    fun `flatten repo cleanStrategy`() {
        val flattenRule = RepoCleanRuleUtils.flattenRule(cleanStrategy)
        println(flattenRule?.size)
        println(flattenRule?.toJsonString())
    }
}
