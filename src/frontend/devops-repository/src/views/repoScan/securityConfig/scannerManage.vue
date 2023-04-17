<template>
    <div class="license-manage-container" v-bkloading="{ isLoading }">
        <div class="mt10 flex-between-center">
            <bk-button class="ml20" icon="plus" theme="primary" @click="upload">{{ $t('upload') }}</bk-button>
            <bk-select
                class="mr20 w250"
                v-model="scannerFilter"
                @change="handlerPaginationChange()">
                <bk-option v-for="scanner in scannerList" :key="scanner.name" :id="scanner.name" :name="scanner.name"></bk-option>
            </bk-select>
        </div>
        <bk-table
            class="mt10"
            height="calc(100% - 100px)"
            :data="dataList"
            :outer-border="false"
            :row-border="false"
            size="small">
            <template #empty>
                <empty-data :is-loading="isLoading"></empty-data>
            </template>
            <bk-table-column label="漏洞包名称" prop="name"></bk-table-column>
            <bk-table-column label="关联扫描器">
                <template #default="{ row }">{{ getScannerName(row) }}</template>
            </bk-table-column>
            <bk-table-column :label="$t('lastModifiedDate')" prop="lastModifiedDate" width="200">
                <template #default="{ row }">{{ formatDate(row.lastModifiedDate) }}</template>
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
        <upload-dialog ref="uploadDialog" height-num="375" @update="getDataListHandler">
            <bk-form class="mb20" :label-width="90">
                <bk-form-item label="关联扫描器">
                    <bk-select
                        class="w250"
                        v-model="scannerName"
                        placeholder="请选择扫描器"
                        :clearable="false">
                        <bk-option v-for="scanner in scannerList" :key="scanner.name" :id="scanner.name" :name="scanner.name"></bk-option>
                    </bk-select>
                </bk-form-item>
            </bk-form>
        </upload-dialog>
    </div>
</template>
<script>
    import uploadDialog from '@repository/views/repoScan/securityConfig/uploadDialog'
    import { mapActions } from 'vuex'
    import { formatDate } from '@repository/utils'
    export default {
        name: 'user',
        components: { uploadDialog },
        data () {
            return {
                isLoading: false,
                scannerFilter: '',
                scannerName: '',
                scannerList: [],
                dataList: [],
                pagination: {
                    count: 0,
                    current: 1,
                    limit: 20,
                    limitList: [10, 20, 40]
                }
            }
        },
        watch: {
            scannerName (val) {
                this.$refs.uploadDialog.setData({
                    fullPath: `${val}`
                }, true)
            }
        },
        created () {
            this.getScannerList().then(res => {
                this.scannerList = res.filter(v => v.type !== 'scancodeToolkit')
                this.scannerName = this.scannerList[0]?.name || ''
            })
            this.handlerPaginationChange()
        },
        methods: {
            formatDate,
            ...mapActions([
                'getScannerList',
                'searchPackageList'
            ]),
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getDataListHandler()
            },
            getDataListHandler () {
                this.isLoading = true
                this.searchPackageList({
                    projectId: 'public-global',
                    repoType: 'generic',
                    repoName: 'vuldb-repo',
                    extRules: this.scannerFilter
                        ? [
                            {
                                field: 'path',
                                value: `/${this.scannerFilter}/`,
                                operation: 'EQ'
                            }
                        ]
                        : [
                            {
                                field: 'path',
                                value: ['/spdx-license/'],
                                operation: 'NIN'
                            }
                        ],
                    current: this.pagination.current,
                    limit: this.pagination.limit
                }).then(({ records, totalRecords }) => {
                    this.dataList = records
                    this.pagination.count = totalRecords
                }).finally(() => {
                    this.isLoading = false
                })
            },
            upload () {
                this.$refs.uploadDialog.setData({
                    projectId: 'public-global',
                    repoName: 'vuldb-repo',
                    show: true,
                    title: '上传',
                    fullPath: `${this.scannerName}`
                })
            },
            getScannerName ({ fullPath }) {
                const scannerType = fullPath.replace(/^\/([^/]+)\/[^/]+$/, '$1')
                const scanner = this.scannerList.find(s => s.name === scannerType)
                // 根据后端同学要求，此处需要先根据当前行返回的数据的 path去除前后 / 后去匹配是否存在于支持的扫描器的数组中的name，
                // 如果存在则回显匹配到的扫描器数组的name
                // 如果不存在，则用当前行返回的数据的 path去除前后 / 后去匹配支持的扫描器数组中的 type ，然后返回匹配到的扫描器数组的 name
                return scanner?.name || this.scannerList.find(s => s.type === scannerType)?.name
            }
        }
    }
</script>
<style lang="scss" scoped>
.license-manage-container {
    height: 100%;
    overflow: hidden;
}
</style>
