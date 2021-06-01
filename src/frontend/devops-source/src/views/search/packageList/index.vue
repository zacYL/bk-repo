<template>
    <div class="common-package-list-container" v-bkloading="{ isLoading }">
        <div class="repo-info flex-align-center">
            <Icon :name="repoType" size="60" />
            <div class="ml20 repo-info-main flex-column">
                <span class="repo-name">{{ currentRepo.name }}</span>
                <span class="mt5 repo-description">{{ currentRepo.description || '--' }}</span>
                <span class="mt5 repo-create">{{ userList[currentRepo.createdBy] ? userList[currentRepo.createdBy].name : currentRepo.createdBy }} 创建于 {{ formatDate(currentRepo.createdDate) }}</span>
            </div>
            <div class="ml10 hover-btn flex-align-center" @click="showGuide = true">
                <Icon class="mr5" name="hand-guide" size="16" />
                {{$t('guide')}}
            </div>
        </div>
        <div class="mt10 common-package-search flex-align-center">
            <bk-input
                class="w220"
                v-model.trim="packageNameInput"
                clearable
                @enter="handlerPaginationChange()"
                @clear="handlerPaginationChange()"
                :placeholder="$t('pleaseInput') + $t('packageName')">
            </bk-input>
            <bk-button class="ml10 pl5 pr5" theme="primary" @click="handlerPaginationChange()" icon="search">
                {{ $t('search') }}
            </bk-button>
            <div class="flex-1 flex-end-center">
                <bk-button class="pl5 pr5" theme="default" @click="$router.push({ name: 'searchRepoList' })">
                    {{ $t('returnBack') }}
                </bk-button>
            </div>
        </div>
        <template v-if="packageList.length">
            <main class="mt10 common-package-main">
                <package-card
                    class="package-card"
                    v-for="pkg in packageList"
                    :key="pkg.key"
                    :card-data="pkg"
                    @click.native="showCommonPackageDetail(pkg)"
                    @delete-card="deletePackageHandler(pkg)">
                </package-card>
            </main>
            <bk-pagination
                class="mt10"
                size="small"
                align="right"
                @change="current => handlerPaginationChange({ current })"
                @limit-change="limit => handlerPaginationChange({ limit })"
                :current.sync="pagination.current"
                :limit="pagination.limit"
                :count="pagination.count"
                :limit-list="pagination.limitList">
            </bk-pagination>
        </template>
        <empty-data v-else></empty-data>
        <bk-sideslider :is-show.sync="showGuide" :quick-close="true" :width="850">
            <div slot="header" class="flex-align-center"><Icon class="mr5" :name="repoType" size="32" />{{ repoName + $t('guide') }}</div>
            <repo-guide class="pt20 pb20 pl10 pr10" slot="content" :article="articleGuide"></repo-guide>
        </bk-sideslider>
    </div>
</template>
<script>
    import { formatDate } from '@/utils'
    import emptyData from '@/components/EmptyData'
    import packageCard from './packageCard'
    import repoGuide from './repoGuide'
    import repoGuideMixin from './repoGuide/repoGuideMixin'
    import { mapState, mapActions } from 'vuex'
    export default {
        name: 'commonPackageList',
        components: { packageCard, emptyData, repoGuide },
        mixins: [repoGuideMixin],
        data () {
            return {
                isLoading: false,
                showGuide: false,
                packageNameInput: '',
                pagination: {
                    current: 1,
                    limit: 20,
                    count: 0,
                    limitList: [10, 20, 40]
                },
                packageList: []
            }
        },
        computed: {
            ...mapState(['userList', 'repoList']),
            currentRepo () {
                return this.repoList.find(v => v.name === this.repoName) || {}
            }
        },
        created () {
            this.handlerPaginationChange()
        },
        methods: {
            formatDate,
            ...mapActions([
                'getPackageList',
                'deletePackage'
            ]),
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getPackageListHandler()
            },
            getPackageListHandler () {
                this.isLoading = true
                return this.getPackageList({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    current: this.pagination.current,
                    limit: this.pagination.limit,
                    packageName: this.packageNameInput
                }).then(({ records, totalRecords }) => {
                    this.packageList = records
                    this.pagination.count = totalRecords
                }).finally(() => {
                    this.isLoading = false
                })
            },
            deletePackageHandler (pkg) {
                this.$bkInfo({
                    type: 'error',
                    title: this.$t('deletePackageTitle'),
                    subTitle: this.$t('deletePackageSubTitle'),
                    showFooter: true,
                    confirmFn: () => {
                        this.deletePackage({
                            projectId: this.projectId,
                            repoType: this.repoType,
                            repoName: this.repoName,
                            packageKey: pkg.key
                        }).then(() => {
                            this.handlerPaginationChange()
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('delete') + this.$t('success')
                            })
                        })
                    }
                })
            },
            showCommonPackageDetail (pkg) {
                this.$router.push({
                    name: 'searchPackageDetail',
                    query: {
                        package: pkg.key
                    }
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
@import '@/scss/conf';
.common-package-list-container {
    padding: 10px 20px;
    .repo-info {
        height: 80px;
        border-bottom: 1px solid $borderWeightColor;
        .repo-info-main {
            flex: 1;
            overflow: hidden;
            .repo-name {
                color: $fontBoldColor;
                font-size: 20px;
                font-weight: 500;
            }
            .repo-description, .repo-create {
                font-size: 12px;
                color: $fontColor;
            }
        }
    }
    .common-package-main {
        max-height: calc(100% - 174px);
        overflow-y: auto;
        flex: 1;
        .package-card {
            margin-top: 10px;
            &:first-child {
                margin-top: 0;
            }
        }
    }
}
</style>
