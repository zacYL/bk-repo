<template>
    <div class="permission-config-container" v-bkloading="{ isLoading }">
        <header class="flex-align-center">
            <bk-button class="pl5 pr5" :theme="'primary'" @click="addPermissionUnit" icon="plus">
                {{ $t('add') }}
            </bk-button>
            <bk-select
                class="ml10 w140"
                v-model="type"
                :placeholder="$t('type')"
                @change="handlerPaginationChange()">
                <bk-option v-for="item in Object.values(typeList)" :key="item.id" :id="item.id" :name="item.name"></bk-option>
            </bk-select>
            <bk-input
                class="ml10 w220"
                v-model.trim="name"
                :clearable="true"
                :placeholder="$t('name')"
                @change="handlerPaginationChange()">
            </bk-input>
            <div class="flex-1 flex-end-center">
                <span class="hover-btn flex-align-center" @click="deletePermissionUnit()"><i class="mr5 devops-icon icon-delete"></i>批量删除</span>
            </div>
        </header>
        <bk-table
            class="mt10 permission-table"
            height="calc(100% - 84px)"
            :data="filterPermissionUnits"
            :outer-border="false"
            :row-border="false"
            size="small"
            @selection-change="selectionChange">
            <bk-table-column type="selection" width="60"></bk-table-column>
            <bk-table-column :label="$t('type')">
                <template #default="{ row }">
                    {{ typeList[row.unitType].name }}
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('name')">
                <template #default="{ row }">
                    {{ getUnitName(row) }}
                </template>
            </bk-table-column>
            <bk-table-column label="是否允许推送">
                <template #default="{ row }">
                    <div class="flex-align-center">
                        <bk-switcher class="mr10" :key="row.unitId" v-model="row.allowPush" @change="changePushStatus(row)"></bk-switcher>
                        <div>{{row.allowPush ? '是' : '否'}}</div>
                    </div>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('operation')" width="100">
                <template #default="{ row }">
                    <div class="flex-align-center">
                        <i class="devops-icon icon-delete hover-btn" @click="deletePermissionUnit([row])"></i>
                    </div>
                </template>
            </bk-table-column>
        </bk-table>
        <bk-pagination
            class="mt10"
            size="small"
            align="right"
            show-total-count
            show-selection-count
            :selection-count="deleteUnits.length"
            :current.sync="pagination.current"
            :limit="pagination.limit"
            :count="pagination.count"
            :limit-list="pagination.limitList"
            @change="current => handlerPaginationChange({ current })"
            @limit-change="limit => handlerPaginationChange({ limit })">
        </bk-pagination>
        <bk-dialog
            v-model="addDialog.show"
            title="添加"
            width="640"
            :close-icon="false"
            :quick-close="false"
            :draggable="false">
            <bk-form ref="addpermissionForm" :label-width="120">
                <bk-form-item :label="$t('type')">
                    <bk-radio-group v-model="addDialog.type" @change="addDialog.list = []">
                        <bk-radio class="mr20" v-for="item in Object.values(typeList)" :key="item.id" :value="item.id">{{ item.name }}</bk-radio>
                    </bk-radio-group>
                </bk-form-item>
                <template v-if="addDialog.type === 'USER'">
                    <bk-form-item :label="$t('user')">
                        <bk-select
                            v-model="addDialog.list"
                            multiple
                            display-tag
                            searchable
                            :enable-virtual-scroll="filterUserList.length > 3000"
                            :list="filterUserList">
                            <bk-option
                                v-for="option in filterUserList"
                                :key="option.id"
                                :id="option.id"
                                :name="option.name">
                            </bk-option>
                        </bk-select>
                    </bk-form-item>
                </template>
                <template v-else>
                    <bk-form-item :label="$t('department')">
                        <bk-select
                            style="min-width: 350px"
                            searchable
                            multiple
                            v-model="addDialog.list"
                            :remote-method="(keyword) => $refs.departmentTree.filter(keyword)"
                            :display-tag="true"
                            :tag-fixed-height="false"
                            :show-empty="false"
                            @toggle="show => show && initTree()"
                            @tab-remove="({ id }) => $refs.departmentTree.setChecked(id, { emitEvent: true, checked: false })"
                            @clear="$refs.departmentTree.removeChecked({ emitEvent: false })">
                            <bk-big-tree
                                ref="departmentTree"
                                show-checkbox
                                :check-strictly="false"
                                show-link-line
                                :lazy-method="(node) => handleDepartmentTreeNode(node)"
                                @check-change="ids => changeAddDepartments(ids)">
                            </bk-big-tree>
                        </bk-select>
                    </bk-form-item>
                </template>
                <bk-form-item label="是否允许推送">
                    <div class="flex-align-center">
                        <bk-switcher class="mr10" v-model="addDialog.allowPush"></bk-switcher>
                        <div>{{addDialog.allowPush ? '是' : '否'}}</div>
                    </div>
                </bk-form-item>
            </bk-form>
            <template #footer>
                <bk-button :loading="addDialog.loading" theme="primary" @click="confirmAdd">{{$t('submit')}}</bk-button>
                <bk-button theme="default" @click="addDialog.show = false">{{$t('cancel')}}</bk-button>
            </template>
        </bk-dialog>
    </div>
