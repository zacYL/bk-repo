import { formatDate } from '@repository/utils'
const CTeamTypeMap = {
    DEMAND: {
        content: '需求',
        type: 'WARNING'
    },
    TASK: {
        content: '任务'
    },
    BUG: {
        content: '缺陷',
        type: 'FAILED'
    }
}
export default {
    computed: {
        rootNode () {
            const { groupId, version, lastModifiedDate } = this.detail.basic
            return {
                title: this.packageName,
                metadata: [
                    groupId && `GroupId: ${groupId}`,
                    version && `版本: ${version}`,
                    lastModifiedDate && `更新时间: ${formatDate(lastModifiedDate)}`
                ]
            }
        },
        leftTree () {
            const metadata = this.detail.metadata || {}
            return {
                children: [
                    {
                        title: '工作项',
                        children: Object.keys(metadata).map(key => {
                            const [type, CTeamId] = key.split('-')
                            if (CTeamTypeMap[type]) {
                                return {
                                    title: metadata[key],
                                    leftTag: CTeamTypeMap[type],
                                    metadata: [
                                        `编号: ${CTeamId}`
                                    ]
                                }
                            } else return false
                        }).filter(Boolean)
                    },
                    {
                        title: '代码',
                        children: [
                            {
                                title: '提交信息',
                                metadata: [
                                    `lastCommit: ${metadata.lastCommit || '--'}`,
                                    `newCommit: ${metadata.newCommit || '--'}`
                                ]
                            }
                        ]
                    }
                ]
            }
        },
        rightTree () {
            const metadata = this.detail.metadata || {}
            const ips = [
                ...(metadata['target.ips'] || '')
                    .toString()
                    .matchAll(/([0-9]+\.){3}[0-9]+/g)
            ].map(match => match[0])
            return {
                children: [
                    {
                        title: '构建',
                        children: metadata.pipelineName
                            ? [
                                {
                                    title: metadata.pipelineName,
                                    metadata: [
                                        `流水线编号: ${metadata.buildNo}`,
                                        `流水线用户: ${this.userList[metadata.userId]?.name || metadata.userId}`
                                    ]
                                }
                            ]
                            : []
                    },
                    {
                        title: '测试',
                        children: metadata.reportUrl
                            ? [
                                {
                                    title: '测试报告',
                                    url: metadata.reportUrl
                                }
                            ]
                            : []
                    },
                    {
                        title: '部署',
                        children: metadata['env.info']
                            ? [
                                {
                                    title: metadata['env.info'],
                                    metadata: [
                                        `机器IP: ${ips}`,
                                        `时间: ${formatDate(metadata['update.time'])}`
                                    ]
                                }
                            ]
                            : []
                    }
                ]
            }
        }
    }
}
