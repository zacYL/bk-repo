<template>
    <div class="project-detail-container">
        <bk-tab class="project-detail-tab page-tab" type="unborder-card" :active.sync="tabName">
            <bk-tab-panel name="basic" label="基础信息">
            </bk-tab-panel>
            <bk-tab-panel v-for="tab in [manage, user, role]" :key="tab.name" :name="tab.name" :label="tab.name" style="display:none;">
            </bk-tab-panel>
        </bk-tab>
        <!-- 基础信息 -->
        <bk-form class="ml10 mr10 tab-common" :label-width="75" v-if="tabName === 'basic'">
            <bk-form-item label="项目标识">
                <span>{{ currentProject.id }}</span>
            </bk-form-item>
            <bk-form-item label="项目名称">
                <span class="break-all">{{ currentProject.name }}</span>
            </bk-form-item>
            <bk-form-item label="项目描述">
                <span class="break-all">{{ currentProject.description }}</span>
            </bk-form-item>
            <bk-form-item>
                <bk-button theme="primary" @click="showProjectDialog">修改</bk-button>
            </bk-form-item>
        </bk-form>
        <!-- 其他 -->
        <div v-if="activeTabObj" class="tab-common">
            <div class="flex-align-center">
                <bk-select class="w250 select-user"
                    v-model="activeTabObj.add"
                    multiple
                    searchable
                    placeholder="请选择用户"
                    :show-select-all="tabName === '项目管理员' ? false : true"
                    :enable-virtual-scroll="selectList(activeTabObj).length > 3000"
                    :list="selectList(activeTabObj)">
                    <bk-option v-for="option in selectList(activeTabObj)"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name">
                    </bk-option>
                </bk-select>
                <bk-button :disabled="!activeTabObj.add.length" icon="plus" theme="primary" class="ml10" @click="confirmHandler(activeTabObj, 'add')">{{ $t('add') }}</bk-button>
                <bk-button :disabled="!activeTabObj.delete.length" theme="default" class="ml10" @click="confirmHandler(activeTabObj, 'delete')">批量移除</bk-button>
            </div>
            <bk-table
                class="mt10"
                :data="activeTabObj.items"
                height="calc(100% - 40px)"
                :outer-border="false"
                :row-border="false"
                size="small"
                :fit="true"
                @select="list => {
                    activeTabObj.delete = list
                }"
                @select-all="list => {
                    activeTabObj.delete = list
                }">
                <template #empty><empty-data style="margin-top:100px;"></empty-data></template>
                <bk-table-column type="selection" width="60"></bk-table-column>
                <bk-table-column :label="activeTabObj.name">
                    <template #default="{ row }">
                        {{ (userList[row] && userList[row].name) || (roleList[row] && roleList[row].name) || row }}
                    </template>
                </bk-table-column>
            </bk-table>
        </div>
        <project-info-dialog ref="projectInfoDialog"></project-info-dialog>
    </div>
</template>
<script>
    import projectInfoDialog from './projectInfoDialog'
    import { mapState, mapActions } from 'vuex'
    export default {
        name: 'projectConfig',
        components: { projectInfoDialog },
        data () {
            return {
                tabName: 'basic',
                manage: {
                    id: 'manage',
                    loading: false,
                    name: '项目管理员',
                    type: 'user',
                    items: [],
                    add: [],
                    delete: []
                },
                user: {
                    id: 'user',
                    loading: false,
                    name: '项目用户',
                    type: 'user',
                    items: [],
                    add: [],
                    delete: []
                },
                role: {
                    id: 'role',
                    loading: false,
                    name: '项目用户组',
                    type: 'role',
                    items: [],
                    add: [],
                    delete: []
                },
                roleList: {}
            }
        },
        computed: {
            ...mapState(['userList', 'projectList']),
            projectId () {
                return this.$route.query.projectId || this.$route.params.projectId
            },
            currentProject () {
                return this.projectList.find(project => project.id === this.projectId) || {}
            },
            activeTabObj () {
                return [this.manage, this.user, this.role].find(item => item.name === this.tabName)
            }
        },
        watch: {
            currentProject () {
                this.initProjectConfig()
            }
        },
        beforeRouteEnter (to, from, next) {
            const breadcrumb = to.meta.breadcrumb
            if (to.query.projectId) {
                breadcrumb.splice(0, breadcrumb.length, { name: 'projectManage', label: to.query.projectId }, { name: 'projectConfig', label: '项目设置' })
            } else {
                breadcrumb.splice(0, breadcrumb.length, { name: 'projectConfig', label: '项目设置' })
            }
            next()
        },
        created () {
            this.currentProject.id && this.initProjectConfig()
            this.getRoleList().then(res => {
                this.roleList = res.reduce((target, item) => {
                    target[item.id] = item
                    return target
                }, {})
            })
        },
        methods: {
            ...mapActions([
                'getRoleList',
                'getProjectPermission',
                'setUserPermission',
                'setRolePermission'
            ]),
            initProjectConfig () {
                this.getProjectPermission({ projectId: this.currentProject.id }).then(data => {
                    const manage = data.find(p => p.permName === 'project_manage_permission') || {}
                    const view = data.find(p => p.permName === 'project_view_permission') || {}
                    this.manage = {
                        ...this.manage,
                        id: manage.id,
                        items: manage.users
                    }
                    this.user = {
                        ...this.user,
                        id: view.id,
                        items: view.users
                    }
                    this.role = {
                        ...this.role,
                        id: view.id,
                        items: view.roles
                    }
                })
            },
            selectList (tab) {
                return Object.values(tab.type === 'role' ? this.roleList : this.userList)
                    .filter(v => v.id !== 'anonymous')
                    .filter(v => !~tab.items.findIndex(w => w === v.id))
            },
            confirmHandler (tab, type) {
                if (tab.loading || !tab[type].length) return
                const key = { user: 'userId', role: 'rId' }[tab.type]
                const value = {
                    add: [...tab.items, ...tab.add],
                    delete: tab.items.filter(v => !tab.delete.find(w => w === v))
                }[type]
                const deleteName = tab.delete.map(v => this.userList[v]?.name || this.roleList[v]?.name || v)

                const confirmFn = () => {
                    tab.loading = true
                    return ({ user: this.setUserPermission, role: this.setRolePermission })[tab.type]({
                        body: {
                            permissionId: tab.id,
                            [key]: value
                        }
                    }).then(() => {
                        this.$bkMessage({
                            theme: 'success',
                            message: (type === 'add' ? this.$t('add') : this.$t('delete')) + this.$t('success')
                        })
                        this.initProjectConfig()
                        tab[type] = []
                    }).finally(() => {
                        tab.loading = false
                    })
                }

                type === 'add'
                    ? confirmFn()
                    : this.$confirm({
                        theme: 'danger',
                        message: `确认移除 ${deleteName} ?`,
                        confirmFn
                    })
            },
            showProjectDialog () {
                const { id = '', name = '', description = '' } = this.currentProject
                this.$refs.projectInfoDialog.setData({
                    show: true,
                    loading: false,
                    add: !id,
                    id,
                    name,
                    description
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.project-detail-container {
    // height: 100%;
    background-color: white;
    .project-detail-tab {
        // height: 100%;
        ::v-deep .bk-tab-section {
               display: none;
        }
    }
}
.tab-common{
    height: calc(100% - 60px);
    padding: 20px;
    border-radius: 0 0 2px 2px;
}
</style>
