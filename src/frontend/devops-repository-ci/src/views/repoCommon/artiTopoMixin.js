import { formatDate } from '@repository/utils'
export default {
    computed: {
        CTeamTypeMap () {
            return {
                demand: {
                    content: this.$t('demand'),
                    type: 'WARNING'
                },
                task: {
                    content: this.$t('task')
                },
                bug: {
                    content: this.$t('bug'),
                    type: 'FAILED'
                }
            }
        },
        rootNode () {
            const { groupId, version, lastModifiedDate } = this.detail.basic
            return {
                title: this.packageName,
                metadata: [
                    groupId && `GroupId: ${groupId}`,
                    version && (this.$t('version') + `: ${version}`),
                    lastModifiedDate && (this.$t('updateTime') + `: ${formatDate(lastModifiedDate)}`)
                ]
            }
        },
        leftTree () {
            const metadata = this.metadataMap
            return {
                children: [
                    {
                        title: this.$t('workItem'),
                        children: Object.keys(metadata)
                            .map(key => {
                                const [type, CTeamId] = key.split('-')
                                const typeLabel = this.CTeamTypeMap[type.toLowerCase()]
                                if (typeLabel) {
                                    return {
                                        title: metadata[key],
                                        leftTag: typeLabel,
                                        metadata: [
                                            this.$t('serialNumber') + `: ${CTeamId}`
                                        ]
                                    }
                                } else return false
                            })
                            .filter(Boolean)
                            .sort((a, b) => b.leftTag.content.charCodeAt() - a.leftTag.content.charCodeAt())
                    },
                    {
                        title: this.$t('code'),
                        children: (metadata.lastCommit || metadata.lastcommit || metadata.newCommit || metadata.newcommit)
                            ? [
                                {
                                    title: this.$t('commitMessage'),
                                    metadata: [
                                        `lastCommit: ${metadata.lastCommit || metadata.lastcommit || '/'}`,
                                        `newCommit: ${metadata.newCommit || metadata.newcommit || '/'}`
                                    ]
                                }
                            ]
                            : []
                    }
                ]
            }
        },
        rightTree () {
            const metadata = this.metadataMap
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
                        title: this.$t('build'),
                        children: pipelineName
                            ? [
                                {
                                    title: pipelineName,
                                    metadata: [
                                        this.$t('pipelineNumber') + `: ${buildNo}`,
                                        this.$t('pipelineUsers') + `: ${this.userList[userId]?.name || userId}`
                                    ]
                                }
                            ]
                            : []
                    },
                    {
                        title: this.$t('test'),
                        children: reportUrl
                            ? [
                                {
                                    title: this.$t('testReport'),
                                    url: reportUrl
                                }
                            ]
                            : []
                    },
                    {
                        title: this.$t('deploy'),
                        children: metadata['env.info']
                            ? [
                                {
                                    title: metadata['env.info'],
                                    metadata: [
                                        this.$t('serverIP') + `: ${ips}`,
                                        this.$t('time') + `: ${formatDate(metadata['update.time'])}`
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
