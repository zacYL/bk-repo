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
                        <a href="javascript:;" @click="handlerCreateStore(item.id)">
                            <div class="flex-align-center">
                                <Icon class="pr5" :name="item.icon" size="16" />
                                <span>
                                    {{$t(item.name)}}
                                </span>
                            </div>
                        </a>
                    </li>
                </ul>
            </bk-dropdown-menu>
            <div class="flex-align-center">
                <bk-input
                    v-model.trim="query.name"
                    class="w250"
                    placeholder="请输入仓库名称, 按Enter键搜索"
                    clearable
                    @enter="handlerPaginationChange()"
                    @clear="handlerPaginationChange()"
                    right-icon="bk-icon icon-search">
                </bk-input>
                <bk-select
                    v-model="query.category"
                    class="ml10 w250"
                    @change="handlerPaginationChange()"
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
                    @change="handlerPaginationChange()"
                    :placeholder="$t('allTypes')">
                    <bk-option v-for="type in repoEnum" :key="type" :id="type" :name="type">
                        <div class="flex-align-center">
                            <Icon size="20" :name="type" />
                            <span class="ml10 flex-1 text-overflow">{{type}}</span>
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
                        class="mr5 repo-tag SUCCESS" data-name="内置">
                    </span>
                    <span v-if="row.configuration.settings.system" class="mr5 repo-tag" data-name="系统"></span>
                    <span v-if="row.public" class="mr5 repo-tag WARNING" data-name="公开"></span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('storeTypes')" width="120">
                <template #default="{ row }">
                    <span>{{$t((row.category.toLowerCase() || 'local') + 'Store')}}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('createdDate')" width="150">
                <template #default="{ row }">{{ formatDate(row.createdDate) }}</template>
            </bk-table-column>
            <bk-table-column :label="$t('createdBy')" width="90">
                <template #default="{ row }">
                    {{ userList[row.createdBy] ? userList[row.createdBy].name : row.createdBy }}
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('operation')" width="70">
                <template #default="{ row }">
                    <operation-list
                        :list="[
                            { label: '设置', clickEvent: () => toRepoConfig(row) },
                            !['custom', 'pipeline', 'docker-local'].includes(row.name) && { label: $t('delete'), clickEvent: () => deleteRepo(row) }
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
        <create-repo-dialog ref="createRepo" :store-type="currentStoreType" @refresh="handlerPaginationChange()" @close="onCloseDialog"></create-repo-dialog>
    </div>
</template>
<script>
    import OperationList from '@repository/components/OperationList'
    import createRepoDialog from '@repository/views/repoList/createRepoDialog'
    import { mapState, mapActions } from 'vuex'
    import { repoEnum, storeTypeEnum } from '@repository/store/publicEnum'
    import { formatDate } from '@repository/utils'
    export default {
        name: 'repoList',
        components: { OperationList, createRepoDialog },
        data () {
            return {
                repoEnum,
                storeTypeEnum, // 仓库类型（本地/远程/虚拟）
                isLoading: false,
                canCreate: false,
                repoList: [],
                query: {
                    name: this.$route.query.name,
                    type: this.$route.query.type,
                    category: this.$route.query.category
                },
                pagination: {
                    count: 0,
                    current: 1,
                    limit: 20,
                    limitList: [10, 20, 40]
                },
                isDropdownShow: false,
                currentStoreType: 'local' // 当前选择的仓库类型
            }
        },
        computed: {
            ...mapState(['userList']),
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            projectId () {
                this.handlerPaginationChange()
            }
        },
        created () {
            this.handlerPaginationChange()
            this.getRepoPermission({
                projectId: this.projectId,
                action: 'create'
            }).then(res => {
                this.canCreate = res
            })
        },
        methods: {
            formatDate,
            ...mapActions([
                'getRepoList',
                'deleteRepoList',
                'getRepoPermission'
            ]),
            // 关闭弹窗后需要将当前选中的仓库类型置为初始值，否则会导致再次打开同一种类型的弹窗时逻辑错误，(远程仓库和虚拟仓库也会默认选中generic仓库)
            onCloseDialog () {
                this.currentStoreType = ''
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
                this.getListData()
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
            toPackageList ({ projectId, repoType, name, category }) {
                this.$router.push({
                    name: repoType === 'generic' ? 'repoGeneric' : 'commonList',
                    params: {
                        projectId,
                        repoType
                    },
                    query: {
                        repoName: name,
                        storeType: category?.toLowerCase() || ''
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
                            this.getListData()
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('delete') + this.$t('success')
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
}
</style>
