<template>
    <div class="repo-list-container" v-bkloading="{ isLoading }">
        <div class="ml20 mr20 mt10 flex-between-center">
            <bk-dropdown-menu
                v-if="canCreate"
                ref="dropdownHover"
                align="left"
                trigger="click"
                @show="isDropdownShow = true"
                @hide="isDropdownShow = false">
                <div slot="dropdown-trigger">
                    <bk-button
                        icon="plus"
                        theme="primary"
                        :icon-right="isDropdownShow ? 'icon-angle-up' : 'icon-angle-down'"
                        ext-cls="create-btn"
                    >{{ $t('create') }}</bk-button>
                </div>
                <ul class="bk-dropdown-list" slot="dropdown-content">
                    <li v-for="item in storeTypeEnum" :key="item.name">
                        <bk-link theme="default" href="javascript:;" @click="handlerCreateStore(item.id)">
                            <dl>
                                <dt class="flex-align-center">
                                    <Icon class="pr5" :name="item.icon" size="16" />
                                    <span> {{$t(item.name)}} </span>
                                </dt>
                                <dd>
                                    <span class="dropdown-list-info">{{$t(item.info)}}</span>
                                </dd>
                            </dl>
                        </bk-link>
                    </li>
                </ul>
            </bk-dropdown-menu>
            <div class="flex-align-center">
                <bk-input
                    v-model.trim="query.name"
                    class="w250"
                    :placeholder="$t('repoEnterTip')"
                    clearable
                    @enter="handlerPaginationChange"
                    @clear="handlerPaginationChange"
                    right-icon="bk-icon icon-search">
                </bk-input>
                <bk-select
                    v-model="query.category"
                    class="ml10 w250"
                    @change="handlerPaginationChange"
                    :placeholder="$t('allStoreTypes')">
                    <bk-option v-for="category in storeTypeEnum" :key="category.id" :id="category.id" :name="$t(category.name)">
                        <div class="flex-align-center">
                            <Icon size="20" :name="category.icon" />
                            <span class="ml10 flex-1 text-overflow">{{$t(category.name)}}</span>
                        </div>
                    </bk-option>
                </bk-select>
                <bk-select
                    v-model="query.type"
                    class="ml10 w250"
                    @change="handlerPaginationChange"
                    :placeholder="$t('allTypes')">
                    <bk-option v-for="type in repoEnum" :key="type.value" :id="type.value" :name="type.label">
                        <div class="flex-align-center">
                            <Icon size="20" :name="type.value" />
                            <span class="ml10 flex-1 text-overflow">{{type.label}}</span>
                        </div>
                    </bk-option>
                </bk-select>
            </div>
        </div>
        <bk-table
            class="mt10"
            :data="repoList"
            height="calc(100% - 100px)"
            :outer-border="false"
            :row-border="false"
            size="small">
            <template #empty>
                <empty-data :is-loading="isLoading" :search="Boolean(query.name || query.type)"></empty-data>
            </template>
            <bk-table-column :label="$t('repoName')" show-overflow-tooltip>
                <template #default="{ row }">
                    <Icon class="mr5 table-svg" size="16" :name="row.repoType" />
                    <span class="hover-btn" @click="toPackageList(row)">{{replaceRepoName(row.name)}}</span>
                    <span v-if="['custom', 'pipeline', 'docker-local','report'].includes(row.name)"
                        class="mr5 repo-tag SUCCESS" :data-name="$t('builtIn')">
                    </span>
                    <span v-if="row.configuration.settings.system" class="mr5 repo-tag" :data-name="$t('system')"></span>
                    <span v-if="row.public" class="mr5 repo-tag WARNING" :data-name="$t('public')"></span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('storeTypes')" width="180">
                <template #default="{ row }">
                    <span>{{$t((row.category.toLowerCase() || 'local') + 'Store')}}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('createdDate')" width="180">
                <template #default="{ row }">{{ formatDate(row.createdDate) }}</template>
            </bk-table-column>
            <bk-table-column :label="$t('createdBy')" width="180" show-overflow-tooltip>
                <template #default="{ row }">
                    {{ userList[row.createdBy] ? userList[row.createdBy].name : row.createdBy }}
                </template>
            </bk-table-column>
            <bk-table-column v-if="canSetting || canDelete" :label="$t('operation')" width="100">
                <template #default="{ row }">
                    <operation-list
                        :list="[
                            canSetting && { label: $t('setting'), clickEvent: () => toRepoConfig(row) },
                            (!['custom', 'pipeline', 'docker-local','report'].includes(row.name) && canDelete) && { label: $t('delete'), clickEvent: () => deleteRepo(row) }
                        ].filter(Boolean)">
                    </operation-list>
                </template>
            </bk-table-column>
        </bk-table>
        <bk-pagination
            class="p10"
            size="small"
            align="right"
            show-total-count
            :current.sync="pagination.current"
            :limit="pagination.limit"
            :count="pagination.count"
            :limit-list="pagination.limitList"
            @change="current => handlerPaginationChange({ current })"
            @limit-change="limit => handlerPaginationChange({ limit })">
        </bk-pagination>
        <create-repo-dialog ref="createRepo" :store-type="currentStoreType" @refresh="handlerPaginationChange" @close="onCloseDialog"></create-repo-dialog>
    </div>
