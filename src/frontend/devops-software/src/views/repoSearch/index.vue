<template>
    <div class="repo-search-container" v-bkloading="{ isLoading }">
        <div class="repo-search-tools flex-column">
            <div class="flex-align-center">
                <type-select
                    :repo-list="repoEnum.filter(r => r.value !== 'generic')"
                    :project-list="projectList"
                    :repo-type="repoType"
                    :artifact-original-list="artifactOriginalList"
                    :condition-list="repoSearchConditionMap.filter((item) => item.id !== 'checkSum')"
                    @change="changeRepoType"
                    @search-artifact="onSearchArtifact">
                </type-select>
            </div>
            <div v-if="resultList.length" class="mt10 flex-end-center">
                <div class="sort-tool flex-align-center">
                    <bk-select
                        style="width:200px;"
                        v-model="property"
                        :clearable="false"
                        @change="changeSortType">
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
        </div>
        <main class="repo-search-result flex-align-center">
            <template v-if="resultList.length">
                <infinite-scroll
                    ref="infiniteScroll"
                    class="package-list flex-1"
                    :is-loading="isLoading"
                    :has-next="hasNextData"
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
                        @click.native="showDependDetail(pkg)">
                    </package-card>
                </infinite-scroll>
            </template>
            <empty-data v-else :is-loading="isLoading" class="flex-1" ex-style="align-self:start;margin-top:130px;"></empty-data>
        </main>
        <searchDependDetail
            v-if="(commonPackageInfo.matchedVersions || []).length"
            ref="searchDependDetailRef"
            :info="commonPackageInfo"
            :version-list="commonPackageInfo.matchedVersions"
            @detail="(version) => showCommonPackageDetail(commonPackageInfo,version)">
        </searchDependDetail>
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
    import searchDependDetail from '@repository/components/searchDependDetail'
    import { mapState, mapActions } from 'vuex'
    import { formatDate } from '@repository/utils'
    import { repoSearchConditionMap, repoEnum } from '@repository/store/publicEnum'
    import { cloneDeep, isEmpty } from 'lodash'
    export default {
        name: 'repoSearch',
        components: {
            packageCard,
            InfiniteScroll,
            typeSelect,
            genericDetail,
            genericShareDialog,
            searchDependDetail
        },
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
                repoSearchConditionMap,
                isLoading: false,
                property: this.$route.query.property || 'lastModifiedDate',
                direction: this.$route.query.direction || 'DESC',
                projectId: this.$route.query.projectId || '',
                packageName: this.$route.query.packageName || '',
                repoType: this.$route.query.repoType || 'docker',
                repoName: this.$route.query.repoName || '',
                pagination: {
                    current: 1,
                    limit: 20
                },
                resultList: [],
                searchArtifactParams: {},
                // 根据制品类型获取到的仓库列表集合
                artifactOriginalList: [],
                // 是否支持滚动加载下一页
                hasNextData: false,
                commonPackageInfo: {}
            }
        },
        computed: {
            ...mapState(['projectList', 'userList'])
        },
        created () {
            // 默认请求获取当前类型的仓库数据，否则会导致刷新时不会请求，进而导致下拉仓库数据无法回显
            this.getRepoSearchArtifactList()
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
                    projectList: this.searchArtifactParams.projectList
                }).then(({ records }) => {
                    scrollLoad ? this.resultList.push(...records) : (this.resultList = records)
                    // 后端接口返回的数组数量是否大于等于当前页码，如果大于等于，表示可能还有下一页，需要支持加载下一页
                    this.hasNextData = records?.length >= this.pagination.limit
                }).finally(() => {
                    this.isLoading = false
                })
            },
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}, scrollLoad = false) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.searckPackageHandler(scrollLoad)
                !scrollLoad && this.$refs.infiniteScroll && this.$refs.infiniteScroll.scrollToTop()
                if (!isEmpty(this.searchArtifactParams)) {
                    const urlParams = cloneDeep(this.searchArtifactParams)
                    // 元数据需要转化后才能显示到url中，例如 metadataProperties[key] = value & metadataProperties[111] = 222
                    if (urlParams.metadataList?.length > 0) {
                        urlParams.metadataList.forEach((metadata) => {
                            urlParams[`metadataProperties[${metadata.key}]`] = metadata.value
                        })
                        delete urlParams.metadataList
                    }
                    // 选择的项目列表处理，例如 projectProperties[0] = test & projectProperties[1] = test2
                    if (urlParams.projectList?.length > 0) {
                        urlParams.projectList.forEach((project, index) => {
                            urlParams[`projectProperties[${index}]`] = project
                        })
                        delete urlParams.projectList
                    }
                    this.$router.replace({
                        query: {
                            repoType: this.repoType,
                            property: this.property,
                            direction: this.direction,
                            ...urlParams
                        }
                    })
                }
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
            // 点击版本详情页，generic仓库直接进入详情页，依赖源仓库在搜索结果和当前包总版本数不一致时需要特殊处理
            showDependDetail (pkg) {
                if (pkg.fullPath) {
                    // generic
                    this.showGenericDetail(pkg)
                } else {
                    // 依赖源仓库
                    if ((isEmpty(this.searchArtifactParams?.name)
                        && isEmpty(this.searchArtifactParams?.version)
                        && (this.searchArtifactParams?.metadataList?.length || 0) === 0) || (pkg.versions === pkg.matchedVersions?.length)) {
                        // 此时表明不是搜索状态下或当前包下的所有版本都符合搜索条件
                        this.showCommonPackageDetail(pkg)
                    } else {
                        this.commonPackageInfo = pkg
                        this.$nextTick(() => {
                            this.$refs.searchDependDetailRef && (this.$refs.searchDependDetailRef.show = true)
                        })
                    }
                }
            },
            showGenericDetail (pkg) {
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
            },
            showCommonPackageDetail (pkg, version) {
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
                        version,
                        // 只有搜索后选择的版本号存在时才需要在版本详情页回显相关参数
                        ...(version ? { searchFlag: true } : '')
                    }
                })
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
