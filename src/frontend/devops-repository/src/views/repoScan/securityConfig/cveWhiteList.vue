<template>
    <div class="white-list-container" v-bkloading="{ isLoading }">
        <div class="mt10 flex-between-center">
            <bk-button class="ml20" icon="plus" theme="primary" @click="addWhiteList">{{ $t('add') }}</bk-button>
            <bk-input
                v-model.trim="cveId"
                class="mr20 w250"
                :placeholder="$t('vulIdSearchPlaceholder')"
                clearable
                @enter="handlerPaginationChange()"
                @clear="handlerPaginationChange()"
                right-icon="bk-icon icon-search">
            </bk-input>
        </div>
        <bk-table
            class="mt10"
            height="calc(100% - 100px)"
            :data="cveList"
            :outer-border="false"
            :row-border="false"
            size="small">
            <template #empty>
                <empty-data :is-loading="isLoading" :search="Boolean(name)"></empty-data>
            </template>
            <bk-table-column :label="'CVE' + $t('space') + $t('serialNumber')" prop="cveId"></bk-table-column>
            <bk-table-column :label="$t('createdDate')">
                <template #default="{ row }">{{formatDate(row.createdDate)}}</template>
            </bk-table-column>
            <bk-table-column :label="$t('operation')" width="100">
                <template #default="{ row }">
                    <Icon class="hover-btn" size="24" name="icon-delete"
                        @click.native.stop="deleteCve(row)" />
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
        <canway-dialog
            v-model="addCveDialog.show"
            width="528"
            height-num="281"
            :title="$t('addCVEWhiteList')"
            @cancel="addCveDialog.show = false"
            @confirm="confirmAddCve">
            <bk-input
                class="w480"
                v-model="addCveDialog.cveStr"
                type="textarea"
                :placeholder="$t('cveVulNumberPlaceholder')"
                :rows="6">
            </bk-input>
        </canway-dialog>
    </div>
</template>
<script>
    import { formatDate } from '@repository/utils'
    import { mapActions } from 'vuex'
    export default {
        name: 'user',
        data () {
            return {
                isLoading: false,
                cveId: '',
                cveList: [],
                addCveDialog: {
                    show: false,
                    cveStr: ''
                },
                pagination: {
                    count: 0,
                    current: 1,
                    limit: 20,
                    limitList: [10, 20, 40]
                }
            }
        },
        created () {
            this.handlerPaginationChange()
        },
        methods: {
            formatDate,
            ...mapActions([
                'getCveWhiteList',
                'addCveWhite',
                'deleteCveWhite'
            ]),
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getCveWhiteListHandler()
            },
            getCveWhiteListHandler () {
                this.isLoading = true
                return this.getCveWhiteList({
                    cveId: this.cveId || undefined,
                    current: this.pagination.current,
                    limit: this.pagination.limit
                }).then(({ records, totalRecords }) => {
                    this.cveList = records
                    this.pagination.count = totalRecords
                }).finally(() => {
                    this.isLoading = false
                })
            },
            addWhiteList () {
                this.addCveDialog = {
                    show: true,
                    cveStr: ''
                }
            },
            confirmAddCve () {
                const cveList = this.addCveDialog.cveStr.split(/\n/).filter(Boolean)
                this.addCveWhite({
                    body: cveList
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('add') + this.$t('space') + this.$t('success')
                    })
                    this.addCveDialog.show = false
                    this.getCveWhiteListHandler()
                })
            },
            deleteCve ({ cveId }) {
                this.deleteCveWhite({
                    cveId
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('remove') + this.$t('space') + this.$t('success')
                    })
                    this.getCveWhiteListHandler()
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.white-list-container {
    height: 100%;
    overflow: hidden;
    .hover-visible {
        visibility: hidden;
    }
    .hover-row .hover-visible {
        visibility: visible;
    }
}
</style>
