<template>
    <div class="mt10 saerch-package-container flex-align-center" v-bkloading="{ isLoading }">
        <div class="pt10 search-repo-list">
            <div class="repo-list">
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
        </div>
        <div class="saerch-package-list">
            <template v-if="packagetList.length">
                <main class="package-list">
                    <div class="hover-btn flex-column result-item"
                        @click="toRepoDetail(result)"
                        v-for="result in packagetList"
                        :key="result.repoName + result.key">
                        <div class="flex-align-center">
                            <icon size="14" :name="repoType" />
                            <span class="ml10 result-repo-name">{{result.name}}</span>
                            <span class="ml10 repo-tag" v-if="result.type === 'MAVEN'">
                                {{ result.key.replace(/^.*\/\/(.+):.*$/, '$1') }}
                            </span>
                            <span class="ml10">({{result.repoName}})</span>
                        </div>
                        <div class="result-card flex-align-center">
                            <div class="flex-align-center" :title="result.latest"><icon class="mr5" size="16" name="latest-version" />{{ result.latest }}</div>
                            <div class="flex-align-center"><icon class="mr5" size="16" name="versions" />{{ result.versions }}</div>
                            <div class="flex-align-center"><icon class="mr5" size="16" name="downloads" />{{ result.downloads }}</div>
                            <div class="flex-align-center"><icon class="mr5" size="16" name="time" />{{ formatDate(result.lastModifiedDate) }}</div>
                            <div class="flex-align-center"><icon class="mr5" size="16" name="updater" />{{ userList[result.lastModifiedBy] ? userList[result.lastModifiedBy].name : result.lastModifiedBy }}</div>
                        </div>
                    </div>
                </main>
                <bk-pagination
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
        </div>
    </div>
</template>
<script>
    import { mapState, mapActions } from 'vuex'
    import { formatDate } from '@/utils'
    import emptyData from '@/components/EmptyData'
    export default {
        name: 'searchPackage',
        components: { emptyData },
        data () {
            return {
                isLoading: false,
                repoName: '',
                repoList: [{ repoName: '', packages: 0 }],
                packagetList: [],
                pagination: {
                    current: 1,
                    limit: 20,
                    count: 0,
                    limitList: [10, 20, 40]
                }
            }
        },
        computed: {
            ...mapState(['userList']),
            repoType () {
                return this.$route.query.repoType
            },
            packageName () {
                return this.$route.query.packageName
            }
        },
        watch: {
            '$route.query': {
                handler: function () {
                    this.handlerPaginationChange()
                    this.repoName = ''
                    this.searchRepoHandler()
                },
                deep: true,
                immediate: true
            }
        },
        methods: {
            formatDate,
            ...mapActions([
                'searchPackageList',
                'searchRepoList'
            ]),
            searchRepoHandler () {
                this.isLoading = true
                this.searchRepoList({
                    repoType: this.repoType.toUpperCase(),
                    packageName: this.packageName || undefined
                }).then(({ list, sum }) => {
                    this.repoList = [{ repoName: '', packages: sum }, ...list]
                }).finally(() => {
                    this.isLoading = false
                })
            },
            changeRepoInput (repo) {
                this.repoName = repo.repoName
                this.handlerPaginationChange()
            },
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.searchPackageHandler()
            },
            searchPackageHandler () {
                this.isLoading = true
                this.searchPackageList({
                    projectId: PROJECT_ID,
                    repoType: this.repoType,
                    repoName: this.repoName,
                    packageName: this.packageName || '',
                    current: this.pagination.current,
                    limit: this.pagination.limit
                }).then(({ records, totalRecords }) => {
                    this.pagination.count = totalRecords
                    this.packagetList = records
                }).finally(() => {
                    this.isLoading = false
                })
            },
            toRepoDetail (pkg) {
                this.$router.push({
                    name: 'searchPackageDetail',
                    params: {
                        repoType: this.repoType,
                        repoName: pkg.repoName
                    },
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
.saerch-package-container {
    border-top: 1px solid $borderWeightColor;
    height: calc(100% - 73px);
    .search-repo-list {
        flex-basis: 210px;
        height: 100%;
        overflow: hidden;
        border-right: 1px solid $borderWeightColor;
        .repo-list {
            height: 100%;
            overflow-y: auto;
            .repo-item {
                padding: 0 10px;
                line-height: 32px;
                cursor: pointer;
                &.selected {
                    color: $primaryColor;
                    background-color: $primaryLightColor;
                }
            }
        }
    }
    .saerch-package-list {
        flex: 1;
        height: 100%;
        padding: 10px 10px 0;
        overflow: hidden;
        .package-list {
            height: calc(100% - 32px);
            overflow-y: auto;
            .result-item{
                justify-content: space-around;
                padding: 5px 20px;
                margin-bottom: 10px;
                height: 70px;
                border: 1px solid $borderWeightColor;
                border-radius: 5px;
                background-color: #fdfdfe;
                cursor: pointer;
                &:hover {
                    border-color: $iconPrimaryColor;
                }
                .result-repo-name {
                    color: #222222;
                    font-size: 12px;
                    font-weight: bold;
                }
                
                .repo-tag {
                    font-weight: normal;
                }
                .result-card {
                    color: $fontWeightColor;
                    font-size: 14px;
                    font-weight: normal;
                    div {
                        overflow: hidden;
                        text-overflow: ellipsis;
                        white-space: nowrap;
                        &:nth-child(1) {
                            flex-basis: 250px;
                        }
                        &:nth-child(2) {
                            flex-basis: 120px;
                        }
                        &:nth-child(3) {
                            flex-basis: 140px;
                        }
                        &:nth-child(4) {
                            flex-basis: 275px;
                        }
                        &:nth-child(5) {
                            flex-basis: 175px;
                        }
                    }
                }
            }
        }
    }
}
</style>
