<template>
    <div class="bw-container" v-bkloading="{ isLoading }">
        <div class="ml20 mr20 mt10 flex-between-center">
            <DefaultTabBox
                :tabs="tabList"
                id-key="name"
                label-key="label"
                :current-tab="currentTab"
                @tabChang="tabChang" />
            <div class="flex-align-center">
                <bk-button theme="primary" class="mr10" @click="addBlackWhiteRepo">{{$t('add')}}</bk-button>
                <FilterCondition :filter-params="filterParams" @confirm="search" @reset="reset" />
            </div>
        </div>
        <bk-table
            class="mt10 scan-table"
            height="calc(100% - 100px)"
            :data="blackWhiteList"
            :outer-border="false"
            :row-border="false"
            row-key="id"
            size="small">
            <template #empty>
                <empty-data :is-loading="isLoading" :search="Boolean(scanName)"></empty-data>
            </template>
            <!-- 制品包名称 -->
            <bk-table-column :label="$t('composerInputLabel')" show-overflow-tooltip>
                <template #default="{ row }">
                    <span class="hover-btn">{{row.packageName}}</span>
                </template>
            </bk-table-column>
            <!-- 版本 -->
            <bk-table-column :label="$t('version')" prop="name">
            </bk-table-column>
            <!-- 仓库类型 -->
            <bk-table-column :label="$t('storeTypes')" prop="type">
            </bk-table-column>
            <!-- 所属仓库 -->
            <bk-table-column :label="$t('repo')" prop="repoName">
            </bk-table-column>
            <!-- 有效时间 -->
            <bk-table-column v-if="type === 'white'" :label="$t('validTime')" prop="repoName">
            </bk-table-column>
            <!-- 操作 -->
            <bk-table-column :label="$t('operation')">
                <template #default="{ row }">
                    <bk-button theme="primary" text @click="removeBlackWhiteList(row)">{{$t('delete')}}</bk-button>
                </template>
            </bk-table-column>
        </bk-table>
        <bk-pagination
            class="p10"
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
        <AddBlackWhiteRepoDialog :title="addConfig.title" :visible="addConfig.visible" @cancel="hideBlackWhiteRepo" @submit="addBlackWhiteRepoSubmit" />
    </div>
</template>
<script>
    import { cloneDeep } from 'lodash'
    import { mapActions } from 'vuex'
    import DefaultTabBox from '@repository/components/DefaultTabBox'
    import FilterCondition from './components/FilterCondition.vue'
    import AddBlackWhiteRepoDialog from './components/AddBlackWhiteRepoDialog.vue'
    const paginationParams = {
        count: 0,
        current: 1,
        limit: 20,
        limitList: [10, 20, 40]
    }
    export default {
        name: 'black-white-list',
        components: {
            DefaultTabBox,
            FilterCondition,
            AddBlackWhiteRepoDialog
        },
        props: {
            projectId: String
        },
        data () {
            return {
                isLoading: false,
                tabList: [
                    {
                        name: 'white',
                        label: this.$t('whiteList')
                    },
                    {
                        name: 'black',
                        label: this.$t('blackList')
                    }
                ],
                type: 'white',
                pagination: cloneDeep(paginationParams),
                filterParams: null,

                addConfig: {
                    title: '',
                    visible: false
                }
            }
        },
        computed: {
            currentTab () {
                return this.tabList.find(tab => tab.name === this.type)
            },
            // 必选查询条件
            defaultFilterParams () {
                return {
                    field: 'projectId',
                    value: this.projectId,
                    operation: 'EQ'
                }
            },
            // 搜索参数
            searchFilterParams () {
                const deepFilterParams = cloneDeep(this.filterParams) || []
                return deepFilterParams
            }
        },
        created () {
            this.handlerPaginationChange({ current: 1, limit: 20 })
        },
        methods: {
            ...mapActions([
                'createBlackWhiteList',
                'deleteBlackWhiteList',
                'getBlackWhiteRecords'
            ]),
            addBlackWhiteRepo () {
                this.addConfig.visible = true
                this.addConfig.title = this.type === 'white' ? (this.$t('add') + this.$t('whiteList')) : (this.$t('add') + this.$t('blackList'))
            },
            hideBlackWhiteRepo () {
                this.addConfig.visible = false
            },
            addBlackWhiteRepoSubmit (form, cb) {
                this.createBlackWhiteList({
                    type: 'MAVEN',
                    projectId: this.projectId,
                    key: form.name,
                    pass: this.type === 'white',
                    version: null,
                    versionRuleType: null
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('addSuccess')
                    })
                    this.getBlackWhiteList({ current: 1 })
                }).catch(() => {
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('addFail')
                    })
                }).finally(() => {
                    cb && cb()
                })
            },
            // 分页
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getBlackWhiteList()
            },
            // 获取黑白名单列表
            getBlackWhiteList () {
                const body = {
                    pageNumber: this.pagination.current,
                    pageSize: this.pagination.limit,
                    type: 'MAVEN',
                    pass: this.type === 'white'
                }
                this.isLoading = true
                
                return this.getBlackWhiteRecords(
                    body
                ).then(res => {
                    this.blackWhiteList = res.records
                    this.pagination.count = res.totalRecords
                }).finally(() => {
                    this.isLoading = false
                })
            },
            // 搜索
            search (filterList, cb) {
                this.filterParams = filterList
                this.$nextTick(() => {
                    this.getBlackWhiteList({ current: 1 }).then(() => {
                        cb && cb()
                    })
                })
            },
            reset (cb) {
                this.filterParams = null
                this.$nextTick(() => {
                    this.getBlackWhiteList({ current: 1 }).then(() => {
                        cb && cb()
                    })
                })
            },
            tabChang (val) {
                this.type = val.name
                this.$nextTick(() => {
                    this.getBlackWhiteList({ current: 1 })
                })
            },
            removeBlackWhiteList (row) {
                this.$bkInfoDevopsConfirm({
                    subTitle: this.$t('deleteBlackWhiteTips', [this.active === 'white' ? this.$t('whiteList') : this.$t('blackList')]),
                    theme: 'danger',
                    confirmFn: () => {
                        const body = {
                            packageType: 'MAVEN',
                            projectId: this.projectId,
                            key: row.name,
                            version: null,
                            versionRuleType: null,
                            pass: this.type === 'white'
                        }
                        this.deleteBlackWhiteList(body).then(() => {
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('removeSuccess')
                            })
                            let current = 0
                            if (this.blackWhiteList.length === 1) {
                                if (this.pagination.current === 1) {
                                    current = 1
                                } else {
                                    current = this.pagination.current - 1
                                }
                            } else {
                                current = this.pagination.current
                            }
                            this.getBlackWhiteList({ current: current })
                        }).catch(() => {
                            this.$bkMessage({
                                theme: 'error',
                                message: this.$t('removeFail')
                            })
                        })
                    }
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.bw-container {
    height: 100%;
    width: 100%;
    background-color: white;
}
</style>
