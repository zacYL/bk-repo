<template>
    <div class="common-package-detail">
        <header class="mb10 pl20 pr20 common-package-header flex-align-center">
            <Icon class="package-img" size="30" :name="repoType" />
            <div class="ml10 common-package-title">
                <div class="repo-title text-overflow" :title="pkg.name">
                    {{ pkg.name }}
                </div>
            </div>
            <bk-button class="common-package-header-refresh" @click="refresh((currentVersion || {}).name)">
                {{ $t('refresh') }}
            </bk-button>
        </header>
        <div class="common-version-main flex-align-center">
            <aside class="common-version" v-bkloading="{ isLoading }">
                <header class="pl30 version-header flex-align-center">{{$t('artifactVersion')}}</header>
                <div class="version-search">
                    <bk-input
                        v-model.trim="versionInput"
                        :placeholder="$t('versionPlaceholder')"
                        clearable
                        @enter="handlerPaginationChange()"
                        @clear="handlerPaginationChange()"
                        right-icon="bk-icon icon-search">
                    </bk-input>
                </div>
                <div class="version-list">
                    <infinite-scroll
                        ref="infiniteScroll"
                        :is-loading="isLoading"
                        :has-next="versionList.length < pagination.count"
                        @load="handlerPaginationChange({ current: pagination.current + 1 }, true)">
                        <div class="mb10 list-count">{{$t('totalVersionCount', [pagination.count])}}</div>
                        <div
                            class="mb10 version-item flex-center"
                            :class="{ 'selected': $version.name === version }"
                            v-for="$version in versionList"
                            :key="$version.name"
                            @click="changeVersion($version)">
                            <span class="text-overflow" style="max-width:150px;" :title="$version.name">{{ $version.name }}</span>
                            <operation-list
                                class="version-operation"
                                :list="[
                                    ...(!$version.metadata.forbidStatus ? [
                                        (showPromotion && !$version.metadata.lockStatus) && {
                                            label: $t('upgrade'), clickEvent: () => changeStageTagHandler($version),
                                            disabled: ($version.stageTag || '').includes('@release')
                                        },
                                        !['conan', 'docker'].includes(repoType) && { label: $t('download'), clickEvent: () => downloadPackageHandler($version) }
                                    ] : []),
                                    forbidOperationPermission && !whetherSoftware && !(storeType === 'virtual') && { clickEvent: () => showLimitDialog('forbid',$version), label: $version.metadata.forbidStatus ? $t('relieve') + $t('space') + $t('forbid') : $t('forbid') },
                                    lockOperationPermission && !whetherSoftware && !(storeType === 'virtual') && { clickEvent: () => showLimitDialog('lock',$version), label: $version.metadata.lockStatus ? $t('relieve') + $t('space') + $t('lock') : $t('lock') },
                                    (deleteOperationPermission && !(storeType === 'virtual') && !$version.metadata.lockStatus) && { label: $t('delete'), clickEvent: () => deleteVersionHandler($version) }
                                ]"></operation-list>
                        </div>
                    </infinite-scroll>
                </div>
            </aside>
            <div class="common-version-detail flex-1">
                <version-detail
                    ref="versionDetail"
                    :show-update-operation="updateOperationPermission"
                    :show-delete-operation="deleteOperationPermission"
                    :show-lock-operation="lockOperationPermission"
                    :show-forbid-operation="forbidOperationPermission"
                    @tag="changeStageTagHandler()"
                    @scan="scanPackageHandler()"
                    @forbid="showLimitDialog('forbid')"
                    @lock="showLimitDialog('lock')"
                    @download="downloadPackageHandler()"
                    @delete="deleteVersionHandler()">
                </version-detail>
            </div>
        </div>
        <common-form-dialog ref="commonFormDialog" @refresh="refresh"></common-form-dialog>
        <operationLimitConfirmDialog ref="operationLimitConfirmDialog" @confirm="changeLimitStatusHandler"></operationLimitConfirmDialog>
    </div>