</template>
<script>
    import OperationList from '@repository/components/OperationList'
    import createRepoDialog from '@repository/views/repoList/createRepoDialog'
    import { mapState, mapActions } from 'vuex'
    import { repoEnum, storeTypeEnum } from '@repository/store/publicEnum'
    import { formatDate, debounce } from '@repository/utils'
    import { cloneDeep } from 'lodash'
    const paginationParams = {
        count: 0,
        current: 1,
        limit: 20,
        limitList: [10, 20, 40]
    }
    export default {
        name: 'repoList',
        components: { OperationList, createRepoDialog },
        data () {
            return {
                repoEnum,
                storeTypeEnum, // 仓库类型（本地/远程/虚拟）
                isLoading: false,
                repoList: [],
                query: {
                    name: this.$route.query.name,
                    type: this.$route.query.type,
                    category: this.$route.query.category,
                    c: this.$route.query.c || 1,
                    l: this.$route.query.l || 20
                },
                pagination: cloneDeep(paginationParams),
                isDropdownShow: false,
                currentStoreType: 'local', // 当前选择的仓库类型
                debounceGetListData: null
            }
        },
        computed: {
            ...mapState(['userList', 'operationPermission']),
            projectId () {
                return this.$route.params.projectId
            },
            // 获取制品仓库菜单中的相关权限
            repoListOperationPermission () {
                return this.operationPermission?.find((res) => res.resourceCode === 'bkrepo')?.actionCodes || []
            },
            // 是否有创建仓库的权限
            canCreate () {
                return this.repoListOperationPermission.includes('create')
            },
            // 是否有设置仓库的权限
            canSetting () {
                return this.repoListOperationPermission.includes('manage')
            },
            // 是否有删除仓库的权限
            canDelete () {
                return this.repoListOperationPermission.includes('repo_delete')
            }
        },
        watch: {
            projectId () {
                this.initData()
            },
            '$route.query' () {
                if (Object.values(this.$route.query).filter(Boolean)?.length === 0) {
                    this.initData()
                }
            }
        },
        created () {
            // 此处的两个顺序不能更换，否则会导致请求数据时报错，防抖这个方法不是function
            this.debounceGetListData = debounce(this.getListData, 100)
            // 当从制品仓库列表页进入依赖源仓库的详情页后点击上方面包屑返回会导致页码相关参数变为string类型，
            // 而bk-pagination的页码相关参数要求为number类型，导致页码不对应，出现一系列问题
            const dependentCurrent = parseInt(this.$route.query.c || 1)
            const dependentLimit = parseInt(this.$route.query.l || 20)
            this.handlerPaginationChange({ current: dependentCurrent, limit: dependentLimit })
        },
        methods: {
            formatDate,
            ...mapActions([
                'getRepoList',
                'deleteRepoList'
            ]),
            // 关闭弹窗后需要将当前选中的仓库类型置为初始值，否则会导致再次打开同一种类型的弹窗时逻辑错误，(远程仓库和虚拟仓库也会默认选中generic仓库)
            onCloseDialog () {
                this.currentStoreType = ''
            },
            initData () {
                // 切换项目或者点击菜单时需要将筛选条件清空，否则会导致点击菜单的时候筛选条件还在，不符合产品要求(点击菜单清空筛选条件，重新请求最新数据)
                this.query = {
                    c: 1,
                    l: 20
                }
                // 此时需要将页码相关参数重置，否则会导致点击制品列表菜单后不能返回首页(页码为1，每页大小为20)
                this.pagination = cloneDeep(paginationParams)
                this.handlerPaginationChange()
            },
            getListData () {
                this.isLoading = true
                this.getRepoList({
                    projectId: this.projectId,
                    ...this.pagination,
                    ...this.query
                }).then(({ records, totalRecords }) => {
                    this.repoList = records.map(v => ({ ...v, repoType: v.type.toLowerCase() }))
                    this.pagination.count = totalRecords
                }).finally(() => {
                    this.isLoading = false
                })
            },
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.$router.replace({
                    query: this.query
                })
                // 此时需要加上防抖，否则在点击菜单的时候会直接触发bk-select的change事件，导致出现多个请求
                this.debounceGetListData ? this.debounceGetListData() : this.getListData()
            },
            // 点击下拉菜单，隐藏下拉框，打开创建仓库弹窗
            handlerCreateStore (type) {
                this.$refs.dropdownHover.hide()
                this.isDropdownShow = false
                this.createRepo(type)
            },
            createRepo (type) {
                this.currentStoreType = type
                this.$refs.createRepo.showDialogHandler()
            },
            toPackageList ({ projectId, repoType, name, category, type }) {
                this.$router.push({
                    name: repoType === 'generic' ? 'repoGeneric' : 'commonList',
                    params: {
                        projectId,
                        repoType
                    },
                    query: {
                        repoName: name,
                        storeType: category?.toLowerCase() || '',
                        ...this.$route.query,
                        type,
                        c: this.pagination.current,
                        l: this.pagination.limit
                    }
                })
            },
            toRepoConfig ({ repoType, name }) {
                this.$router.push({
                    name: 'repoConfig',
                    params: {
                        ...this.$route.params,
                        repoType
                    },
                    query: {
                        ...this.$route.query,
                        repoName: name
                    }
                })
            },
            deleteRepo ({ name }) {
                this.$confirm({
                    theme: 'danger',
                    message: this.$t('deleteRepoTitle', { name }),
                    confirmFn: () => {
                        return this.deleteRepoList({
                            projectId: this.projectId,
                            name
                        }).then(() => {
                            this.debounceGetListData ? this.debounceGetListData() : this.getListData()
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
.repo-list-container {
    height: 100%;
    background-color: white;
    ::v-deep .bk-table td,
    ::v-deep .bk-table th {
        height: 44px;
    }
    // 解决右边icon图标没有垂直居中的问题
    ::v-deep .bk-dropdown-menu .bk-dropdown-trigger .right-icon {
        top: 0;
    }
    .dropdown-list-info{
        color: #8797aa;
        margin-left: -24px;
    }
    ::v-deep .bk-dropdown-menu .bk-dropdown-content .bk-dropdown-list li{
        height: 100%;
    }
    ::v-deep .bk-dropdown-menu .bk-dropdown-list>li>a{
        height: 60px;
        line-height: 26px;
    }
    ::v-deep .bk-dropdown-menu .bk-dropdown-list {
        max-height: 220px;
    }
}
</style>
