<template>
    <div class="repo-search-container" v-bkloading="{ isLoading }">
        <div class="repo-search-tools flex-column">
            <div class="name-tool flex-center">
                <type-select
                    :repo-list="repoEnum.filter(r => r !== 'generic')"
                    :repo-type="repoType"
                    :artifact-original-list="artifactOriginalList"
                    @change="changeRepoType"
                    @search-artifact="onSearchArtifact">
                </type-select>
            </div>
            <div v-if="resultList.length" class="mt20 flex-end-center">
                <div class="sort-tool flex-align-center">
                    <bk-select
                        style="width:150px;"
                        v-model="property"
                        :clearable="false"
                        @change="changeSortType">
                        <bk-option id="name" name="名称排序"></bk-option>
                        <bk-option id="lastModifiedDate" name="时间排序"></bk-option>
                        <bk-option id="downloads" name="下载量排序"></bk-option>
                    </bk-select>
                    <bk-popover :content="`切换为${direction === 'ASC' ? '降序' : '升序'}`" placement="top">
                        <div class="ml10 sort-order flex-center" @click="changeDirection">
                            <Icon :name="`order-${direction.toLowerCase()}`" size="16"></Icon>
                        </div>
                    </bk-popover>
                </div>
            </div>
        </div>
        <main class="repo-search-result flex-align-center">
            <template v-if="resultList.length">
                <infinite-scroll
                    ref="infiniteScroll"
                    class="package-list flex-1"
                    :is-loading="isLoading"
                    :has-next="resultList.length < pagination.count"
                    @load="handlerPaginationChange({ current: pagination.current + 1 }, true)">
                    <package-card
                        class="mb10"
                        v-for="pkg in resultList"
                        :key="pkg.repoName + (pkg.key || pkg.fullPath)"
                        :card-data="pkg"
                        readonly
                        :show-search-version-list="((pkg.matchedVersions || []).length > 0 && repoType !== 'generic') ? true : false"
                        @show-detail="showDetail"
                        @share="handlerShare"
                        @jump-to-specific-version="(version) => {
                            showCommonPackageDetail(pkg,version)
                        }"
                        @click.native="showCommonPackageDetail(pkg)">
                    </package-card>
                </infinite-scroll>
            </template>
            <empty-data v-else :is-loading="isLoading" class="flex-1" ex-style="align-self:start;margin-top:130px;"></empty-data>
        </main>
        <generic-detail ref="genericDetail"></generic-detail>
        <generic-share-dialog ref="genericShareDialog"></generic-share-dialog>
    </div>
