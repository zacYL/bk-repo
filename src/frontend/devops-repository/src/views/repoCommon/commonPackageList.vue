<template>
    <div class="common-package-container" v-bkloading="{ isLoading }">
        <header class="mb10 pl20 pr20 common-package-header flex-align-center">
            <Icon class="package-img" size="30" :name="repoType" />
            <div class="ml10 common-package-title">
                <div class="mb5 repo-title text-overflow" :title="repoName">
                    {{ repoName }}
                </div>
            </div>
            <div class="flex-end-center flex-1">
                <bk-button @click="handlerPaginationChange">
                    {{ $t('refresh') }}
                </bk-button>
                <bk-button v-if="showUploadRepo" class="ml10" @click="handleClickUpload">{{$t('uploadArtifact')}}</bk-button>
                <bk-button v-if="showBatchUploadRepo" class="ml10" @click="handleClickBatchUpload">{{$t('batchUpload')}}</bk-button>
                <bk-button class="ml20 flex-align-center" @click="onClickShowGuide">
                    <span class="flex-align-center">
                        <Icon class="mr5" name="hand-guide" size="16" />
                        {{$t('guide')}}
                    </span>
                </bk-button>
                <bk-button
                    v-if="repoType === 'cocoapods' && storeType === 'remote'"
                    class="ml10"
                    @click="updateIndexHandler"
                >{{$t('updateIndex')}}</bk-button>
            </div>
        </header>
        <!-- 存在包, 加载中默认存在包 -->
        <template v-if="packageList.length || $route.query.packageName || isLoading">
            <div class="package-search-tools flex-between-center">
                <div class="flex-align-center">
                    <bk-input
                        class="w250 mr10"
                        v-model.trim="packageNameVal"
                        :placeholder="$t('artifactPlaceholder')"
                        clearable
                        @enter="handlerPaginationChange()"
                        @clear="handlerPaginationChange()"
                        right-icon="bk-icon icon-search">
                    </bk-input>
                    <bk-button v-if="currentType === 'MAVEN'" @click="exportList">
                        {{ $t('exportList') }}
                    </bk-button>
                </div>
                <div class="sort-tool flex-align-center">
                    <bk-select
                        style="width:200px;"
                        v-model="property"
                        :clearable="false"
                        @change="handlerPaginationChange()">
                        <bk-option id="name" :name="$t('nameSorting')"></bk-option>
                        <bk-option id="lastModifiedDate" :name="$t('lastModifiedTimeSorting')"></bk-option>
                        <bk-option id="createdDate" :name="$t('createTimeSorting')"></bk-option>
                        <bk-option id="downloads" :name="$t('downloadSorting')"></bk-option>
                    </bk-select>
                    <bk-popover :content="$t('toggle') + $t('space') + `${direction === 'ASC' ? $t('desc') : $t('asc')}`" placement="top">
                        <div class="ml10 sort-order flex-center" @click="changeDirection">
                            <Icon :name="`order-${direction.toLowerCase()}`" size="16"></Icon>
                        </div>
                    </bk-popover>
                </div>
            </div>
            <div class="common-package-list">
                <!-- 有数据 -->
                <template v-if="packageList.length">
                    <infinite-scroll
                        ref="infiniteScroll"
                        :is-loading="isLoading"
                        :has-next="hasNextData"
                        @load="handlerPaginationChange({ current: pagination.current + 1 }, true)">
                        <div class="mb10 list-count">{{$t('totalArtifactCount', { length: pagination.count })}} </div>
                        <package-card
                            class="mb10"
                            v-for="pkg in packageList"
                            :key="pkg.repoName + pkg.key"
                            :card-data="pkg"
                            :readonly="!deleteOperationPermission"
                            @click.native="showCommonPackageDetail(pkg)"
                            @delete-card="deletePackageHandler(pkg)">
                        </package-card>
                    </infinite-scroll>
                </template>
                <!-- 无数据 -->
                <template v-else>
                    <empty-data :is-loading="isLoading" ex-style="padding-top: 130px;" search></empty-data>
                </template>
            </div>
        </template>
        <!-- 不存在包 -->
        <template v-else>
            <empty-guide class="empty-guide" :store-type="storeType" :article="articleGuide"></empty-guide>
        </template>

        <bk-sideslider :is-show.sync="showGuide" :quick-close="true" :width="600">
            <template #header>
                <div class="flex-align-center"><icon class="mr5" :name="repoType" size="32"></icon>{{ replaceRepoName(repoName) + $t('space') + $t('guide') }}</div>
            </template>
            <template #content>
                <repo-guide class="pt20 pb20 pl10 pr10" :article="articleGuide"></repo-guide>
            </template>
        </bk-sideslider>

        <!-- 'MAVEN', 'DOCKER', 'NPM' 包制上传侧边栏 -->
        <repoUploader
            v-if="['MAVEN', 'DOCKER', 'NPM'].includes(currentType) && repoUploader.isVisible"
            v-model="repoUploader.isVisible"
            :project-id="projectId"
            :repo-name="repoName"
            :repo-type="currentType"
            @update="onUpdateUploader"
            @cancel="onCancelUploader" />
        
        <!-- 使用指引 -->
        <useGuide ref="useGuideRef"></useGuide>
    </div>
