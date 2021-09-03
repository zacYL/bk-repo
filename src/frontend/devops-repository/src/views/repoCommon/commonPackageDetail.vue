<template>
    <div class="common-package-detail flex-align-center">
        <aside class="common-package-version">
            <header class="pl30 version-header flex-align-center">制品版本</header>
            <div class="p20 version-search">
                <bk-input
                    v-model.trim="versionInput"
                    placeholder="请输入版本, 按Enter键搜索"
                    clearable
                    @enter="handlerPaginationChange()"
                    @clear="handlerPaginationChange()"
                    right-icon="bk-icon icon-search">
                </bk-input>
            </div>
            <div class="version-list">
                <div class="version-item"></div>
            </div>
        </aside>
        <div class="common-package-info flex-1">
            <!-- <bk-tab class="common-package-info-main" type="unborder-card" :active.sync="tabName">
                <bk-tab-panel name="commonVersion" :label="$t('version')" v-bkloading="{ isLoading }">
                    <div class="common-package-version-1">
                        <div class="mb20 flex-align-center">
                            <bk-input
                                class="common-version-search"
                                v-model.trim="versionInput"
                                clearable
                                :placeholder="$t('versionPlacehodler')"
                                @enter="handlerPaginationChange()"
                                @clear="handlerPaginationChange()">
                            </bk-input>
                            <i class="common-version-search-btn devops-icon icon-search" @click="handlerPaginationChange()"></i>
                        </div>
                        <bk-table
                            class="common-version-table"
                            height="calc(100% - 140px)"
                            :data="versionList"
                            :outer-border="false"
                            :row-border="false"
                            :row-style="{ cursor: 'pointer' }"
                            size="small"
                            @row-click="toCommonVersionDetail"
                        >
                            <bk-table-column :label="$t('version')" prop="name"></bk-table-column>
                            <bk-table-column :label="$t('artiStatus')">
                                <template v-if="props.row.stageTag" slot-scope="props">
                                    <span class="mr5 repo-tag" v-for="tag in props.row.stageTag"
                                        :key="props.row.tag + tag">{{ tag }}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('size')">
                                <template slot-scope="props">
                                    {{ convertFileSize(props.row.size) }}
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('downloads')" prop="downloads"></bk-table-column>
                            <bk-table-column :label="$t('lastModifiedBy')">
                                <template slot-scope="props">
                                    {{ userList[props.row.lastModifiedBy] ? userList[props.row.lastModifiedBy].name : props.row.lastModifiedBy }}
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('lastModifiedDate')">
                                <template slot-scope="props">
                                    {{ formatDate(props.row.lastModifiedDate) }}
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('operation')" width="150">
                                <template slot-scope="props">
                                    <bk-button class="mr20"
                                        :disabled="(props.row.stageTag || '').includes('@release')"
                                        @click.stop="changeStageTagHandler(props.row)" text theme="primary">
                                        <i class="devops-icon icon-arrows-up"></i>
                                    </bk-button>
                                    <bk-button v-if="repoType !== 'docker'" class="mr20" @click.stop="downloadPackageHandler(props.row)" text theme="primary">
                                        <i class="devops-icon icon-download"></i>
                                    </bk-button>
                                    <bk-button @click.stop="deleteVersionHandler(props.row)" text theme="primary">
                                        <i class="devops-icon icon-delete"></i>
                                    </bk-button>
                                </template>
                            </bk-table-column>
                        </bk-table>
                        <bk-pagination
                            class="mt10"
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
                </bk-tab-panel>
            </bk-tab> -->
        </div>
        
        <bk-dialog
            v-model="formDialog.show"
            :title="$t('upgrade')"
            :close-icon="false"
            :quick-close="false"
            width="600"
            header-position="left">
            <bk-form :label-width="120" :model="formDialog" :rules="rules" ref="formDialog">
                <bk-form-item :label="$t('upgradeTo')" :required="true" property="tag">
                    <bk-radio-group v-model="formDialog.tag">
                        <bk-radio :disabled="!!formDialog.default.length" value="@prerelease">@prerelease</bk-radio>
                        <bk-radio class="ml20" value="@release">@release</bk-radio>
                    </bk-radio-group>
                </bk-form-item>
            </bk-form>
            <div slot="footer">
                <bk-button ext-cls="mr5" :loading="formDialog.loading" theme="primary" @click.stop.prevent="submitFormDialog">{{$t('submit')}}</bk-button>
                <bk-button ext-cls="mr5" theme="default" @click.stop="cancelFormDialog">{{$t('cancel')}}</bk-button>
            </div>
        </bk-dialog>
    </div>
