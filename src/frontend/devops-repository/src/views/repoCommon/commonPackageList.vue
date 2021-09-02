<template>
    <div class="common-package-container" v-bkloading="{ isLoading }">
        <header class="mb10 p20 common-package-header flex-align-center">
            <Icon class="package-img" size="80" :name="repoType" />
            <div class="ml20 common-package-title flex-column flex-1">
                <span class="mb10 repo-title text-overflow" :title="repoName">
                    {{ repoName }}
                </span>
                <span class="repo-description" :title="currentRepo.description">
                    {{ currentRepo.description || '--' }}
                </span>
            </div>
            <div class="ml10 repo-guide-btn flex-align-center" @click="showGuide = true">
                <Icon class="mr5" name="hand-guide" size="16" />
                {{$t('guide')}}
            </div>
        </header>
        <!-- 搜索中/有数据 -->
        <div v-if="$route.query.packageName || packageList.length" class="package-search-tools flex-between-center">
            <bk-input
                class="w250"
                v-model.trim="packageNameInput"
                size="small"
                placeholder="请输入制品名称, 按Enter键搜索"
                @enter="handlerPaginationChange()">
            </bk-input>
            <div class="sort-tool flex-align-center">
                <bk-select
                    style="width:150px;"
                    v-model="property"
                    :clearable="false"
                    size="small"
                    @change="handlerPaginationChange()">
                    <bk-option id="name" name="名称排序"></bk-option>
                    <bk-option id="lastModifiedDate" name="时间排序"></bk-option>
                    <bk-option id="downloads" name="下载量排序"></bk-option>
                </bk-select>
                <div class="ml10 sort-order flex-center hover-btn" @click="changeDirection">
                    <Icon :name="`order-${direction.toLowerCase()}`" size="16"></Icon>
                </div>
            </div>
        </div>
        <!-- 有数据 -->
        <infinite-scroll
            v-if="packageList.length"
            ref="infiniteScroll"
            class="common-package-list"
            :is-loading="isLoading"
            :has-next="packageList.length < pagination.count"
            @load="handlerPaginationChange({ current: pagination.current + 1 }, true)">
            <div class="mb10 list-count">共计{{ pagination.count }}个制品</div>
            <package-card
                class="mb20"
                v-for="pkg in packageList"
                :key="pkg.key"
                :card-data="pkg"
                @click.native="showCommonPackageDetail(pkg)"
                @delete-card="deletePackageHandler(pkg)">
            </package-card>
        </infinite-scroll>
        <!-- 未搜索无数据 -->
        <empty-guide v-if="!isLoading && !$route.query.packageName && !packageList.length" class="empty-guide" :article="articleGuide"></empty-guide>
        <!-- 搜索无数据 -->
        <empty-data v-if="$route.query.packageName && !packageList.length" ex-style="margin-top: 100px;" search></empty-data>

        <bk-sideslider :is-show.sync="showGuide" :quick-close="true" :width="850">
            <div slot="header" class="flex-align-center"><icon class="mr5" :name="repoType" size="32"></icon>{{ replaceRepoName(repoName) + $t('guide') }}</div>
            <repo-guide class="pt20 pb20 pl10 pr10" slot="content" :article="articleGuide"></repo-guide>
        </bk-sideslider>
    </div>
</template>
<script>
    import InfiniteScroll from '@/components/InfiniteScroll'
    import packageCard from '@/components/PackageCard'
    import repoGuide from './repoGuide'
    import emptyGuide from './emptyGuide'
    import repoGuideMixin from './repoGuideMixin'
    import { mapState, mapActions } from 'vuex'
    export default {
        name: 'commonPackageList',
        components: { InfiniteScroll, packageCard, repoGuide, emptyGuide },
        mixins: [repoGuideMixin],
        data () {
            return {
                isLoading: false,
                packageNameInput: '',
                property: this.$route.query.property || 'lastModifiedDate',
                direction: this.$route.query.direction || 'ASC',
                packageList: [],
                pagination: {
                    current: 1,
                    limit: 20,
                    count: 0,
                    limitList: [10, 20, 40]
                },
                showGuide: false
            }
        },
        computed: {
            ...mapState(['repoListAll']),
            currentRepo () {
                return this.repoListAll.find(repo => repo.name === this.repoName) || {}
            }
        },
        created () {
            this.getRepoListAll({ projectId: this.projectId })
            this.handlerPaginationChange()
        },
        methods: {
            ...mapActions([
                'getRepoListAll',
                'searchPackageList',
                'deletePackage'
            ]),
            changeDirection () {
                this.direction = this.direction === 'ASC' ? 'DESC' : 'ASC'
                this.handlerPaginationChange()
            },
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getPackageListHandler()
                this.$router.replace({
                    query: {
                        ...this.$route.query,
                        packageName: this.packageNameInput,
                        property: this.property,
                        direction: this.direction
                    }
                })
            },
            getPackageListHandler () {
                this.isLoading = true
                return this.searchPackageList({
                    projectId: this.projectId,
                    repoType: this.repoType,
                    repoName: this.repoName,
                    packageName: this.packageNameInput,
                    property: this.property,
                    direction: this.direction,
                    current: this.pagination.current,
                    limit: this.pagination.limit
                }).then(({ records, totalRecords }) => {
                    this.packageList = records
                    this.pagination.count = totalRecords
                }).finally(() => {
                    this.isLoading = false
                })
            },
            deletePackageHandler ({ key }) {
                this.$confirm({
                    theme: 'danger',
                    message: this.$t('deletePackageTitle', { name: key }),
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
                                message: this.$t('delete') + this.$t('success')
                            })
                        })
                    }
                })
            },
            showCommonPackageDetail (pkg) {
                this.$router.push({
                    name: 'commonPackage',
                    query: {
                        name: this.repoName,
                        package: pkg.key
                    }
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
@import '@/scss/conf';
.common-package-container {
    height: 100%;
    .common-package-header{
        height: 130px;
        background-color: white;
        .package-img {
            width: 110px;
            height: 90px;
            border-radius: 4px;
            box-shadow: 0px 3px 5px 0px rgba(217, 217, 217, 0.5);
        }
        .common-package-title {
            overflow: hidden;
            .repo-title {
                max-width: 500px;
                font-size: 20px;
                font-weight: bold;
                color: $fontBoldColor;
            }
            .repo-description {
                max-width: 600px;
                font-size: 12px;
                display: -webkit-box;
                -webkit-line-clamp: 3;
                -webkit-box-orient: vertical;
                overflow: hidden;
            }
        }
        .repo-guide-btn {
            padding: 6px 12px;
            border-radius: 4px;
            cursor: pointer;
            &:hover {
                color: white;
                background-color: $primaryColor;
            }
        }
    }
    .package-search-tools {
        padding: 10px 20px;
        background-color: white;
        .sort-tool {
            color: $boxShadowColor;
            .sort-order {
                width: 24px;
                height: 24px;
                border: 1px solid currentColor;
                border-radius: 2px;
            }
        }
    }
    .common-package-list {
        height: calc(100% - 186px);
        padding: 0 20px 10px;
        background-color: white;
        overflow-y: auto;
        .list-count {
            font-size: 12px;
            color: #999;
        }
    }
    .empty-guide {
        height: calc(100% - 130px);
        background-color: white;
        overflow-y: auto;
    }
}
</style>
