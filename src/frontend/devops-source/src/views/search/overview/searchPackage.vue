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
            <template v-if="packageList.length">
                <main class="package-list">
                    <package-card
                        class="package-card"
                        v-for="pkg in packageList"
                        :key="pkg.key"
                        :card-data="pkg"
                        :show-repo="true"
                        @refresh="handlerPaginationChange()">
                    </package-card>
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
    import packageCard from '../packageList/packageCard'
    import emptyData from '@/components/EmptyData'
    export default {
        name: 'searchPackage',
        components: { packageCard, emptyData },
        data () {
            return {
                isLoading: false,
                repoName: '',
                repoList: [{ repoName: '', packages: 0 }],
                packageList: [],
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
                    this.packageList = records
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
            .package-card {
                margin-top: 10px;
                &:first-child {
                    margin-top: 0;
                }
            }
        }
    }
}
</style>
