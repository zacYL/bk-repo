<template>
    <div class="admin-container" v-bkloading="{ isLoading }">
        <div class="mb10 flex-align-center">
            <bk-button class="pl5 pr5" :theme="'primary'" @click="addAdmins()" icon="plus">
                {{ $t('add') }}
            </bk-button>
            <bk-input
                class="ml10 w220"
                v-model.trim="adminInput"
                clearable
                :placeholder="'账号/中文名'"
                @change="handlerPaginationChange()">
            </bk-input>
            <div v-show="deleteAdminList.length" class="flex-1 flex-end-center">
                <span class="hover-btn flex-align-center" @click="deleteAdmins()"><i class="mr5 devops-icon icon-delete"></i>批量删除</span>
            </div>
        </div>
        <bk-table
            class="admin-table"
            height="calc(100% - 84px)"
            :data="filterAdminList"
            :outer-border="false"
            :row-border="false"
            size="small"
            @selection-change="selectionChange">
            <bk-table-column type="selection" width="60"></bk-table-column>
            <bk-table-column :label="$t('account')" prop="userId" width="200"></bk-table-column>
            <bk-table-column :label="$t('chineseName')" prop="name"></bk-table-column>
            <bk-table-column :label="$t('operation')" width="100">
                <template #default="{ row }">
                    <div class="flex-align-center">
                        <i class="devops-icon icon-delete hover-btn" @click="deleteAdmins([row.userId])"></i>
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
            :selection-count="deleteAdminList.length"
            :current.sync="pagination.current"
            :limit="pagination.limit"
            :count="pagination.count"
            :limit-list="pagination.limitList"
            @change="current => handlerPaginationChange({ current })"
            @limit-change="limit => handlerPaginationChange({ limit })">
        </bk-pagination>
        <bk-dialog
            v-model="showAddDialog"
            title="添加管理员"
            :close-icon="false"
            :quick-close="false"
            :draggable="false">
            <bk-form :label-width="100">
                <bk-form-item label="管理员">
                    <bk-select
                        v-model="addAdminList"
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
            </bk-form>
            <template #footer>
                <bk-button :loading="loading" theme="primary" @click="confirm">{{$t('submit')}}</bk-button>
                <bk-button theme="default" @click="showAddDialog = false">{{$t('cancel')}}</bk-button>
            </template>
        </bk-dialog>
    </div>
</template>
<script>
    import { mapState, mapActions } from 'vuex'
    export default {
        name: 'admin',
        data () {
            return {
                isLoading: false,
                adminInput: '',
                adminList: [],
                pagination: {
                    count: 0,
                    current: 1,
                    limit: 20,
                    selectionCount: 0,
                    limitList: [10, 20, 40]
                },
                showAddDialog: false,
                loading: false,
                addAdminList: [],
                deleteAdminList: []
            }
        },
        computed: {
            ...mapState(['userList']),
            filterAdminList () {
                const { current, limit } = this.pagination
                return this.adminList.filter(admin => {
                    return ~admin.userId.indexOf(this.adminInput) || ~admin.name.indexOf(this.adminInput)
                }).slice((current - 1) * limit, current * limit)
            },
            filterUserList () {
                return Object.values(this.userList).filter(user => {
                    return !~this.adminList.findIndex(admin => admin.userId === user.id)
                })
            }
        },
        created () {
            this.getAdminListHandler()
        },
        methods: {
            ...mapActions([
                'getUserList',
                'setAdmins'
            ]),
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
            },
            getAdminListHandler () {
                this.isLoading = true
                this.getUserList({
                    admin: true,
                    current: 1,
                    limit: 1000
                }).then(({ records, totalRecords }) => {
                    this.adminList = records
                    this.pagination.count = totalRecords
                }).finally(() => {
                    this.isLoading = false
                })
            },
            addAdmins () {
                this.addAdminList = []
                this.showAddDialog = true
            },
            confirm () {
                this.loading = true
                this.setAdmins({
                    admin: true,
                    body: this.addAdminList
                }).then(() => {
                    this.showAddDialog = false
                    this.getAdminListHandler()
                    this.handlerPaginationChange()
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('add') + this.$t('success')
                    })
                }).finally(() => {
                    this.loading = false
                })
            },
            selectionChange (rows) {
                this.deleteAdminList = rows.map(user => user.userId)
            },
            deleteAdmins (list = this.deleteAdminList) {
                this.isLoading = true
                this.setAdmins({
                    admin: false,
                    body: list
                }).then(() => {
                    this.getAdminListHandler()
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
.admin-container {
    height: 100%;
    padding: 10px 20px;
    .admin-table {
        .icon-delete {
            font-size: 16px;
        }
    }
}
</style>
