<template>
    <div class="repo-search-container">
        <div class="repo-search-tools flex-column">
            <div class="name-tool flex-center">
                <type-select v-model="repoType" @change="handlerPaginationChange()"></type-select>
                <bk-input
                    v-focus
                    style="width:390px"
                    v-model.trim="packageNameInput"
                    size="large"
                    :placeholder="$t('pleaseInput') + $t('packageName')"
                    @enter="handlerPaginationChange()">
                </bk-input>
                <i class="name-search devops-icon icon-search flex-center" @click="handlerPaginationChange()"></i>
            </div>
            <div class="mt20 flex-between-center">
                <div class="result-count">为您搜索到到相关结果{{ pagination.count }}个</div>
                <div class="sort-tool flex-align-center">
                    <bk-select
                        style="width:150px;"
                        v-model="property"
                        :clearable="false"
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
        </div>
        <main class="repo-search-result flex-align-center" v-bkloading="{ isLoading }">
            <template v-if="resultList.length">
                <div v-if="repoType" class="mr20 repo-list">
                    <div class="repo-item flex-between-center"
                        :class="{ 'selected': repo.repoName === repoName }"
                        v-for="(repo, index) in repoList"
                        :key="repo.repoName || index"
                        :title="repo.repoName"
                        @click="changeRepoInput(repo)">
                        <span class="flex-1 text-overflow">{{ repo.repoName || '全部' }}</span>
                        <span class="ml5">{{ repo.packages }}</span>
                    </div>
                </div>
                <infinite-scroll
                    ref="infiniteScroll"
                    class="package-list flex-1"
                    :is-loading="isLoading"
                    :has-next="resultList.length < pagination.count"
                    @load="handlerPaginationChange({ current: pagination.current + 1 }, true)">
                    <package-card
                        class="mb20"
                        v-for="pkg in resultList"
                        :key="pkg.repoName + pkg.key"
                        :card-data="pkg"
                        readonly
                        @click.native="showCommonPackageDetail(pkg)">
                    </package-card>
                </infinite-scroll>
            </template>
            <empty-data v-else class="flex-1" search></empty-data>
        </main>
    </div>
</template>
<script>
    import packageCard from '@/components/PackageCard'
    import InfiniteScroll from '@/components/InfiniteScroll'
    import typeSelect from './typeSelect'
    import { mapState, mapActions } from 'vuex'
    import { formatDate } from '@/utils'
    export default {
        name: 'repoSearch',
        components: { packageCard, InfiniteScroll, typeSelect },
        directives: {
            focus: {
                inserted (el) {
                    el.querySelector('input').focus()
                }
            }
        },
        data () {
            return {
                isLoading: false,
                property: this.$route.query.property || 'lastModifiedDate',
                direction: this.$route.query.direction || 'ASC',
                packageNameInput: this.$route.query.packageName || '',
                repoType: this.$route.query.repoType || '',
                repoList: [{ repoName: '', packages: 0 }],
                repoName: this.$route.query.repoName || '',
                pagination: {
                    current: 1,
                    limit: 20,
                    count: 0
                },
                resultList: []
            }
        },
        computed: {
            ...mapState(['userList']),
            projectId () {
                return this.$route.params.projectId
            }
        },
        created () {
            this.handlerPaginationChange()
        },
        methods: {
            formatDate,
            ...mapActions(['searchPackageList', 'searchRepoList']),
            searchRepoHandler () {
                this.searchRepoList({
                    projectId: this.projectId,
                    repoType: this.repoType.toUpperCase(),
                    packageName: this.packageName || ''
                }).then(({ list, sum }) => {
                    this.repoList = [{ repoName: '', packages: sum }, ...list]
                })
            },
            searckPackageHandler (load) {
                if (this.isLoading) return
                this.isLoading = !load
                this.searchPackageList({
                    projectId: this.projectId,
                    repoType: this.repoType,
                    repoName: this.repoName,
                    packageName: this.packageNameInput,
                    property: this.property,
                    direction: this.direction,
                    current: this.pagination.current,
                    limit: this.pagination.limit
                }).then(({ records, totalRecords }) => {
                    this.pagination.count = totalRecords
                    load ? this.resultList.push(...records) : (this.resultList = records)
                }).finally(() => {
                    this.isLoading = false
                })
            },
            showCommonPackageDetail (pkg) {
                this.$router.push({
                    name: 'commonPackage',
                    params: {
                        projectId: this.projectId,
                        repoType: pkg.type.toLowerCase()
                    },
                    query: {
                        repoName: pkg.repoName,
                        package: pkg.key
                    }
                })
            },
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}, load) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.searckPackageHandler(load)
                if (!load) {
                    this.$refs.infiniteScroll && this.$refs.infiniteScroll.scrollToTop()
                    this.repoType && this.searchRepoHandler()
                    this.$router.replace({
                        query: {
                            repoType: this.repoType,
                            repoName: this.repoName,
                            packageName: this.packageNameInput,
                            property: this.property,
                            direction: this.direction
                        }
                    })
                }
            },
            changeDirection () {
                this.direction = this.direction === 'ASC' ? 'DESC' : 'ASC'
                this.handlerPaginationChange()
            },
            changeRepoInput (repo) {
                this.repoName = repo.repoName
                this.handlerPaginationChange()
            }
        }
    }
</script>
<style lang="scss" scoped>
.repo-search-container {
    position: relative;
    height: 100%;
    padding: 20px 20px 0;
    background-color: white;
    .repo-search-tools {
        padding-bottom: 10px;
        z-index: 1;
        background-color: white;
        .name-tool {
            height: 48px;
            ::v-deep .bk-input-large {
                border-radius: 0;
                height: 48px;
                line-height: 48px;
            }
            .name-search {
                width: 81px;
                height: 100%;
                margin-left: -1px;
                color: white;
                font-size: 16px;
                font-weight: bold;
                background-color: var(--primaryColor);
                border-radius: 0 2px 2px 0;
                cursor: pointer;
            }
        }
        .result-count {
            color: var(--fontSubsidiaryColor);
        }
        .sort-tool {
            color: var(--boxShadowColor);
            .sort-order {
                width: 32px;
                height: 32px;
                border: 1px solid currentColor;
                border-radius: 2px;
            }
        }
    }
    .repo-search-result {
        height: calc(100% - 100px);
        .repo-list {
            width: 200px;
            height: 100%;
            overflow-y: auto;
            .repo-item {
                padding: 0 10px;
                line-height: 32px;
                cursor: pointer;
                &.selected {
                    color: var(--primaryColor);
                    background-color: var(--bgColor);
                }
            }
        }
        .package-list {
            height: 100%;
        }
    }
}
</style>
