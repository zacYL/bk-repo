<!--
 * @Date: 2024-11-22 10:22:36
 * @LastEditors: xiaoshan
 * @LastEditTime: 2024-11-22 14:09:48
 * @FilePath: /artifact/src/frontend/devops-repository/src/views/repoConfig/permissionConfig/index.vue
-->
<template>
    <div style="width: 100%;height: 100%;display: flex;flex-direction: column;">
        <div class="flex-align-center">
            <bk-button theme="primary" icon="plus" @click="createPermission">{{ $t('create') }}</bk-button>
        </div>
        <main>
            <bk-table
                class="mt10 scan-table"
                height="100%"
                :data="permissionList"
                :outer-border="false"
                :row-border="false"
                row-key="id"
                size="small">
                <template #empty>
                    <empty-data :is-loading="isLoading"></empty-data>
                </template>
                <!-- 路径集合 -->
                <bk-table-column :label="$t('pathCollection')" prop="repoName" :render-header="renderHeader" show-overflow-tooltip>
                </bk-table-column>
                <!-- 创建人 -->
                <bk-table-column :label="$t('createdBy')" prop="repoName">
                </bk-table-column>
                <!-- 创建时间 -->
                <bk-table-column :label="$t('createdDate')" prop="repoName">
                </bk-table-column>
                <!-- 更新时间 -->
                <bk-table-column :label="$t('updateTime')" prop="repoName">
                </bk-table-column>
                <!-- 操作 -->
                <bk-table-column :label="$t('operation')">
                    <template #default="{ row }">
                        <bk-button theme="primary" text class="mr10" @click="editPermission(row)">{{$t('edit')}}</bk-button>
                        <bk-button theme="danger" text @click="remove(row)">{{$t('delete')}}</bk-button>
                    </template>
                </bk-table-column>
            </bk-table>
        </main>
        <bk-pagination
            class="p10"
            size="small"
            align="right"
            show-total-count
            @change="current => handlerPaginationChange({ current })"
            @limit-change="limit => handlerPaginationChange({ limit })"
            :current.sync="pagination.current"
            :limit="pagination.limit"
            :count="pagination.count"
            :limit-list="pagination.limitList">
        </bk-pagination>
        <PermissionSideslider v-bind="PermissionConfig" :is-show.sync="PermissionConfig.isShow"></PermissionSideslider>
    </div>
</template>
<script>
    import { cloneDeep } from 'lodash'
    import PermissionSideslider from './permissionSideslider.vue'
    const paginationParams = {
        count: 0,
        current: 1,
        limit: 20,
        limitList: [10, 20, 40]
    }
    export default {
        name: 'permission-config',
        components: {
            PermissionSideslider
        },
        data () {
            return {
                isLoading: false,
                pagination: cloneDeep(paginationParams),
                permissionList: [],
                PermissionConfig: {
                    isShow: false,
                    title: this.$t('createPathCollection'),
                    type: 'create',
                    insertForm: {}
                }
            }
        },
        created () {
            this.handlerPaginationChange({ current: 1, limit: 20 })
        },
        methods: {
            createPermission () {
                this.PermissionConfig.isShow = true
                this.PermissionConfig.title = this.$t('createPathCollection')
                this.PermissionConfig.type = 'create'
            },
            editPermission (row) {
                this.PermissionConfig.isShow = true
                this.PermissionConfig.title = this.$t('editPathCollection')
                this.PermissionConfig.type = 'edit'
                this.PermissionConfig.insertForm = row
            },
            renderHeader (h, data) {
                const directive = {
                    name: 'bkTooltips',
                    content: '',
                    html: `<p>${this.$t('pathCollectionDesc')}</p>
                    <p>${this.$t('operationMethod')}</p>
                    <p>${this.$t('pathCollectionTips1')}</p>
                    <p>${this.$t('pathCollectionTips2')}</p>`,
                    placement: 'right'
                }
                return h(
                    'span', // 标签名
                    {
                        style: { borderBottom: '1px dashed' },
                        directives: [ // 指令
                            {
                                name: 'bk-tooltips',
                                value: directive
                            }
                        ]
                    },
                    this.$t('pathCollection') // 子节点（文本）
                )
            },
            // 分页
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getRecycleBinList()
            },
            // 获取权限列表
            getPermissionList () {
                const body = {
                    page: {
                        pageNumber: this.pagination.current,
                        pageSize: this.pagination.limit
                    },
                    sort: {
                        properties: [
                            'lastModifiedDate'
                        ],
                        direction: 'DESC'
                    },
                    rule: {
                        rules: [
                            {
                                field: 'projectId',
                                value: this.projectId,
                                operation: 'EQ'
                            },
                            {
                                field: 'name',
                                value: this.name,
                                operation: 'EQ'
                            }
                        ],
                        relation: 'AND'
                    }
                }
                this.isLoading = true
                const url = '/repository/api/packageVersion/search'
                return this.$ajax.post(
                    url,
                    body
                ).then(res => {
                    this.permissionList = res.records
                    this.pagination.count = res.totalRecords
                }).finally(() => {
                    this.isLoading = false
                })
            },

            resetTable () {
                let current = 0
                if (this.permissionList.length === 1) {
                    if (this.pagination.current === 1) {
                        current = 1
                    } else {
                        current = this.pagination.current - 1
                    }
                } else {
                    current = this.pagination.current
                }
                this.handlerPaginationChange({ current: current })
            },
            remove (row) {
                this.$bkInfoDevopsConfirm({
                    subTitle: this.$t('permanentlyDeleteTips'),
                    theme: 'danger',
                    confirmFn: () => {
                        // todo 缺一个删除接口
                        Promise.resolve().then(() => {
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('removeSuccess')
                            })
                            this.resetTable()
                        }).catch(() => {
                            this.$bkMessage({
                                theme: 'error',
                                message: this.$t('removeFail')
                            })
                        })
                    }
                })
            }

        }
    }
</script>
<style lang="scss" scoped>
main {
    flex: 1;
    min-width: 0;
    min-height: 0;
}
</style>