</template>
<script>
    import InfiniteScroll from '@repository/components/InfiniteScroll'
    import packageCard from '@repository/components/PackageCard'
    import repoGuide from '@repository/views/repoCommon/repoGuide'
    import emptyGuide from '@repository/views/repoCommon/emptyGuide'
    import repoUploader from '@repository/views/repoCommon/repoUploader'
    import useGuide from '@repository/views/repoCommon/useGuide'
    import repoGuideMixin from '@repository/views/repoCommon/repoGuideMixin'
    import { mapState, mapActions } from 'vuex'
    import { blobFileDownload } from '@repository/utils/index'
    export default {
        name: 'commonPackageList',
        components: { InfiniteScroll, packageCard, repoGuide, emptyGuide, repoUploader, useGuide },
        mixins: [repoGuideMixin],
        data () {
            return {
                isLoading: false,
                packageNameVal: this.$route.query.packageName,
                property: this.$route.query.property || 'lastModifiedDate',
                direction: this.$route.query.direction || 'DESC',
                packageList: [],
                pagination: {
                    current: 1,
                    limit: 20,
                    count: 0,
                    limitList: [10, 20, 40]
                },
                showGuide: false,
                repoUploader: {
                    isVisible: false
                }
            }
        },
        computed: {
            ...mapState(['repoListAll', 'permission', 'currentRepositoryDataPermission']),
            currentRepo () {
                return this.repoListAll.find(repo => repo.name === this.repoName) || {}
            },
            currentType () {
                return this.currentRepo.type || ''
            },
            // 是否是 软件源模式
            whetherSoftware () {
                return this.$route.path.startsWith('/software')
            },
            // 是否显示上传制品按钮
            showUploadRepo () {
                return ['MAVEN', 'DOCKER', 'NPM'].includes(this.currentType) && !(this.storeType === 'remote') && !(this.storeType === 'virtual') && !this.whetherSoftware && this.canUploadArtifact
            },
            showBatchUploadRepo () {
                return ['MAVEN', 'NPM'].includes(this.currentType) && !(this.storeType === 'remote') && !(this.storeType === 'virtual') && !this.whetherSoftware && this.canUploadArtifact
            },
            currentRepoDataPermission () {
                return this.currentRepositoryDataPermission?.find((item) => item.resourceCode === 'bkrepo')?.actionCodes || []
            },
            // 删除制品操作权限
            deleteOperationPermission () {
                return this.currentRepoDataPermission.includes('delete')
            },
            // 是否拥有上传制品权限
            canUploadArtifact () {
                return this.currentRepoDataPermission.includes('write')
            }
        },
        created () {
            this.getCurrentRepositoryDataPermission({ projectId: this.projectId, repoName: this.repoName })
            this.getRepoListAll({ projectId: this.projectId })
            this.handlerPaginationChange()
        },
        methods: {
            ...mapActions([
                'getRepoListAll',
                'searchPackageList',
                'packageListExport',
                'deletePackage',
                'getCurrentRepositoryDataPermission',
                'updateIndex'
            ]),
            updateIndexHandler () {
                this.updateIndex({
                    projectId: this.projectId,
                    repoName: this.repoName
                }).then((res) => {
                    if (res._error) {
                        return this.$bkMessage({
                            theme: 'error',
                            message: res?._error?.message || this.$t('update') + this.$t('space') + this.$t('fail')
                        })
                    }
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('update') + this.$t('space') + this.$t('success')
                    })
                }).catch((error) => {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || (this.$t('update') + this.$t('space') + this.$t('fail'))
                    })
                })
            },
            changeDirection () {
                this.direction = this.direction === 'ASC' ? 'DESC' : 'ASC'
                this.handlerPaginationChange()
            },
            // 点击上传制品按钮，显示侧边抽屉
            handleClickUpload () {
                this.repoUploader.isVisible = true
            },
            // 批量导出弹窗
            handleClickBatchUpload () {
                this.$globalUploadFiles({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    folder: false,
                    fullPath: '',
                    uploadType: 'mavenUpload'
                })
            },
            onUpdateUploader (flag) {
                this.onCancelUploader(flag)
                this.handlerPaginationChange()
            },
            // uploader上传制品侧边栏取消按钮点击事件
            onCancelUploader (flag) {
                this.repoUploader.isVisible = flag
            },
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}, load) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getPackageListHandler(load)
                if (!load) {
                    this.$refs.infiniteScroll && this.$refs.infiniteScroll.scrollToTop()
                    this.$router.replace({
                        query: {
                            ...this.$route.query,
                            packageName: this.packageNameVal,
                            property: this.property,
                            direction: this.direction
                        }
                    })
                }
            },
            exportList () {
                this.packageListExport({
                    projectId: this.projectId,
                    repoType: this.repoType,
                    repoName: this.repoName,
                    packageName: this.packageNameVal,
                    property: this.property,
                    direction: this.direction,
                    current: this.pagination.current,
                    limit: this.pagination.limit,
                    isExport: true
                }).then((res) => {
                    blobFileDownload(res)
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('exportSuccess')
                    })
                }).catch((error) => {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || this.$t('exportFailed')
                    })
                })
            },
            getPackageListHandler (load) {
                if (this.isLoading) return
                this.isLoading = !load
                return this.searchPackageList({
                    projectId: this.projectId,
                    repoType: this.repoType,
                    repoName: this.repoName,
                    packageName: this.packageNameVal,
                    property: this.property,
                    direction: this.direction,
                    current: this.pagination.current,
                    limit: this.pagination.limit
                }).then(({ records, totalRecords }) => {
                    load ? this.packageList.push(...records) : (this.packageList = records)
                    this.pagination.count = totalRecords
                    // 后端接口返回的数组数量是否大于等于当前页码，如果大于等于，表示可能还有下一页，需要支持加载下一页
                    this.hasNextData = records?.length >= this.pagination.limit
                }).catch((e) => {
                    this.$bkMessage({
                        message: e.message,
                        theme: 'error'
                    })
                    if ((e.status === 404 && e?.error?.code === 251006) || ((e.status === 403 && e?.error?.code === 250111))) {
                        // e.status === 404 && e?.error?.code === 251006 此时表明当前仓库不存在了
                        // e.status === 403 && e?.error?.code === 250111 此时表明当前用户没有当前仓库的查看权限了
                        this.$router.push({ name: 'repoList' })
                    }
                }).finally(() => {
                    this.isLoading = false
                })
            },
            deletePackageHandler ({ key }) {
                this.$confirm({
                    theme: 'danger',
                    message: this.$t('deletePackageTitle', { name: '' }),
                    subMessage: key,
                    confirmFn: () => {
                        return this.deletePackage({
                            projectId: this.projectId,
                            repoType: this.repoType,
                            repoName: this.repoName,
                            packageKey: key
                        }).then(() => {
                            this.handlerPaginationChange()
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('delete') + this.$t('space') + this.$t('success')
                            })
                        })
                    }
                })
            },
            showCommonPackageDetail (pkg) {
                this.$router.push({
                    name: 'commonPackage',
                    query: {
                        // 需要保留之前制品列表页的筛选项和页码相关参数
                        ...this.$route.query,
                        repoName: this.repoName,
                        packageKey: pkg.key,
                        // 此时需要将version清除掉，否则在进入仓库详情页后再返回包列表页，然后选择其他的包进入版本详情页，
                        // 会导致出现无效请求，且packageKey为最新版本的，但是版本号是之前版本的，进而导致请求出错
                        version: undefined,
                        storeType: this.storeType,
                        // 虚拟仓库中需要添加仓库来源，供制品详情页获取制品版本列表数据使用
                        sourceName: this.storeType === 'virtual' ? pkg.repoName || '' : undefined
                    }
                })
            },
            onClickShowGuide () {
                this.$refs.useGuideRef.setData({
                    show: true,
                    loading: false
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.common-package-container {
    height: 100%;
    .common-package-header{
        height: 60px;
        background-color: white;
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
    .package-search-tools {
        padding: 10px 20px;
        background-color: white;
        .sort-tool {
            color: var(--fontSubsidiaryColor);
            .sort-order {
                width: 30px;
                height: 30px;
                border: 1px solid var(--borderWeightColor);
                border-radius: 2px;
                cursor: pointer;
                &:hover {
                    color: var(--primaryColor);
                    border-color: currentColor;
                    background-color: var(--bgHoverLighterColor);
                }
            }
        }
    }
    .common-package-list {
        height: calc(100% - 120px);
        padding: 0 20px;
        background-color: white;
        .list-count {
            font-size: 12px;
            color: var(--fontSubsidiaryColor);
        }
    }
    .empty-guide {
        height: calc(100% - 70px);
        background-color: white;
        overflow-y: auto;
    }
}
</style>