</template>
<script>
    import { mapState, mapActions } from 'vuex'
    export default {
        name: 'permissionConfig',
        data () {
            return {
                isLoading: false,
                type: '',
                typeList: {
                    USER: {
                        id: 'USER',
                        name: '用户'
                    },
                    DEPARTMENT: {
                        id: 'DEPARTMENT',
                        name: '部门'
                    }
                },
                name: '',
                permissionUnits: {
                    user: [],
                    department: []
                },
                departmentTree: [],
                departmentMap: {},
                pagination: {
                    count: 0,
                    current: 1,
                    limit: 20,
                    selectionCount: 0,
                    limitList: [10, 20, 40]
                },
                addDialog: {
                    loading: false,
                    show: false,
                    type: 'USER',
                    allowPush: false,
                    list: []
                },
                deleteUnits: []
            }
        },
        computed: {
            ...mapState(['userList']),
            repoName () {
                return this.$route.params.repoName
            },
            filterPermissionUnits () {
                const { current, limit } = this.pagination
                return [...this.permissionUnits.department, ...this.permissionUnits.user].filter(unit => {
                    const unitName = this.getUnitName(unit)
                    return (!this.type || unit.unitType === this.type) && ~unitName.indexOf(this.name)
                }).slice((current - 1) * limit, current * limit)
            },
            filterUserList () {
                return Object.values(this.userList).filter(user => {
                    return !~this.permissionUnits.user.findIndex(unit => unit.unitId === user.id)
                })
            }
        },
        created () {
            this.handleGetPermissionUnits()
            // 根节点
            this.getRepoDepartmentList({}).then(res => {
                res.forEach(department => {
                    this.$set(this.departmentMap, department.id, department)
                })
                this.departmentTree = res.map(v => ({ ...v, has_children: true }))
            })
        },
        methods: {
            ...mapActions([
                'getPermissionUnits',
                'addPermissionUnits',
                'editPermissionUnits',
                'deletePermissionUnits',
                'getRepoDepartmentDetail',
                'getRepoDepartmentList'
            ]),
            getUnitName ({ unitType, unitId }) {
                return unitType === 'USER'
                    ? (this.userList[unitId] ? this.userList[unitId].name : unitId)
                    : (this.departmentMap[unitId] ? this.departmentMap[unitId].name : unitId)
            },
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
            },
            handleGetPermissionUnits () {
                this.isLoading = true
                this.getPermissionUnits({
                    repoName: this.repoName
                }).then(({ user, department }) => {
                    this.permissionUnits = {
                        user,
                        department
                    }
                    this.pagination.count = user.length + department.length
                    this.handleGetDepartmentDetail(department.map(v => v.unitId))
                }).finally(() => {
                    this.isLoading = false
                })
            },
            handleGetDepartmentDetail (departmentIds) {
                const body = departmentIds.filter(id => !this.departmentMap[id])
                body.length && this.getRepoDepartmentDetail({
                    body
                }).then(list => {
                    list.forEach(department => {
                        this.$set(this.departmentMap, department.id, department)
                    })
                })
            },
            addPermissionUnit () {
                this.addDialog = {
                    loading: false,
                    show: true,
                    type: 'USER',
                    allowPush: false,
                    list: []
                }
            },
            initTree () {
                const treeTarget = this.$refs.departmentTree
                treeTarget.setData(this.departmentTree)
                this.permissionUnits.department.forEach(({ unitId: id }) => {
                    treeTarget.setChecked(id)
                    treeTarget.setDisabled(id)
                })
                this.addDialog.list.forEach(id => {
                    treeTarget.setChecked(id)
                })
            },
            async handleDepartmentTreeNode (node) {
                // 叶节点
                if (!node.data.has_children) return ({ data: [], leaf: [] })
                // 枝节点
                const res = await this.getRepoDepartmentList({
                    departmentId: node.id
                })
                
                res.forEach(department => {
                    this.$set(this.departmentMap, department.id, department)
                })

                this.$nextTick(() => {
                    let target = this.departmentTree
                    node.parents.forEach(parent => {
                        target = (target.children || target).find(v => v.id === parent.id).children
                    })
                    target = target.find(v => v.id === node.id)
                    target.children = res
                    if (this.$refs.departmentTree) {
                        this.initTree()
                        this.$refs.departmentTree.setExpanded([node.id])
                    }
                })
                return {
                    data: [],
                    leaf: res.filter(v => !v.has_children).map(w => w.id)
                }
            },
            changeAddDepartments (ids) {
                this.addDialog.list = ids.filter(id => !this.permissionUnits.department.find(exist => id === exist.unitId))
            },
            confirmAdd () {
                if (!this.addDialog.list.length) {
                    this.addDialog.show = false
                }

                this.addDialog.loading = true
                this.addPermissionUnits({
                    repoName: this.repoName,
                    unitType: this.addDialog.type,
                    push: this.addDialog.allowPush,
                    body: this.addDialog.list
                }).then(() => {
                    this.addDialog.show = false
                    this.handleGetPermissionUnits()
                    this.handlerPaginationChange()
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('add') + this.$t('success')
                    })
                }).finally(() => {
                    this.addDialog.loading = false
                })
            },
            changePushStatus ({ unitType, unitId, allowPush }) {
                this.editPermissionUnits({
                    repoName: this.repoName,
                    unitType,
                    push: allowPush,
                    body: [unitId]
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('edit') + this.$t('success')
                    })
                }).catch(err => {
                    this.handleGetPermissionUnits()
                    Promise.reject(err)
                })
            },
            selectionChange (rows) {
                this.deleteUnits = rows
            },
            deletePermissionUnit (list = this.deleteUnits) {
                this.isLoading = true
                this.deletePermissionUnits({
                    repoName: this.repoName,
                    body: list.reduce((target, unit) => {
                        target[unit.unitType.toLowerCase()].push(unit.unitId)
                        return target
                    }, { user: [], department: [] })
                }).then(() => {
                    this.handleGetPermissionUnits()
                    this.handlerPaginationChange()
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('delete') + this.$t('success')
                    })
                }).finally(() => {
                    this.isLoading = false
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
@import '@/scss/conf';
.permission-config-container {
    height: 100%;
    .permission-table {
        .devops-icon {
            font-size: 14px;
        }
    }
}
</style>
