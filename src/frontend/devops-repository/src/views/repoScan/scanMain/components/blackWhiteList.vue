<template>
    <div class="bw-container" v-bkloading="{ isLoading }">
        <div class="ml20 mr20 mt10 flex-between-center">
            <DefaultTabBox
                :tabs="tabList"
                id-key="name"
                label-key="label"
                :current-tab="currentTab"
                @tabChang="tabChang" />
        </div>
        <bk-table
            class="mt10 scan-table"
            height="calc(100% - 100px)"
            :data="scanList"
            :outer-border="false"
            :row-border="false"
            row-key="id"
            size="small">
            <template #empty>
                <empty-data :is-loading="isLoading" :search="Boolean(scanName)"></empty-data>
            </template>
            <bk-table-column :label="$t('schemeName')" show-overflow-tooltip>
                <template #default="{ row }">
                    <span class="hover-btn" @click="showScanReport(row)">{{row.name}}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('schemeType')">
                <template #default="{ row }">{{ $t(`scanTypeEnum.${row.planType}`) }}</template>
            </bk-table-column>
            <!-- <bk-table-column :label="$t('scanner')" prop="scanner" show-overflow-tooltip></bk-table-column> -->
            <bk-table-column :label="$t('scanStatus')">
                <template #default="{ row }">
                    <span class="repo-tag" :class="row.status">{{ $t(`scanStatusEnum.${row.status}`)}}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('lastScanTime')">
                <template #default="{ row }">{{formatDate(row.lastScanDate)}}</template>
            </bk-table-column>
            <bk-table-column :label="$t('operation')" width="100">
                <template #default="{ row }">
                    <operation-list
                        :list="[
                            !row.readOnly && { label: $t('setting'), clickEvent: () => showScanConfig(row) },
                            { label: $t('suspend'), clickEvent: () => stopScanHandler(row) },
                            !row.readOnly && { label: $t('scan'), clickEvent: () => startScanHandler(row) },
                            !row.readOnly && { label: $t('delete'), clickEvent: () => deleteScanHandler(row) }
                        ]"></operation-list>
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
    const paginationParams = {
        count: 0,
        current: 1,
        limit: 20,
        limitList: [10, 20, 40]
    }
    export default {
        name: 'scan-solution',
        components: {
            DefaultTabBox
        },
        data () {
            return {
                isLoading: false,
                tabList: [
                    {
                        name: 'white',
                        label: '白名单'
                    },
                    {
                        name: 'black',
                        label: '黑名单'
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
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
            },
            tabChang (val) {
                this.type = val.name
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
