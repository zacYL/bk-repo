<template>
    <div class="license-manage-container" v-bkloading="{ isLoading }">
        <div class="mt10 flex-between-center">
            <bk-button class="ml20" icon="plus" theme="primary" @click="showUploadLicense">{{ $t('upload') }}</bk-button>
            <div class="mr20 flex-align-center">
                <bk-input
                    v-model.trim="name"
                    class="w250"
                    :placeholder="$t('nameSearchPlaceholder')"
                    clearable
                    @enter="handlerPaginationChange()"
                    @clear="handlerPaginationChange()"
                    right-icon="bk-icon icon-search">
                </bk-input>
                <bk-select
                    class="ml10 w250"
                    v-model="isTrust"
                    :placeholder="$t('compliancePlaceholder')"
                    @change="handlerPaginationChange()">
                    <bk-option id="true" :name="$t('compliance')"></bk-option>
                    <bk-option id="false" :name="$t('notCompliance')"></bk-option>
                </bk-select>
            </div>
        </div>
        <bk-table
            class="mt10"
            height="calc(100% - 100px)"
            :data="licenseList"
            :outer-border="false"
            :row-border="false"
            size="small">
            <template #empty>
                <empty-data :is-loading="isLoading" :search="Boolean(name || isTrust)"></empty-data>
            </template>
            <bk-table-column :label="$t('name')">
                <template #default="{ row }">
                    <span class="hover-btn"
                        v-bk-tooltips="{ content: row.name, placements: ['top'] }"
                        @click="showLicenseUrl(row)"
                    >{{ row.licenseId }}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="'OSI' + $t('space') + $t('authenticated')" width="160">
                <template #default="{ row }">{{ `${row.isOsiApproved ? $t('already') : $t('not')}` + $t('space') + $t('authenticated') }}</template>
            </bk-table-column>
            <bk-table-column :label="'FSF' + +$t('space') + $t('openSource')" width="150">
                <template #default="{ row }">{{ `${row.isFsfLibre ? $t('already') : $t('not')}` + $t('space') + $t('openSource') }}</template>
            </bk-table-column>
            <bk-table-column :label="$t('useStatus')" width="120">
                <template #default="{ row }">{{ `${row.isDeprecatedLicenseId ? $t('abandoned') : $t('usable')}` }}</template>
            </bk-table-column>
            <bk-table-column :label="$t('complianceQuality')" width="190">
                <template #default="{ row }">
                    <span class="repo-tag" :class="row.isTrust ? 'SUCCESS' : 'FAILED'">{{ `${row.isTrust ? $t('compliance') : $t('notCompliance')}` }}</span>
                    <bk-button class="hover-visible ml5" text theme="primary" @click="changeTrust(row)">{{$t('switch')}}</bk-button>
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
        <upload-dialog ref="uploadDialog" @update="getLicenseListHandler"></upload-dialog>
        <canway-dialog
            v-model="licenseInfo.show"
            :title="$t('certificateDetails')"
            width="500"
            :height-num="400"
            @cancel="licenseInfo.show = false">
            <bk-form class="mr10" :label-width=" currentLanguage === 'zh-cn' ? 90 : 150">
                <bk-form-item :label="$t('licenseInfo')">
                    <a style="word-break:break-all;" :href="licenseInfo.reference" target="_blank">{{ licenseInfo.reference }}</a>
                </bk-form-item>
                <bk-form-item :label="$t('referenceDocuments')">
                    <div v-for="url in licenseInfo.seeAlso" :key="url">
                        <a style="word-break:break-all;" :href="url" target="_blank">{{ url }}</a>
                    </div>
                </bk-form-item>
            </bk-form>
            <template #footer>
                <bk-button theme="default" @click="licenseInfo.show = false">{{$t('close')}}</bk-button>
            </template>
        </canway-dialog>
    </div>
</template>
<script>
    import uploadDialog from '@repository/views/repoScan/securityConfig/uploadDialog'
    import { mapActions } from 'vuex'
    export default {
        name: 'user',
        components: { uploadDialog },
        data () {
            return {
                isLoading: false,
                name: '',
                isTrust: '',
                licenseList: [],
                licenseInfo: {
                    show: false,
                    reference: '',
                    seeAlso: []
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
            ...mapActions([
                'getLicenseList',
                'editLicense'
            ]),
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getLicenseListHandler()
            },
            getLicenseListHandler () {
                this.isLoading = true
                return this.getLicenseList({
                    name: this.name || undefined,
                    isTrust: this.isTrust || undefined,
                    current: this.pagination.current,
                    limit: this.pagination.limit
                }).then(({ records, totalRecords }) => {
                    this.licenseList = records
                    this.pagination.count = totalRecords
                }).finally(() => {
                    this.isLoading = false
                })
            },
            showUploadLicense () {
                this.$refs.uploadDialog.setData({
                    projectId: 'public-global',
                    repoName: 'vuldb-repo',
                    show: true,
                    title: this.$t('uploadLicense'),
                    fullPath: '/spdx-license'
                })
            },
            changeTrust ({ licenseId, isTrust }) {
                this.editLicense({
                    licenseId,
                    isTrust: !isTrust
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('setCertificate') + this.$t('space') + `${!isTrust ? this.$t('compliance') : this.$t('notCompliance')}` + this.$t('space') + this.$t('success')
                    })
                }).finally(() => {
                    this.getLicenseListHandler()
                })
            },
            showLicenseUrl (row) {
                this.licenseInfo = {
                    show: true,
                    ...row
                }
            }
        }
    }
</script>
<style lang="scss" scoped>
.license-manage-container {
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
