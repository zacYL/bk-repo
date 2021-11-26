<template>
    <bk-collapse class="permission-config-container" v-model="activeName" v-bkloading="{ isLoading }">
        <bk-collapse-item v-for="section in [admin, user, viewer]" :key="section.name" :name="section.name">
            <header class="section-header">
                <div class="flex-align-center">
                    <icon class="mr10" size="20" :name="section.icon"></icon>
                    <span>{{ section.title }}</span>
                    <span v-if="getActions(section.actions.data)" class="mr10 permission-actions">（{{ getActions(section.actions.data) }}）</span>
                    <i v-if="section === user" class="devops-icon icon-edit hover-btn" @click.stop="editActionsDialogHandler(section)"></i>
                </div>
            </header>
            <div slot="content" class="section-main">
                <template v-for="part in ['users', 'roles', ...(section !== admin ? ['departments'] : [])]">
                    <header :key="part + 'header'" class="section-sub-title flex-align-center">
                        <span>{{section[part].title}}</span>
                        <i class="ml10 devops-icon hover-btn"
                            :class="section[part].showAddArea ? 'icon-minus-square' : 'icon-plus-square'"
                            @click="handleShowAddArea(section[part])">
                        </i>
                        <div v-show="section[part].showAddArea" :key="part + 'operation'" class="ml15 flex-align-center">
                            <template v-if="part === 'departments'">
                                <bk-select
                                    style="min-width: 350px"
                                    searchable
                                    multiple
                                    v-model="section[part].addList"
                                    :remote-method="(keyword) => $refs[`${section.name}Tree`][0].filter(keyword)"
                                    :display-tag="true"
                                    :tag-fixed-height="false"
                                    :show-empty="false"
                                    @toggle="show => show && initTree($refs[`${section.name}Tree`][0], section[part])"
                                    @tab-remove="({ id }) => $refs[`${section.name}Tree`][0].setChecked(id, { emitEvent: true, checked: false })"
                                    @clear="$refs[`${section.name}Tree`][0].removeChecked({ emitEvent: false })">
                                    <bk-big-tree
                                        :ref="`${section.name}Tree`"
                                        show-checkbox
                                        :check-strictly="false"
                                        show-link-line
                                        :lazy-method="(node) => handleDepartmentTreeNode(node, $refs[`${section.name}Tree`][0], section[part])"
                                        @check-change="ids => changeAddDepartments(section[part], ids)">
                                    </bk-big-tree>
                                </bk-select>
                            </template>
                            <template v-else>
                                <bk-tag-input
                                    style="min-width: 250px"
                                    v-model="section[part].addList"
                                    :list="filterSelectOptions(section[part], part)"
                                    :search-key="['id', 'name']"
                                    placeholder="请输入，按Enter键确认"
                                    trigger="focus"
                                    allow-create>
                                </bk-tag-input>
                            </template>
                            <i v-if="section[part].addList.length"
                                class="section-sub-add-btn devops-icon icon-check-1"
                                @click="() => {
                                    submit('add', part, section)
                                }">
                            </i>
                        </div>
                    </header>
                    <div :key="part + 'data'" class="section-sub">
                        <div class="section-sub-main mt10">
                            <div class="permission-tag" v-for="tag in filterDeleteTagList(section[part])" :key="tag">
                                {{ getName(part, tag) }}
                                <i class="devops-icon icon-close-circle-shape" @click="handleDeleteTag(tag, part, section)"></i>
                            </div>
                        </div>
                    </div>
                </template>
            </div>
        </bk-collapse-item>
        <canway-dialog
            :value="editActionsDialog.show"
            width="410"
            height-num="274"
            :title="editActionsDialog.title"
            @cancel="editActionsDialog.show = false">
            <bk-checkbox-group v-model="editActionsDialog.actions">
                <bk-checkbox v-for="action in actionList" :key="action.id" class="m10" :value="action.id">{{ action.name }}</bk-checkbox>
            </bk-checkbox-group>
            <template #footer>
                <bk-button theme="default" @click.stop="editActionsDialog.show = false">{{$t('cancel')}}</bk-button>
                <bk-button :loading="editActionsDialog.loading" theme="primary" @click.stop.prevent="handleActionPermission">{{$t('submit')}}</bk-button>
            </template>
        </canway-dialog>
    </bk-collapse>
