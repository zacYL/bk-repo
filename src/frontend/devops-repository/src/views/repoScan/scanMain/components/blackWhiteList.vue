<template>
    <div class="bw-container" v-bkloading="{ isLoading }">
        <div class="ml20 mr20 mt10 flex-between-center">
            <DefaultTabBox
                :tabs="tabList"
                id-key="name"
                label-key="label"
                :current-tab="currentTab"
                @tabChang="tabChang" />
            <FilterCondition :wb-type="type" @confirm="search" />
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
            <bk-table-column :label="$t('version')">
                <template #default="{ row }">{{row.name}}</template>
            </bk-table-column>
            <!-- 仓库类型 -->
            <bk-table-column :label="$t('storeTypes')">
                <template #default="{ row }">{{row.type}}</template>
            </bk-table-column>
            <!-- 所属仓库 -->
            <bk-table-column :label="$t('repo')">
            </bk-table-column>
            <!-- 启用状态 -->
            <bk-table-column :label="$t('enabledStatus')">
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
        data () {
            return {
                defaultFilterParams: {
                    field: 'projectId',
                    value: this.projectId,
                    operation: 'EQ'
                },
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
                pagination: cloneDeep(paginationParams)
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            currentTab () {
                return this.tabList.find(tab => tab.name === this.type)
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
            tabChang (val) {
                this.type = val.name
            },
            // 获取黑白名单列表
            getBlackWhiteList (filterList = []) {
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
                            ...filterList
                        ],
                        relation: 'AND'
                    }
                }
                this.isLoading = true
                const url = '/repository/api/packageVersion/search'
                this.$ajax.post(
                    url,
                    body
                ).then(res => {
                    this.blackWhiteListList = res.data.records
                    this.pagination.count = res.data.total
                }).finally(() => {
                    this.isLoading = false
                })
            },
            // 搜索
            search (filterList) {
                this.getBlackWhiteList(filterList)
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
