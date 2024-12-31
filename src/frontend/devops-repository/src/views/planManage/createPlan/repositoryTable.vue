<template>
    <div class="repository-table-container">
        <bk-button v-show="!disabled" icon="plus" @click="showAddDialog = true">{{$t('addRepo')}}</bk-button>
        <div v-show="replicaTaskObjects.length" class="mt10">
            <bk-table
                class="mt10 scan-table"
                height="300px"
                :style="{
                    width: (currentLanguage === 'zh-cn' ? 520 : 540) + 'px'
                }"
                :data="replicaTaskObjects"
                :row-border="false"
                row-key="fid"
                size="small">
                <!-- 制品包名称 -->
                <bk-table-column :label="$t('originRepo')" show-overflow-tooltip width="130">
                    <template #default="{ row }">
                        <div class="flex-align-center">
                            <Icon size="16" :name="row.type.toLowerCase()" class="mr5" style="flex-shrink: 0;" />
                            <span class="repo-name text-overflow" :title="row.name">{{ row.name }}</span>
                        </div>
                    </template>
                </bk-table-column>
                <bk-table-column :render-header="stateRender" min-width="100">
                    <template #default="{ row }">
                        <bk-form
                            :ref="row.fid"
                            form-type="inline"
                            :rules="getRules(row.type.toLowerCase())"
                            :model="row">
                            <bk-form-item property="targetProject">
                                <bk-input style="width: 130px;" :placeholder="row.projectId" v-model.trim="row.targetProject" maxlength="6" :disabled="disabled"></bk-input>
                            </bk-form-item>
                            <bk-form-item property="targetStore">
                                <bk-input style="width: 130px;"
                                    v-bk-tooltips="{
                                        content: row.targetStore,
                                        placement: 'top',
                                        disabled: !disabled
                                    }"
                                    :placeholder="row.name" v-model.trim="row.targetStore" maxlength="32" :disabled="disabled"></bk-input>
                            </bk-form-item>
                        </bk-form>
                    </template>
                </bk-table-column>
                <bk-table-column v-if="!disabled" :label="$t('operation')" :width="currentLanguage === 'zh-cn' ? 80 : 100">
                    <template #default="{ $index }">
                        <Icon class="hover-btn" size="24" name="icon-delete" @click.native="replicaTaskObjects.splice($index, 1)" />
                    </template>
                </bk-table-column>
            </bk-table>
        </div>
        <repo-dialog :show="showAddDialog" :insert-filter-repo-list="insertFilterRepoList" :replica-task-objects="replicaTaskObjects" @confirm="confirm" @cancel="showAddDialog = false"></repo-dialog>
    </div>
</template>
<script>
    import repoDialog from './repoDialog'
    export default {
        name: 'repositoryTable',
        components: { repoDialog },
        props: {
            initData: {
                type: Array,
                default: () => []
            },
            disabled: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                showAddDialog: false,
                replicaTaskObjects: [],
                insertFilterRepoList: []
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            initData: {
                handler (data) {
                    this.replicaTaskObjects = JSON.parse(JSON.stringify(data)).map(repo => {
                        return {
                            ...repo,
                            type: repo.repoType,
                            name: repo.localRepoName,
                            projectId: this.projectId,
                            targetProject: repo.remoteProjectId || '',
                            targetStore: repo.remoteRepoName || '',
                            fid: repo.remoteProjectId + repo.remoteRepoName
                        }
                    })
                },
                immediate: true
            }
        },
        methods: {
            getRules (type) {
                return {
                    // 目标项目正则
                    targetProject: [
                        {
                            validator: function (val) {
                                return !!val.match(/^[a-z0-9]{6}$|^$/)
                            },
                            message: this.$t('projectIdCheckTips'),
                            trigger: 'blur'
                        }
                    ],
                    // 目标仓库正则
                    targetStore: [
                        {
                            validator: function (val) {
                                const regex = type === 'docker' ? /^[a-z][a-z0-9\-_]{1,31}$|^$/ : /^[a-zA-Z][a-zA-Z0-9\-_]{1,31}$|^$/
                                return !!val.match(regex)
                            },
                            message: type === 'docker' ? this.$t('repoDockerNamePlaceholder') : this.$t('repoNamePlaceholder'),
                            trigger: 'blur'
                        }
                    ]
                }
            },
            confirm (repoList) {
                const repoMap = new Map(repoList.map(v => [v.fid, { ...v, targetProject: '', targetStore: '' }]))

                this.replicaTaskObjects.forEach(v => {
                    if (repoMap.has(v.fid)) {
                        const repo = repoMap.get(v.fid)
                        repo.targetProject = v.targetProject || ''
                        repo.targetStore = v.targetStore || ''
                    }
                })

                this.replicaTaskObjects = Array.from(repoMap.values())
                this.$emit('clearError')
            },
            getConfigCheck () {
                return new Promise((resolve, reject) => {
                    // eslint-disable-next-line prefer-promise-reject-errors
                    this.replicaTaskObjects.length ? resolve([]) : reject()
                })
            },
            getConfig () {
                const refs = this.replicaTaskObjects.map(v => {
                    return this.$refs[v.fid].validate()
                })
                return new Promise((resolve, reject) => {
                    Promise.all(refs).then(res => {
                        const replicaTaskObjects = this.replicaTaskObjects.map(v => {
                            return {
                                localRepoName: v.name,
                                remoteProjectId: v.targetProject || v.projectId,
                                remoteRepoName: v.targetStore || v.name,
                                repoType: v.type
                            }
                        })
                        // eslint-disable-next-line prefer-promise-reject-errors
                        replicaTaskObjects.length ? resolve(replicaTaskObjects) : reject()
                    }).catch(() => {
                        // eslint-disable-next-line prefer-promise-reject-errors
                        reject()
                    })
                })
            },
            // 强制内部参数
            filterChange (cb) {
                this.replicaTaskObjects = this.replicaTaskObjects.filter(v => cb(v))
            },
            // 获取过滤列表
            setInsertFilterRepoList (v) {
                this.insertFilterRepoList = v
            },
            stateRender (h) {
                return h('div', {
                    class: 'flex-align-center'
                }, [
                    h('div', {
                        style: {
                            width: '130px'
                        }
                    }, this.$t('TargetProject')),
                    h('div', {
                        style: {
                            width: '20px'
                        }
                    }),
                    h('div', {
                        style: {
                            width: '130px'
                        }
                    }, this.$t('TargetStore'))
                ])
            }
        }
    }
</script>
