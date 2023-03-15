<template>
    <bk-collapse class="permission-config-container" v-model="activeName" v-bkloading="{ isLoading }">
        <bk-collapse-item v-for="section in collapseList" :key="section.name" :name="section.name">
            <header class="section-header">
                <div class="flex-align-center">
                    <Icon class="mr10" size="20" :name="section.icon" />
                    <span>{{ section.title }}</span>
                    <span class="mr5 permission-actions">（{{ getActions(section.name) }}）</span>
                    <Icon v-if="section === user" class="hover-btn" size="24" name="icon-edit" @click.native.stop="editActionsDialogHandler(section)" />
                </div>
            </header>
            <template #content><div class="section-main">
                <template v-for="part in ['users', 'roles', ...(section !== admin ? ['departments'] : [])]">
                    <header :key="part + 'header'" class="section-sub-title flex-align-center">
                        <span class="mr20">{{section[part].title}}</span>
                        <template v-if="part === 'departments'">
                            <bk-select
                                style="min-width: 300px"
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
                                    expand-icon="bk-icon icon-angle-down"
                                    collapse-icon="bk-icon icon-angle-right"
                                    :lazy-method="(node) => handleDepartmentTreeNode(node, $refs[`${section.name}Tree`][0], section[part])"
                                    :lazy-disabled="(node) => !node.data.has_children"
                                    @check-change="ids => changeAddDepartments(section[part], ids)">
                                </bk-big-tree>
                            </bk-select>
                        </template>
                        <template v-else>
                            <bk-select
                                style="width: 300px"
                                v-model="section[part].addList"
                                multiple
                                clearable
                                searchable
                                :title="section[part].addList.map(u => userList[u] ? userList[u].name : u)"
                                :enable-virtual-scroll="filterSelectOptions(section[part], part).length > 3000"
                                :list="filterSelectOptions(section[part], part)">
                                <bk-option v-for="option in filterSelectOptions(section[part], part)"
                                    :key="option.id"
                                    :id="option.id"
                                    :name="option.name">
                                </bk-option>
                            </bk-select>
                        </template>
                        <i v-if="section[part].addList.length"
                            class="section-sub-add-btn devops-icon icon-check-1"
                            @click="() => {
                                submit('add', part, section)
                            }">
                        </i>
                    </header>
                    <div :key="part + 'data'" class="section-sub">
                        <div class="permission-tag" v-for="tag in filterDeleteTagList(section[part])" :key="tag">
                            {{ getName(part, tag) }}
                            <i class="devops-icon icon-close-circle-shape" @click="handleDeleteTag(tag, part, section)"></i>
                        </div>
                    </div>
                </template>
            </div></template>
        </bk-collapse-item>
        <canway-dialog
            v-if="category !== 'VIRTUAL'"
            :value="editActionsDialog.show"
            width="400"
            height-num="450"
            :title="editActionsDialog.title"
            @cancel="editActionsDialog.show = false">
            <bk-checkbox-group class="vertical-checkbox" v-model="editActionsDialog.actions">
                <bk-checkbox v-for="action in actionList" :key="action.id" :value="action.id" :disabled="action.id === 'READ'">
                    <span>{{ action.name }}</span>
                    <div class="checkbox-tip">
                        <div v-for="tip in action.tips" :key="tip">{{ tip }}</div>
                    </div>
                </bk-checkbox>
            </bk-checkbox-group>
            <template #footer>
                <bk-button theme="default" @click.stop="editActionsDialog.show = false">{{$t('cancel')}}</bk-button>
                <bk-button class="ml10" :loading="editActionsDialog.loading" theme="primary" @click.stop.prevent="handleActionPermission">{{$t('submit')}}</bk-button>
            </template>
        </canway-dialog>
    </bk-collapse>