</template>
<script>
    import { mapState, mapActions } from 'vuex'
    export default {
        name: 'permissionConfig',
        data () {
            return {
                isLoading: false,
                activeName: ['admin', 'user', 'viewer'],
                editActionsDialog: {
                    show: false,
                    loading: false,
                    id: '',
                    name: '',
                    title: '',
                    actions: []
                },
                admin: {
                    name: 'admin',
                    loading: false,
                    title: this.$t('admin'),
                    icon: 'perm-controller',
                    id: '',
                    actions: {
                        data: []
                    },
                    users: {
                        title: this.$t('user'),
                        showAddArea: false,
                        data: [],
                        addList: [],
                        deleteList: []
                    },
                    roles: {
                        title: this.$t('userGroup'),
                        showAddArea: false,
                        data: [],
                        addList: [],
                        deleteList: []
                    },
                    departments: {
                        title: '组织',
                        showAddArea: false,
                        data: [],
                        addList: [],
                        deleteList: []
                    }
                },
                user: {
                    name: 'user',
                    loading: false,
                    title: this.$t('users'),
                    icon: 'perm-user',
                    id: '',
                    actions: {
                        data: []
                    },
                    users: {
                        title: this.$t('user'),
                        showAddArea: false,
                        data: [],
                        addList: [],
                        deleteList: []
                    },
                    roles: {
                        title: this.$t('userGroup'),
                        showAddArea: false,
                        data: [],
                        addList: [],
                        deleteList: []
                    },
                    departments: {
                        title: '组织',
                        showAddArea: false,
                        data: [],
                        addList: [],
                        deleteList: []
                    }
                },
                viewer: {
                    name: 'viewer',
                    loading: false,
                    title: '查看者',
                    icon: 'perm-viewer',
                    id: '',
                    actions: {
                        data: []
                    },
                    users: {
                        title: '用户',
                        showAddArea: false,
                        data: [],
                        addList: [],
                        deleteList: []
                    },
                    roles: {
                        title: '用户组',
                        showAddArea: false,
                        data: [],
                        addList: [],
                        deleteList: []
                    },
                    departments: {
                        title: '组织',
                        showAddArea: false,
                        data: [],
                        addList: [],
                        deleteList: []
                    }
                },
                // userList: {},
                roleList: {},
                flatDepartment: {},
                departmentTree: [],
                actionList: []
            }
        },
        computed: {
            ...mapState(['userList', 'userInfo']),
            projectId () {
                return this.$route.params.projectId
            },
            repoName () {
                return this.$route.query.repoName
            },
            getName () {
                return (part, tag) => {
                    const map = {
                        users: this.userList,
                        roles: this.roleList,
                        departments: this.flatDepartment
                    }[part]
                    return map[tag] ? map[tag].name : tag
                }
            },
            getActions () {
                return (actions) => {
                    return actions.map(v => this.actionList.find(w => w.id === v)?.name).filter(Boolean).join('，')
                }
            },
            filterDeleteTagList () {
                return (target) => {
                    return target.data.filter(v => !target.deleteList.find(w => w === v))
                }
            }
        },
        created () {
            this.getRepoActions({
                projectId: this.projectId,
                repoName: this.repoName
            }).then(res => {
                this.actionList = res.map(v => ({ id: v.action, name: v.nickName }))
            })
            this.getRepoRoleList({
                projectId: this.projectId,
                repoName: this.repoName
            }).then(res => {
                this.roleList = res.reduce((target, item) => {
                    target[item.id] = item
                    return target
                }, {})
            })
            // 根节点
            this.getRepoDepartmentList({
                username: this.userInfo.username
            }).then(res => {
                this.handleFlatDepartment(res)
                this.departmentTree = res.map(v => ({ ...v, has_children: true }))
            })
            this.handlePermissionDetail()
        },
        methods: {
            ...mapActions([
                'getPermissionDetail',
                'getRepoActions',
                'getRepoRoleList',
                'setUserPermission',
                'setRolePermission',
                'setActionPermission',
                'getRepoDepartmentList',
                'setDepartmentPermission',
                'getRepoDepartmentDetail'
            ]),
            filterSelectOptions (target, part) {
                const list = Object.values({ users: this.userList, roles: this.roleList }[part])
                return list
                    .filter(v => v.id !== 'anonymous')
                    .filter(v => !~target.data.findIndex(w => w === v.id))
            },
            handleShowAddArea (target) {
                target.showAddArea = !target.showAddArea
            },
            handleDeleteTag (tag, part, section) {
                section[part].deleteList.push(tag)
                this.submit('delete', part, section)
            },
            initTree (treeTarget, { data: disabled = [], addList: add = [] } = {}) {
                treeTarget.setData(this.departmentTree)
                disabled.forEach(id => {
                    treeTarget.setChecked(id)
                    treeTarget.setDisabled(id)
                })
                add.forEach(id => {
                    treeTarget.setChecked(id)
                })
            },
            changeAddDepartments (treeTarget, ids) {
                treeTarget.addList = ids.filter(id => !treeTarget.data.find(exist => id === exist))
            },
            handleFlatDepartment (departments) {
                departments.forEach(v => {
                    this.$set(this.flatDepartment, v.id, v)
                })
            },
            async handleDepartmentTreeNode (node, root, data) {
                // 叶节点
                if (!node.data.has_children) return ({ data: [], leaf: [] })
                // 枝节点
                const res = await this.getRepoDepartmentList({
                    username: this.userInfo.username,
                    departmentId: node.id
                })
                this.handleFlatDepartment(res)
                this.$nextTick(() => {
                    let target = this.departmentTree
                    node.parents.forEach(parent => {
                        target = (target.children || target).find(v => v.id === parent.id).children
                    })
                    target = target.find(v => v.id === node.id)
                    target.children = res
                    if (root) {
                        this.initTree(root, data)
                        root.setExpanded([node.id])
                    }
                })
                return {
                    data: [],
                    leaf: res.filter(v => !v.has_children).map(w => w.id)
                }
            },
            handlePermissionDetail (target, origin, id) {
                this.isLoading = true
                return this.getPermissionDetail({
                    projectId: this.projectId,
                    repoName: this.repoName
                }).then(res => {
                    if (target && origin && id) {
                        this[origin][target].data = res.find(v => v.id === id)[target]
                    } else {
                        let departments = []
                        res.forEach(part => {
                            const perm = this[part.permName.replace(/^.*_([^_]+)$/, '$1')]
                            perm.id = part.id
                            perm.users.data = part.users
                            perm.roles.data = part.roles
                            perm.departments.data = part.departments
                            perm.actions.data = part.actions
                            departments = departments.concat(part.departments)
                        })
                        departments = Array.from(new Set(departments)).filter(v => !this.flatDepartment.hasOwnProperty(v))
                        departments.length && this.getRepoDepartmentDetail({ body: departments }).then(this.handleFlatDepartment)
                    }
                }).finally(() => {
                    this.isLoading = false
                })
            },
            submit (type, part, section) {
                if (section.loading) return
                section.loading = true
                const fn = {
                    users: this.setUserPermission,
                    roles: this.setRolePermission,
                    departments: this.setDepartmentPermission
                }[part]
                const key = {
                    users: 'userId',
                    roles: 'rId',
                    departments: 'departmentId'
                }[part]
                const value = {
                    add: [...section[part].data, ...section[part].addList],
                    delete: section[part].data.filter(v => !section[part].deleteList.find(w => w === v))
                }[type]
                fn({
                    body: {
                        permissionId: section.id,
                        [key]: value
                    }
                }).then(res => {
                    this.$bkMessage({
                        theme: 'success',
                        message: (type === 'add' ? this.$t('add') : this.$t('delete')) + this.$t('success')
                    })
                    this.handlePermissionDetail(part, section.name, section.id).then(() => {
                        section[part][`${type}List`] = []
                    })
                }).finally(() => {
                    section.loading = false
                })
            },
            cancel (target) {
                target.deleteList = []
            },
            editActionsDialogHandler (data) {
                this.editActionsDialog = {
                    show: true,
                    loading: false,
                    id: data.id,
                    name: data.name,
                    title: data.title + '权限配置',
                    actions: JSON.parse(JSON.stringify(data.actions.data))
                }
            },
            handleActionPermission () {
                if (this.editActionsDialog.loading) return
                this.editActionsDialog.loading = true
                this.setActionPermission({
                    body: {
                        permissionId: this.editActionsDialog.id,
                        actions: this.editActionsDialog.actions
                    }
                }).then(res => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('save') + this.$t('success')
                    })
                    this.editActionsDialog.show = false
                    this.handlePermissionDetail('actions', this.editActionsDialog.name, this.editActionsDialog.id)
                }).finally(() => {
                    this.editActionsDialog.loading = false
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.permission-config-container {
    margin: 0 -10px;
    ::v-deep .bk-collapse-item {
        margin-bottom: 20px;
        .bk-collapse-item-detail {
            color: inherit;
        }
        .bk-collapse-item-header {
            position: relative;
            height: 42px;
            line-height: 40px;
            .icon-angle-right {
                padding: 0 15px;
            }
        }
    }
    .section-header {
        padding-left: 10px;
        color: var(--fontPrimaryColor);
        background-color: var(--bgHoverColor);
        border: 1px solid var(--borderColor);
        font-weight: bold;
        .permission-actions {
            font-size: 12px;
            font-weight: normal;
            color: var(--fontPrimaryColor);
        }
    }
    .section-main {
        padding: 10px;
        border: solid var(--borderColor);
        border-width: 0 1px 1px;
        ::v-deep .bk-select-empty {
            display: none;
        }
        .section-sub-title {
            height: 52px;
            padding: 10px;
            border-bottom: 1px solid var(--borderColor);
            > :first-child {
                flex-basis: 45px;
            }
            .section-sub-add-btn {
                position: relative;
                z-index: 1;
                padding: 9px;
                color: white;
                margin-left: -2px;
                border-radius: 0 2px 2px 0;
                background-color: var(--primaryColor);
                cursor: pointer;
                &:hover {
                    background-color: var(--primaryHoverColor);
                }
            }
        }
        .section-sub {
            margin-bottom: 20px;
            .section-sub-main {
                display: flex;
                flex-wrap: wrap;
                .permission-tag {
                    position: relative;
                    margin-right: 15px;
                    margin-bottom: 10px;
                    padding: 7px 20px;
                    background-color: var(--bgHoverColor);
                    .icon-close-circle-shape {
                        display: none;
                        position: absolute;
                        top: -5px;
                        right: -5px;
                        color: var(--dangerColor);
                        cursor: pointer;
                    }
                    &:hover .icon-close-circle-shape {
                        display: block;
                    }
                }
            }
        }
    }
}
</style>
