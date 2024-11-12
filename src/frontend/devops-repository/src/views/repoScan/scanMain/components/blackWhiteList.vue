<template>
    <div class="bw-container" v-bkloading="{ isLoading }">
        <div class="ml20 mr20 mt10 flex-between-center">
            <DefaultTabBox
                :tabs="tabList"
                id-key="name"
                label-key="label"
                :current-tab="currentTab"
                @tabChang="tabChang" />
            <FilterCondition :filter-params="filterParams" @confirm="search" @reset="reset" />
        </div>
        <bk-table
            class="mt10 scan-table"
            height="calc(100% - 100px)"
            :data="blackWhiteListList"
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
            <!-- 启用状态 -->
            <bk-table-column :label="$t('enabledStatus')">
                <template #default="{ row }">
                    <bk-switcher disabled :value="!!(row.metadata[0]?.value)"></bk-switcher>
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
    </div>
</template>
<script>
    import { cloneDeep } from 'lodash'
    import DefaultTabBox from '@repository/components/DefaultTabBox'
    import FilterCondition from './components/FilterCondition.vue'
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
            FilterCondition
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
                filterParams: null
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
                // 替换 startType 为 黑白名单指定字段
                const bwTarget = deepFilterParams.find(item => item.field === 'startType')
                if (bwTarget) bwTarget.field = this.type === 'white' ? 'metadata.exemptEnabled' : 'metadata.forbidEnabled'
                return this.filterParams
                    ? deepFilterParams
                    : [{
                        field: this.type === 'white' ? 'metadata.exemptEnabled' : 'metadata.forbidEnabled',
                        operation: 'NOT_NULL'
                    }]
            }
        },
        created () {
            this.handlerPaginationChange({ current: 1, limit: 20 })
        },
        methods: {
            // 分页
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getBlackWhiteList()
            },
            // 获取黑白名单列表
            getBlackWhiteList () {
                const body = {
                    page: {
                        pageNumber: this.pagination.current,
                        pageSize: this.pagination.limit
                    },
                    sort: {
                        properties: [
                            'lastModifiedDate'
                        ],
                        direction: 'DESC'
                    },
                    rule: {
                        rules: [
                            this.defaultFilterParams,
                            ...this.searchFilterParams
                        ],
                        relation: 'AND'
                    }
                }
                this.isLoading = true
                const url = '/repository/api/packageVersion/search'
                return this.$ajax.post(
                    url,
                    body
                ).then(res => {
                    this.blackWhiteListList = res.records
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