</template>
<script>
    import OperationList from '@repository/components/OperationList'
    import InfiniteScroll from '@repository/components/InfiniteScroll'
    import operationLimitConfirmDialog from '@repository/components/operationLimitConfirmDialog'
    import VersionDetail from './commonVersionDetail'
    import commonFormDialog from '@repository/views/repoCommon/commonFormDialog'
    import { mapState, mapGetters, mapActions } from 'vuex'
    export default {
        name: 'commonPackageDetail',
        components: {
            OperationList,
            InfiniteScroll,
            VersionDetail,
            commonFormDialog,
            operationLimitConfirmDialog
        },
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
                            message: this.$t('pleaseSelect') + this.$t('space') + this.$t('tag'),
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
                    limitList: [10, 20, 40]
                }
            }
        },
        computed: {
            ...mapState(['scannerSupportPackageType', 'currentRepositoryDataPermission']),
            ...mapGetters(['isEnterprise']),
            projectId () {
                return this.$route.params.projectId || ''
            },
            repoType () {
                return this.$route.params.repoType || ''
            },
            repoName () {
                return this.$route.query.repoName || ''
            },
            packageKey () {
                return this.$route.query.packageKey || ''
            },
            version () {
                return this.$route.query.version || ''
            },
            currentVersion () {
                return this.versionList.find(version => version.name === this.version)
            },
            // 当前仓库类型，本地/远程/虚拟/组合
            storeType () {
                return this.$route.query.storeType || ''
            },
            // 是否是 软件源模式
            whetherSoftware () {
                return this.$route.path.startsWith('/software')
            },
            showRepoScan () {
                // 软件源模式下屏蔽安全扫描和禁用操作
                // 虚拟仓库屏蔽安全扫描和禁用操作
                return this.scannerSupportPackageType.join(',').toLowerCase().includes(this.repoType) && !(this.storeType === 'virtual') && !this.whetherSoftware
            },
            // 是否显示晋级操作
            showPromotion () {
                // 远程或虚拟仓库不显示晋级操作
                return this.updateOperationPermission && !(this.storeType === 'remote') && !(this.storeType === 'virtual') && !this.whetherSoftware
            },
            // 虚拟仓库的仓库来源，虚拟仓库时需要更换repoName为此值
            sourceRepoName () {
                return this.$route.query.sourceName || ''
            },
            currentRepoDataPermission () {
                return this.currentRepositoryDataPermission?.find((item) => item.resourceCode === 'bkrepo')?.actionCodes || []
            },
            // 制品晋级、添加元数据、删除元数据操作权限，实际上是编辑制品
            updateOperationPermission () {
                return this.currentRepoDataPermission.includes('update')
            },
            // 删除制品操作权限
            deleteOperationPermission () {
                return this.currentRepoDataPermission.includes('delete')
            },
            // 锁定制品操作权限
            lockOperationPermission () {
                return this.currentRepoDataPermission.includes('lock')
            },
            // 禁用制品操作权限
            forbidOperationPermission () {
                return this.currentRepoDataPermission.includes('forbid')
            }
        },
        created () {
            // 注意：软件源模式下不需要判断权限，软件源的vuex中也没有该接口定义，因为软件源模式下所有用户都是只读权限
            !this.whetherSoftware && this.getCurrentRepositoryDataPermission({ projectId: this.projectId, repoName: this.repoName })
            // 制品搜索且选择了指定的版本详情时需要默认触发版本的搜索
            this.versionInput = this.$route.query.searchFlag ? this.version : ''
            this.getPackageInfoHandler()
            this.handlerPaginationChange()
            this.refreshSupportPackageTypeList()
        },
        methods: {
            ...mapActions([
                'getPackageInfo',
                'getVersionList',
                'changeStageTag',
                'deleteVersion',
                'forbidPackageMetadata',
                'lockPackageMetadata',
                'refreshSupportPackageTypeList',
                'getCurrentRepositoryDataPermission'
            ]),
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}, load) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getVersionListHandler(load)
                if (!load) {
                    this.$refs.infiniteScroll && this.$refs.infiniteScroll.scrollToTop()
                    this.$router.replace({
                        query: {
                            ...this.$route.query,
                            versionName: this.versionInput
                        }
                    })
                }
            },
            getVersionListHandler (load) {
                if (this.isLoading) return
                this.isLoading = !load
                this.getVersionList({
                    projectId: this.projectId,
                    repoName: this.storeType === 'virtual' ? this.sourceRepoName : this.repoName,
                    packageKey: this.packageKey,
                    current: this.pagination.current,
                    limit: this.pagination.limit,
                    version: this.versionInput,
                    ...['go'].includes(this.repoType)
                        ? {
                            sortProperty: 'ordinal'
                        }
                        : {}
                }).then(({ records, totalRecords }) => {
                    load ? this.versionList.push(...records) : (this.versionList = records)
                    this.pagination.count = totalRecords
                    if (!this.versionInput) {
                        if (!this.versionList.length) {
                            // 当前包没有任何版本时需要返回到包列表页面，使用 route.back() 直接返回上一层可能存在问题
                            this.$router.push({
                                name: 'commonList',
                                params: {
                                    projectId: this.projectId,
                                    repoType: this.repoType
                                },
                                query: {
                                    repoName: this.repoName,
                                    storeType: this.storeType,
                                    type: this.repoType,
                                    category: this.storeType,
                                    c: this.pagination.current,
                                    l: this.pagination.limit,
                                    property: this.$route.query?.property,
                                    direction: this.$route.query?.direction
                                }
                            })
                        }
                        // records 这个数组可能因为当前包最后一个版本被删除导致数组为空
                        if (records?.length && (!this.version || !this.versionList.find(v => v.name === this.version))) {
                            this.$router.replace({
                                query: {
                                    ...this.$route.query,
                                    version: records?.[0]?.name
                                }
                            })
                        }
                    }
                }).finally(() => {
                    this.isLoading = false
                })
            },
            getPackageInfoHandler () {
                this.infoLoading = true
                this.getPackageInfo({
                    projectId: this.projectId,
                    repoName: this.storeType === 'virtual' ? this.sourceRepoName : this.repoName,
                    packageKey: this.packageKey
                }).then(info => {
                    this.pkg = info
                }).finally(() => {
                    this.infoLoading = false
                })
            },
            changeVersion ({ name: version }) {
                this.$router.replace({
                    query: {
                        ...this.$route.query,
                        version
                    }
                })
            },
            refresh (version) {
                this.getVersionListHandler()
                if (this.version === version) {
                    this.$refs.versionDetail && this.$refs.versionDetail.getDetail()
                }
            },
            changeStageTagHandler (row = this.currentVersion) {
                if ((row.stageTag || '').includes('@release')) return
                this.$refs.commonFormDialog.setData({
                    show: true,
                    loading: false,
                    title: this.$t('upgrade'),
                    type: 'upgrade',
                    version: row.name,
                    default: row.stageTag,
                    tag: ''
                })
            },
            scanPackageHandler (row = this.currentVersion) {
                this.$refs.commonFormDialog.setData({
                    show: true,
                    loading: false,
                    title: this.$t('scan') + this.$t('space') + this.$t('artifact'),
                    type: 'scan',
                    id: '',
                    name: this.pkg.name,
                    version: row.name
                })
            },
            // 打开二次确认弹窗
            showLimitDialog (limitType, row = this.currentVersion) {
                this.$refs.operationLimitConfirmDialog.setData({
                    show: true,
                    loading: false,
                    limitType: limitType,
                    theme: 'danger',
                    limitStatus: row.metadata[`${limitType}Status`],
                    limitReason: '',
                    name: row.name,
                    message: this.$t(row.metadata[`${limitType}Status`] ? 'confirmRemoveLimitOperationInfo' : 'confirmLimitOperationInfo', { limit: this.$t(limitType), type: this.$t('version') })
                })
            },
            // 改变制品的禁用或锁定状态
            changeLimitStatusHandler (row = this.currentVersion) {
                this[`${row.limitType}PackageMetadata`]({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    body: {
                        packageKey: this.packageKey,
                        version: row.name,
                        versionMetadata: [{ key: `${row.limitType}Status`, value: !row.limitStatus, description: row.limitReason }]
                    }
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: (row.limitStatus ? this.$t('relieve') + this.$t('space') + this.$t(row.limitType) : this.$t(row.limitType)) + this.$t('space') + this.$t('success')
                    })
                    this.$refs.operationLimitConfirmDialog.dialogData.show = false
                    this.refresh(row.name)
                }).finally(() => {
                    this.$refs.operationLimitConfirmDialog.dialogData.loading = false
                })
            },
            downloadPackageHandler (row = this.currentVersion) {
                if (this.repoType === 'docker') return
                const repoName = this.storeType === 'virtual' ? this.sourceRepoName : this.repoName
                const url = `/repository/api/version/download/${this.projectId}/${repoName}?packageKey=${this.packageKey}&version=${encodeURIComponent(row.name)}&download=true`
                this.$ajax.head(url).then(() => {
                    window.open(
                        '/web' + url,
                        '_self'
                    )
                }).catch(e => {
                    const message = e.status === 423 ? this.$t('fileDownloadError') : this.$t('fileError')
                    this.$bkMessage({
                        theme: 'error',
                        message
                    })
                })
            },
            deleteVersionHandler ({ name: version } = this.currentVersion) {
                this.$confirm({
                    theme: 'danger',
                    message: this.$t('deleteVersionTitle', { version }),
                    confirmFn: () => {
                        return this.deleteVersion({
                            projectId: this.projectId,
                            repoType: this.repoType,
                            repoName: this.repoName,
                            packageKey: this.packageKey,
                            version
                        }).then(() => {
                            this.getVersionListHandler()
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('delete') + this.$t('space') + this.$t('success')
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
    .common-package-header{
        height: 60px;
        background-color: white;
        &-refresh{
            margin-left: auto !important ;
        }
        .package-img {
            border-radius: 4px;
        }
        .common-package-title {
            .repo-title {
                max-width: 500px;
                font-size: 16px;
                font-weight: 500;
                color: #081E40;
            }
            // .repo-description {
            //     max-width: 70vw;
            //     padding: 5px 15px;
            //     background-color: var(--bgWeightColor);
            //     border-radius: 2px;
            // }
        }
    }
    .common-version-main {
        height: calc(100% - 70px);
        .common-version {
            width: 250px;
            height: 100%;
            margin-right: 10px;
            background-color: white;
            .version-header {
                height: 50px;
                font-size: 14px;
                color: var(--fontPrimaryColor);
                border-bottom: 1px solid var(--borderWeightColor);
            }
            .version-search {
                padding: 20px 20px 10px;
            }
            .version-list {
                height: calc(100% - 120px);
                padding: 0 20px 10px;
                background-color: white;
                .list-count {
                    font-size: 12px;
                    color: var(--fontSubsidiaryColor);
                }
                .version-item {
                    position: relative;
                    height: 42px;
                    border-radius: 2px;
                    background-color: var(--bgLightColor);
                    cursor: pointer;
                    .version-operation {
                        position: absolute;
                        right: 10px;
                    }
                    &:hover {
                        background-color: var(--bgHoverLighterColor);
                    }
                    &.selected {
                        color: white;
                        background-color: var(--primaryColor);
                        .version-operation {
                            ::v-deep .devops-icon.hover-btn {
                                color: white;
                                &:hover {
                                    background-color: transparent;
                                }
                            }
                            &:hover {
                                background-color: var(--primaryColor);
                            }
                        }
                    }
                }
            }
        }
        .common-version-detail {
            height: 100%;
            background-color: white;
        }
    }
}
</style>
