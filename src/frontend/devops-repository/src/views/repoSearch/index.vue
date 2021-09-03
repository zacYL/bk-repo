<template>
    <div class="repo-search-container">
        <div class="repo-search-tools flex-column">
            <div class="name-tool flex-align-center">
                <bk-input
                    v-focus
                    class="name-input"
                    v-model.trim="packageNameInput"
                    size="large"
                    :placeholder="$t('pleaseInput') + $t('packageName')"
                    @enter="handlerPaginationChange()">
                </bk-input>
                <i class="name-search devops-icon icon-search flex-center" @click="handlerPaginationChange()"></i>
            </div>
            <div class="mt10 flex-between-center">
                <bk-radio-group class="type-tool" v-model="repoType" @change="handlerPaginationChange()">
                    <bk-radio-button :value="''">全部</bk-radio-button>
                    <bk-radio-button v-for="repo in repoEnum" :key="repo" :value="repo">
                        <Icon size="14" :name="repo" />
                        <span class="ml5">{{repo}}</span>
                    </bk-radio-button>
                </bk-radio-group>
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
                <div v-if="repoType" class="mr20 mt40 repo-list">
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
                    class="package-list"
                    :is-loading="isLoading"
                    :has-next="resultList.length < pagination.count"
                    @load="handlerPaginationChange({ current: pagination.current + 1 }, true)">
                    <div class="mb10 result-count">为您搜索到到相关结果{{ pagination.count }}个</div>
                    <package-card
                        class="mb20"
                        v-for="pkg in resultList"
                        :key="pkg.key"
                        :card-data="pkg"
                        readonly
                        @click.native="showCommonPackageDetail(pkg)">
                    </package-card>
                </infinite-scroll>
            </template>
            <empty-data v-else class="flex-1" ex-style="margin-top: -250px;" search></empty-data>
        </main>
    </div>
</template>
<script>
    import packageCard from '@/components/PackageCard'
    import InfiniteScroll from '@/components/InfiniteScroll'
    import { mapState, mapActions } from 'vuex'
    import { repoEnum } from '@/store/publicEnum'
    import { formatDate } from '@/utils'
    export default {
        name: 'repoSearch',
        components: { packageCard, InfiniteScroll },
        directives: {
            focus: {
                inserted (el) {
                    el.querySelector('input').focus()
                }
            }
        },
        data () {
            return {
                repoEnum: repoEnum.filter(v => v !== 'generic'),
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
                        name: pkg.repoName,
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
            height: 38px;
            .name-input {
                max-width: 480px;
            }
            .name-search {
                width: 80px;
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
        .type-tool {
            display: flex;
            align-items: center;
            ::v-deep .bk-form-radio-button {
                margin-right: 20px;
                .bk-radio-button-text {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    height: 24px;
                    line-height: initial;
                    border-radius: 24px;
                }
            }
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
                    background-color: var(--primaryLightColor);
                }
            }
        }
        .package-list {
            flex: 1;
            height: 100%;
            .result-count {
                font-size: 12px;
                color: #999;
            }
        }
    }
}
</style>