</template>
<script>
    import { mapState, mapActions } from 'vuex'
    export default {
        name: 'permissionConfig',
        props: {
            // 当前仓库类型(本地/远程/虚拟)
            category: {
                type: String,
                default: ''
            }
        },
        data () {
            return {
                isLoading: false,
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
                        data: [],
                        addList: [],
                        deleteList: []
                    },
                    roles: {
                        title: this.$t('userGroup'),
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
                        data: [],
                        addList: [],
                        deleteList: []
                    },
                    roles: {
                        title: this.$t('userGroup'),
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
                userList: {},
                roleList: {},
                flatDepartment: {},
                departmentTree: [],
                permissionDeptList: [], // 当前项目下有权限的部门集合
                treeIds: [] // 当前所有树节点的id集合
            }
        },
        computed: {
            ...mapState(['userInfo']),
            projectId () {
                return this.$route.params.projectId
            },
            repoName () {
                return this.$route.query.repoName
            },
            filterDeleteTagList () {
                return (target) => {
                    return target.data.filter(v => !target.deleteList.find(w => w === v))
                }
            },
            activeName () {
                if (this.category === 'VIRTUAL') {
                    return ['admin']
                } else {
                    return ['admin', 'user']
                }
            },
            collapseList () {
                if (this.category === 'VIRTUAL') {
                    return [this.admin]
                } else {
                    return [this.admin, this.user]
                }
            },
            actionList () {
                // 虚拟仓库不需要
                if (this.category === 'VIRTUAL') {
                    return []
                } else {
                    const list = [
                        {
                            id: 'READ',
                            name: '查看',
                            tips: [
                                '仓库内所有制品的查看和下载权限'
                            ]
                        },
                    
                        {
                            id: 'UPDATE',
                            name: '修改',
                            tips: [
                                'Generic仓库：重命名、添加元数据、删除元数据',
                                '依赖源仓库：制品版本晋级、添加元数据、删除元数据',
                                '全仓库：禁止使用，解除禁止'
                            ]
                        },
                        {
                            id: 'DELETE',
                            name: '删除',
                            tips: [
                                '仓库内所有制品的页面删除权限'
                            ]
                        }
                    ]
                    // 当前仓库时远程仓库时需要屏蔽上传功能
                    if (this.category !== 'REMOTE') {
                        const write = {
                            id: 'WRITE',
                            name: '上传',
                            tips: [
                                'Generic仓库：新建文件夹、上传文件、复制、移动',
                                '依赖源仓库：上传制品版本'
                            ]
                        }
                        list.splice(1, 0, write)
                    }
                    return list.filter(Boolean)
                }
            }
        },
        created () {
            this.getProjectRoleList({
                projectId: this.projectId
            }).then(res => {
                this.roleList = res.reduce((target, item) => {
                    target[item.id] = item
                    return target
                }, {})
            })
            this.getProjectUserList({
                projectId: this.projectId
            }).then(res => {
                this.userList = res.reduce((target, item) => {
                    target[item.userId] = {
                        id: item.userId,
                        name: item.name
                    }
                    return target
                }, {})
            })
            // 根节点
            this.getRepoDepartmentList({
                username: this.userInfo.username,
                projectId: this.projectId
            }).then(res => {
                this.handleFlatDepartment(res)
                this.departmentTree = res
            })
            this.handlePermissionDetail()
        },
        methods: {
            ...mapActions([
                'getPermissionDetail',
                'getProjectUserList',
                'getProjectRoleList',
                'setUserPermission',
                'setRolePermission',
                'setActionPermission',
                'getRepoDepartmentList',
                'setDepartmentPermission',
                'getRepoDepartmentDetail'
                // 'getRepoAuthDepartmentList'
            ]),
            getName (part, tag) {
                const map = {
                    users: this.userList,
                    roles: this.roleList,
                    departments: this.flatDepartment
                }[part]
                return map[tag] ? map[tag].name : tag
            },
            getActions (name) {
                const actionsName = ['READ', ...this[name].actions.data].map(id => this.actionList.find(action => action.id === id)?.name)
                switch (name) {
                    case 'admin':
                        return '仓库管理，制品管理所有权限'
                    case 'user':
                        return `制品管理权限：${actionsName.filter(Boolean).join('，')}`
                }
            },
            filterSelectOptions (target, part) {
                const list = Object.values({ users: this.userList, roles: this.roleList }[part])
                return list
                    .filter(v => v.id !== 'anonymous')
                    .filter(v => !~target.data.findIndex(w => w === v.id))
            },
            handleDeleteTag (tag, part, section) {
                section[part].deleteList.push(tag)
                this.submit('delete', part, section)
            },
            // 设置当前树节点是否禁用
            setTreeNodeDisabled (treeTarget, treeData) {
                treeData.forEach(item => {
                    treeTarget.setDisabled(item.id, { emitEvent: false, disabled: !item.permission })

                    if (item.has_children && item.children && item.children.length > 0) {
                        this.setTreeNodeDisabled(treeTarget, item.children)
                    }
                })
            },
            initTree (treeTarget, { data: disabled = [], addList: add = [] } = {}) {
                // const noPermissionList = []
                treeTarget.setData(this.departmentTree)
                this.setTreeNodeDisabled(treeTarget, this.departmentTree)
                disabled.forEach(id => {
                    treeTarget.setChecked(id)
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
                const res = await this.getRepoDepartmentList({
                    username: this.userInfo.username,
                    departmentId: node.id,
                    projectId: this.projectId
                })
                if (!res.length) {
                    this.$set(node.data, 'has_children', false)
                } else {
                    this.handleFlatDepartment(res)
                    this.$nextTick(() => {
                        let target = this.departmentTree
                        node.parents.forEach(parent => {
                            target = (target.children || target).find(v => v.id === parent.id).children
                        })
                        target = target.find(v => v.id === node.id)
                        target.children = res
                        this.initTree(root, data)
                        root.setExpanded(node.id)
                    })
                }
                return {}
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
                        departments = Array.from(new Set(departments)).filter(v => !(v in this.flatDepartment))
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
                }).then(() => {
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
                    actions: ['READ', ...JSON.parse(JSON.stringify(data.actions.data))]
                }
            },
            handleActionPermission () {
                if (this.editActionsDialog.loading) return
                this.editActionsDialog.loading = true
                this.setActionPermission({
                    body: {
                        permissionId: this.editActionsDialog.id,
                        actions: this.editActionsDialog.actions.filter(a => a !== 'READ')
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
        padding-left: 20px;
        color: var(--fontPrimaryColor);
        background-color: var(--bgColor);
        border: 1px solid var(--borderColor);
        .permission-actions {
            font-size: 12px;
            font-weight: normal;
            color: var(--fontSubsidiaryColor);
        }
    }
    .section-main {
        border: solid var(--borderColor);
        border-width: 0 1px 1px;
        ::v-deep .bk-select-empty {
            display: none;
        }
        .section-sub-title {
            padding: 20px 10px 10px;
            > :first-child {
                flex-basis: 45px;
                text-align: right;
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
            display: flex;
            flex-wrap: wrap;
            padding-left: 75px;
            padding-bottom: 10px;
            border-bottom: 1px solid var(--borderColor);
            margin-bottom: -1px;
            .permission-tag {
                position: relative;
                margin-right: 15px;
                margin-bottom: 10px;
                padding: 7px 20px;
                background-color: var(--bgHoverLighterColor);
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
.vertical-checkbox {
    .bk-form-checkbox {
        display: flex;
        align-items: flex-start;
        margin-bottom: 20px;
        .checkbox-tip {
            margin-top: 10px;
            color: var(--fontSubsidiaryColor);
            line-height: 1.5;
        }
    }
}
</style>
