<!--
 * @Date: 2024-11-22 10:22:36
 * @LastEditors: xiaoshan
 * @LastEditTime: 2024-11-28 16:16:24
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
                <!-- 权限名称 -->
                <bk-table-column :label="$t('permName')" prop="permName">
                </bk-table-column>
                <!-- 路径集合 -->
                <bk-table-column :label="$t('pathCollection')" prop="repoName" :render-header="renderHeader" show-overflow-tooltip>
                    <template #default="{ row }">
                        <div>
                            {{ row.includePattern.join('、') }}
                        </div>
                    </template>
                </bk-table-column>
                <!-- 创建人 -->
                <bk-table-column :label="$t('createdBy')" prop="createBy">
                </bk-table-column>
                <!-- 创建时间 -->
                <bk-table-column :label="$t('createdDate')" prop="createAt">
                    <template #default="{ row }">
                        {{ formatTime(row.createAt) }}
                    </template>
                </bk-table-column>
                <!-- 更新时间 -->
                <bk-table-column :label="$t('updateTime')" prop="updateAt">
                    <template #default="{ row }">
                        {{ formatTime(row.updateAt) }}
                    </template>
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
        <PermissionSideslider v-bind="PermissionConfig" :is-show.sync="PermissionConfig.isShow" @submit="submit"></PermissionSideslider>
    </div>
</template>
<script>
    import PermissionSideslider from './permissionSideslider.vue'
    import { mapActions } from 'vuex'
    import moment from 'moment'
    export default {
        name: 'permission-config',
        components: {
            PermissionSideslider
        },
        props: {
            baseData: {
                type: Object,
                default: () => ({})
            }
        },
        data () {
            return {
                isLoading: false,
                permissionList: [],
                editRow: {},
                PermissionConfig: {
                    isShow: false,
                    title: this.$t('createPathCollection'),
                    type: 'create',
                    insertForm: {}
                }
            }
        },
        created () {
            this.getPermissionList()
        },
        methods: {
            ...mapActions(['repoPathUpdate', 'repoPathList', 'repoPathDelete', 'repoPathCreate']),
            formatTime (dateTimeString) {
                const momentObj = moment(dateTimeString)
                const formattedDateTime = momentObj.format('YYYY-MM-DD HH:mm:ss.SS')
                return formattedDateTime
            },
            createPermission () {
                this.editRow = {}
                this.PermissionConfig.isShow = true
                this.PermissionConfig.title = this.$t('createPathCollection')
                this.PermissionConfig.type = 'create'
                this.PermissionConfig.insertForm = {}
            },
            editPermission (row) {
                this.editRow = row
                this.PermissionConfig.isShow = true
                this.PermissionConfig.title = this.$t('editPathCollection')
                this.PermissionConfig.type = 'edit'
                this.PermissionConfig.insertForm = {
                    name: row.permName,
                    path: row.includePattern
                }
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
            // 获取权限列表
            getPermissionList () {
                const body = {
                    projectId: this.baseData.projectId,
                    repo: this.baseData.name
                }
                
                this.isLoading = true
                return this.repoPathList(
                    { body }
                ).then(res => {
                    this.permissionList = res
                }).finally(() => {
                    this.isLoading = false
                })
            },
            remove (row) {
                this.$bkInfoDevopsConfirm({
                    subTitle: this.$t('deleteTips'),
                    theme: 'danger',
                    confirmFn: () => {
                        this.repoPathDelete({
                            body: {
                                permissionId: row.id,
                                projectId: this.baseData.projectId
                            }
                        }).then(() => {
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('removeSuccess')
                            })
                            this.getPermissionList()
                        }).catch((err) => {
                            this.$bkMessage({
                                theme: 'error',
                                message: err.message ?? this.$t('removeFail')
                            })
                        })
                    }
                })
            },
            submit (form, cb) {
                const method = this.PermissionConfig.type === 'create' ? this.repoPathCreate : this.repoPathUpdate
                const body = {
                    includePattern: form.path.map(item => item.value),
                    permName: form.name,
                    projectId: this.baseData.projectId,
                    ...this.PermissionConfig.type === 'create'
                        ? {
                            repos: [
                                this.baseData.name
                            ]
                        }
                        : {
                            permissionId: this.editRow.id
                        }
                    
                }
                method(
                    { body }
                ).then(res => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.PermissionConfig.type === 'create' ? this.$t('createSuccess') : this.$t('editSuccess')
                    })
                    this.getPermissionList()
                    cb()
                }).catch((err) => {
                    this.$bkMessage({
                        theme: 'error',
                        message: err.message ?? (this.PermissionConfig.type === 'create' ? this.$t('createFail') : this.$t('editFail'))
                    })
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