</template>
<script>
    import packageCard from '@repository/components/PackageCard'
    import InfiniteScroll from '@repository/components/InfiniteScroll'
    import genericDetail from '@repository/views/repoGeneric/genericDetail'
    import genericShareDialog from '@repository/views/repoGeneric/genericShareDialog'
    import typeSelect from '@repository/views/repoSearch/typeSelect'
    import { mapState, mapActions } from 'vuex'
    import { formatDate } from '@repository/utils'
    import { repoEnum } from '@repository/store/publicEnum'
    export default {
        name: 'repoSearch',
        components: { packageCard, InfiniteScroll, typeSelect, genericDetail, genericShareDialog },
        directives: {
            focus: {
                inserted (el) {
                    el.querySelector('input').focus()
                }
            }
        },
        data () {
            return {
                repoEnum,
                isLoading: false,
                property: this.$route.query.property || 'lastModifiedDate',
                direction: this.$route.query.direction || 'DESC',
                projectId: this.$route.query.projectId || '',
                packageName: this.$route.query.packageName || '',
                repoType: this.$route.query.repoType || 'docker',
                repoName: this.$route.query.repoName || '',
                pagination: {
                    current: 1,
                    limit: 20,
                    count: 0
                },
                resultList: [],
                searchArtifactParams: {},
                // 根据制品类型获取到的仓库列表集合
                artifactOriginalList: []
            }
        },
        computed: {
            ...mapState(['projectList', 'userList']),
            isSearching () {
                const { packageName, repoType, repoName } = this.$route.query
                return Boolean(packageName || repoType || repoName)
            }
        },
        created () {
            // 更新程度：类型 》 包名 》 树/排序 》 滚动加载
            this.changeRepoType(this.repoType)
        },
        methods: {
            formatDate,
            ...mapActions(['searchPackageList', 'getRepoListAll']),
            searckPackageHandler (scrollLoad) {
                if (this.isLoading) return
                this.isLoading = !scrollLoad
                this.searchPackageList({
                    projectId: this.projectId,
                    repoType: this.repoType,
                    repoName: this.repoName,
                    packageName: this.searchArtifactParams.name,
                    property: this.property,
                    direction: this.direction,
                    current: this.pagination.current,
                    limit: this.pagination.limit,
                    version: this.searchArtifactParams.version,
                    md5: this.searchArtifactParams.md5,
                    sha256: this.searchArtifactParams.sha256,
                    metadataList: this.searchArtifactParams.metadataList,
                    artifactList: this.searchArtifactParams.artifactList
                }).then(({ records, totalRecords }) => {
                    this.pagination.count = totalRecords
                    scrollLoad ? this.resultList.push(...records) : (this.resultList = records)
                }).finally(() => {
                    this.isLoading = false
                })
            },
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}, scrollLoad = false) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.searckPackageHandler(scrollLoad)
                !scrollLoad && this.$refs.infiniteScroll && this.$refs.infiniteScroll.scrollToTop()
            },
            // 搜索制品
            onSearchArtifact (params) {
                this.searchArtifactParams = params
                this.handlerPaginationChange()
            },
            changeSortType () {
                this.handlerPaginationChange()
            },
            changeDirection () {
                this.direction = this.direction === 'ASC' ? 'DESC' : 'ASC'
                this.handlerPaginationChange()
            },
            changeRepoType (repoType) {
                // 制品搜索页，当切换制品类型时将搜索的包名参数重置为空，否则不重置，解决复制url导致搜索参数丢失的问题
                if (this.repoType !== repoType) {
                    this.packageName = ''
                    this.repoType = repoType
                }
                // 改变制品类型之后需要把搜索相关的参数重置为空，否则会保留之前的搜索参数，导致结果有误
                this.searchArtifactParams = {}
                this.getRepoSearchArtifactList()
                this.handlerPaginationChange()
            },
            // 获取仓库下拉选择框原始数据，不分页
            getRepoSearchArtifactList () {
                this.getRepoListAll({ projectId: this.projectId, type: this.repoType, searchFlag: true }).then((res) => {
                    this.artifactOriginalList = res
                })
            },
            showCommonPackageDetail (pkg, version) {
                if (pkg.fullPath) {
                    // generic
                    this.$router.push({
                        name: 'repoGeneric',
                        params: {
                            projectId: pkg.projectId
                        },
                        query: {
                            repoName: pkg.repoName,
                            path: pkg.fullPath
                        }
                    })
                } else {
                    // 依赖源仓库进入制品详情需要知道仓库类型(远程、本地、组合)，根据仓库类型限制操作
                        this.artifactOriginalList?.forEach((item) => {
                            if (item.name === pkg.repoName) {
                                pkg.repoCategory = item.category || ''
                            }
                        })
                    this.$router.push({
                        name: 'commonPackage',
                        params: {
                            projectId: pkg.projectId,
                            repoType: pkg.type.toLowerCase()
                        },
                        query: {
                            repoName: pkg.repoName,
                            packageKey: pkg.key,
                             storeType: pkg.repoCategory?.toLowerCase() || '',
                             version
                        }
                    })
                }
            },
            showDetail (pkg) {
                this.$refs.genericDetail.setData({
                    show: true,
                    loading: false,
                    projectId: pkg.projectId,
                    repoName: pkg.repoName,
                    folder: pkg.folder,
                    path: pkg.fullPath,
                    data: {}
                })
            },
            handlerShare (cardData) {
                this.$refs.genericShareDialog.setData({
                    projectId: cardData.projectId,
                    repoName: cardData.repoName,
                    show: true,
                    loading: false,
                    title: `${this.$t('share')} (${cardData.name})`,
                    path: cardData.fullPath,
                    user: [],
                    ip: [],
                    permits: '',
                    time: 7
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.repo-search-container {
    position: relative;
    height: 100%;
    background-color: white;
    .repo-search-tools {
        padding: 20px 20px 10px;
        z-index: 1;
        background-color: white;
        border-bottom: 1px solid var(--borderColor);
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
    .repo-search-result {
        height: calc(100% - 130px);
        .package-list {
            padding: 10px 20px 0;
        }
    }
}
</style>
