import { formatDate } from '@repository/utils'
const CTeamTypeMap = {
    demand: {
        content: '需求',
        type: 'WARNING'
    },
    task: {
        content: '任务'
    },
    bug: {
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
                            const typeLabel = CTeamTypeMap[type.toLowerCase()]
                            if (typeLabel) {
                                return {
                                    title: metadata[key],
                                    leftTag: typeLabel,
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
                                    `lastCommit: ${metadata.lastCommit || metadata.lastcommit || '/'}`,
                                    `newCommit: ${metadata.newCommit || metadata.newcommit || '/'}`
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
            const pipelineName = metadata.pipelineName || metadata.pipelinename
            const buildNo = metadata.buildNo || metadata.buildno
            const userId = metadata.userId || metadata.userid
            const reportUrl = metadata.reportUrl || metadata.reporturl
            return {
                children: [
                    {
                        title: '构建',
                        children: pipelineName
                            ? [
                                {
                                    title: pipelineName,
                                    metadata: [
                                        `流水线编号: ${buildNo}`,
                                        `流水线用户: ${this.userList[userId]?.name || userId}`
                                    ]
                                }
                            ]
                            : []
                    },
                    {
                        title: '测试',
                        children: reportUrl
                            ? [
                                {
                                    title: '测试报告',
                                    url: reportUrl
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
