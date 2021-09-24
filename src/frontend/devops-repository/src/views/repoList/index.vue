<template>
    <div class="repo-list-container" v-bkloading="{ isLoading }">
        <div class="ml15 mr15 mt10 repo-list-search flex-between-center">
            <bk-button v-if="canCreate" icon="plus" theme="primary" @click="createRepo"><span class="mr5">{{ $t('create') }}</span></bk-button>
            <div class="flex-align-center">
                <bk-input
                    v-model.trim="query.name"
                    class="w250"
                    placeholder="请输入仓库名称, 按Enter键搜索"
                    clearable
                    @enter="handlerPaginationChange()"
                    @clear="handlerPaginationChange()"
                    right-icon="bk-icon icon-search">
                </bk-input>
                <bk-select
                    v-model="query.type"
                    class="ml10 w250"
                    @change="handlerPaginationChange()"
                    :placeholder="$t('allTypes')">
                    <bk-option v-for="type in repoEnum" :key="type" :id="type" :name="type">
                        <div class="flex-align-center">
                            <Icon size="24" :name="type" />
                            <span class="ml10 flex-1 text-overflow">{{type}}</span>
                        </div>
                    </bk-option>
                </bk-select>
            </div>
        </div>
        <bk-table
            class="mt10"
            :data="repoList"
            height="calc(100% - 104px)"
            :outer-border="false"
            :row-border="false"
            size="small"
            :row-style="({ row }) => ({ 'color': row.hasPermission ? '' : '#dcdee5 !important' })"
            @row-click="toPackageList">
            <template #empty>
                <empty-data :search="Boolean(query.name || query.type)">
                    <template v-if="!Boolean(query.name || query.type) && canCreate">
                        <span class="ml10">暂无仓库数据，</span>
                        <bk-button text @click="createRepo">即刻创建</bk-button>
                    </template>
                </empty-data>
            </template>
            <bk-table-column :label="$t('repoName')">
                <template #default="{ row }">
                    <div class="flex-align-center" :title="replaceRepoName(row.name)">
                        <Icon size="24" :name="row.repoType" />
                        <span class="ml10 w250 flex-1 text-overflow hover-btn">{{replaceRepoName(row.name)}}</span>
                    </div>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('createdDate')">
                <template #default="{ row }">
                    {{ formatDate(row.createdDate) }}
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('createdBy')">
                <template #default="{ row }">
                    {{ userList[row.createdBy] ? userList[row.createdBy].name : row.createdBy }}
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('operation')" width="100">
                <template #default="{ row }"><template v-if="row.hasPermission">
                    <i class="mr10 devops-icon icon-cog hover-btn" @click.stop="toRepoConfig(row)"></i>
                    <i v-if="row.repoType !== 'generic'" class="devops-icon icon-delete hover-btn" @click.stop="deleteRepo(row)"></i>
                </template></template>
            </bk-table-column>
        </bk-table>
        <bk-pagination
            class="m10"
            size="small"
            align="right"
            show-total-count
            @change="current => handlerPaginationChange({ current })"
            @limit-change="limit => handlerPaginationChange({ limit })"
            :current.sync="pagination.current"
            :limit="pagination.limit"
            :count="pagination.count"
            :limit-list="pagination.limitList">
        </bk-pagination>
        <create-repo-dialog ref="createRepo" @refresh="handlerPaginationChange()"></create-repo-dialog>
    </div>
</template>
<script>
    import createRepoDialog from './createRepoDialog'
    import { mapState, mapActions } from 'vuex'
    import { repoEnum } from '@/store/publicEnum'
    import { formatDate } from '@/utils'
    export default {
        name: 'repoList',
        components: { createRepoDialog },
        data () {
            return {
                repoEnum,
                isLoading: false,
                canCreate: false,
                repoList: [],
                query: {
                    name: '',
                    type: ''
                },
                pagination: {
                    count: 0,
                    current: 1,
                    limit: 20,
                    limitList: [10, 20, 40]
                }
            }
        },
        computed: {
            ...mapState(['userList']),
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            projectId () {
                this.handlerPaginationChange()
            }
        },
        created () {
            this.handlerPaginationChange()
            this.getRepoPermission({
                projectId: this.projectId,
                action: 'create'
            }).then(res => {
                this.canCreate = res
            })
        },
        methods: {
            formatDate,
            ...mapActions([
                'getRepoList',
                'deleteRepoList',
                'getRepoPermission'
            ]),
            getListData () {
                this.isLoading = true
                this.getRepoList({
                    projectId: this.projectId,
                    ...this.pagination,
                    ...this.query
                }).then(({ records, totalRecords }) => {
                    this.repoList = records.map(v => ({ ...v, repoType: v.type.toLowerCase() }))
                    this.pagination.count = totalRecords
                }).finally(() => {
                    this.isLoading = false
                })
            },
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getListData()
            },
            createRepo () {
                this.$refs.createRepo.showDialogHandler()
            },
            toPackageList ({ hasPermission, repoType, name }) {
                if (!hasPermission) return
                this.$router.push({
                    name: repoType === 'generic' ? 'repoGeneric' : 'commonList',
                    params: {
                        projectId: this.projectId,
                        repoType
                    },
                    query: {
                        repoName: name
                    }
                })
            },
            toRepoConfig ({ hasPermission, repoType, name }) {
                if (!hasPermission) return
                this.$router.push({
                    name: 'repoConfig',
                    params: {
                        ...this.$route.params,
                        repoType
                    },
                    query: {
                        repoName: name
                    }
                })
            },
            deleteRepo ({ hasPermission, name }) {
                if (!hasPermission) return
                this.$confirm({
                    theme: 'danger',
                    message: this.$t('deleteRepoTitle', { name }),
                    confirmFn: () => {
                        return this.deleteRepoList({
                            projectId: this.projectId,
                            name
                        }).then(() => {
                            this.getListData()
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('delete') + this.$t('success')
                            })
                        })
                    }
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.repo-list-container {
    height: 100%;
    background-color: white;
}
</style>