</template>
<script>
    import { convertFileSize, formatDate } from '@/utils'
    import commonMixin from './commonMixin'
    import { mapState, mapActions } from 'vuex'
    export default {
        name: 'commonPackageDetail',
        mixins: [commonMixin],
        data () {
            return {
                tabName: 'commonVersion',
                isLoading: false,
                infoLoading: false,
                formDialog: {
                    show: false,
                    loading: false,
                    version: '',
                    default: [],
                    tag: ''
                },
                rules: {
                    tag: [
                        {
                            required: true,
                            message: this.$t('pleaseSelect') + this.$t('tag'),
                            trigger: 'blur'
                        }
                    ]
                },
                pkg: {
                    name: '',
                    key: '',
                    downloads: 0,
                    versions: 0,
                    latest: '1.9',
                    lastModifiedBy: '',
                    lastModifiedDate: new Date()
                },
                versionInput: '',
                versionList: [],
                pagination: {
                    count: 0,
                    current: 1,
                    limit: 20,
                    'limit-list': [10, 20, 40]
                }
            }
        },
        computed: {
            ...mapState(['userList'])
        },
        created () {
            this.getPackageInfoHandler()
            this.handlerPaginationChange()
        },
        methods: {
            formatDate,
            convertFileSize,
            ...mapActions([
                'getPackageInfo',
                'getVersionList',
                'changeStageTag',
                'deleteVersion'
            ]),
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getVersionListHandler()
            },
            getVersionListHandler () {
                this.isLoading = true
                this.getVersionList({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    packageKey: this.packageKey,
                    current: this.pagination.current,
                    limit: this.pagination.limit,
                    version: this.versionInput
                }).then(({ records, totalRecords }) => {
                    this.versionList = records
                    this.pagination.count = totalRecords
                }).finally(() => {
                    this.isLoading = false
                })
            },
            getPackageInfoHandler () {
                this.infoLoading = true
                this.getPackageInfo({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    packageKey: this.packageKey
                }).then(info => {
                    this.pkg = info
                }).finally(() => {
                    this.infoLoading = false
                })
            },
            toCommonVersionDetail (row) {
                this.$router.push({
                    name: 'commonVersion',
                    query: {
                        name: this.repoName,
                        package: this.packageKey,
                        version: row.name
                    }
                })
            },
            changeStageTagHandler (row) {
                this.formDialog = {
                    show: true,
                    loading: false,
                    version: row.name,
                    default: row.stageTag,
                    tag: ''
                }
            },
            async submitFormDialog () {
                await this.$refs.formDialog.validate()
                this.formDialog.loading = true
                this.changeStageTag({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    packageKey: this.packageKey,
                    version: this.formDialog.version,
                    tag: this.formDialog.tag
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('upgrade') + this.$t('success')
                    })
                    this.cancelFormDialog()
                    this.getVersionListHandler()
                }).finally(() => {
                    this.formDialog.loading = false
                })
            },
            cancelFormDialog () {
                this.$refs.formDialog.clearError()
                this.formDialog.show = false
            },
            downloadPackageHandler (row) {
                const url = `/repository/api/version/download/${this.projectId}/${this.repoName}?packageKey=${this.packageKey}&version=${row.name}`
                this.$ajax.head(url).then(() => {
                    window.open(
                        '/web' + url,
                        '_self'
                    )
                }).catch(e => {
                    this.$bkMessage({
                        theme: 'error',
                        message: e.status !== 404 ? e.message : this.$t('fileNotExist')
                    })
                })
            },
            deleteVersionHandler (row) {
                this.$bkInfo({
                    type: 'warning',
                    theme: 'warning',
                    title: this.$t('deleteVersionTitle', { version: this.version }),
                    subTitle: this.$t('deleteVersionSubTitle'),
                    showFooter: true,
                    confirmFn: () => {
                        this.deleteVersion({
                            projectId: this.projectId,
                            repoType: this.repoType,
                            repoName: this.repoName,
                            packageKey: this.packageKey,
                            version: row.name
                        }).then(() => {
                            this.getVersionListHandler()
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
.common-package-detail {
    height: 100%;
    .common-package-version {
        width: 250px;
        height: 100%;
        margin-right: 10px;
        background-color: white;
        .version-header {
            height: 50px;
            color: var(--fontWeightColor);
            border-bottom: 1px solid var(--borderWeightColor);
        }
        .version-search {

        }
    }
}
</style>
