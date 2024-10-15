<template>
    <div class="plan-container" v-bkloading="{ isLoading }">
        <div class="ml20 mr20 mt10" :class="createPlanPermission ? 'flex-between-center' : 'flex-end-center' ">
            <bk-button v-if="createPlanPermission" icon="plus" theme="primary" @click="handleClickCreatePlan">{{ $t('create') }}</bk-button>
            <div class="flex-align-center">
                <bk-input
                    class="w250"
                    v-model.trim="planInput"
                    clearable
                    :placeholder="$t('planPlaceholder')"
                    right-icon="bk-icon icon-search"
                    @enter="handlerPaginationChange()"
                    @clear="handlerPaginationChange()">
                </bk-input>
                <bk-select
                    class="ml10 w250"
                    v-model="lastExecutionStatus"
                    :placeholder="$t('lastExecutionStatus')"
                    @change="handlerPaginationChange()">
                    <bk-option v-for="(label, key) in asyncPlanStatusEnum" :key="key" :id="key" :name="$t(`asyncPlanStatusEnum.${key}`)"></bk-option>
                </bk-select>
                <bk-select
                    class="ml10 w250"
                    v-model="showEnabled"
                    :placeholder="$t('planStatus')"
                    @change="handlerPaginationChange()">
                    <bk-option id="true" :name="$t('activePlan')"></bk-option>
                    <bk-option id="false" :name="$t('discontinuedPlan')"></bk-option>
                </bk-select>
            </div>
        </div>
        <bk-table
            class="mt10 plan-table"
            height="calc(100% - 100px)"
            :data="planList"
            :outer-border="false"
            :row-border="false"
            row-key="userId"
            size="small"
            @sort-change="handleSortChange">
            <template #empty>
                <empty-data :is-loading="isLoading" :search="Boolean(planInput || lastExecutionStatus || showEnabled)"></empty-data>
            </template>
            <bk-table-column :label="$t('planName')" show-overflow-tooltip>
                <template #default="{ row }">
                    <span class="hover-btn" @click="showPlanDetailHandler(row)">{{row.name}}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('targetNode')" show-overflow-tooltip>
                <template #default="{ row }">{{ row.remoteClusters.map(v => v.name).join('、') }}</template>
            </bk-table-column>
            <bk-table-column :label="$t('syncType')" width="120" show-overflow-tooltip>
                <template #default="{ row }">
                    {{ { 'REPOSITORY': $t('synchronizeRepository'), 'PACKAGE': $t('synchronizePackage'), 'PATH': $t('synchronizePath') }[row.replicaObjectType] }}
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('synchronizationPolicy')" width="120" show-overflow-tooltip>
                <template #default="{ row }">{{ getExecutionStrategy(row) }}</template>
            </bk-table-column>
            <bk-table-column :label="$t('lastExecutionTime')" prop="LAST_EXECUTION_TIME" width="170" sortable="custom">
                <template #default="{ row }">{{formatDate(row.lastExecutionTime)}}</template>
            </bk-table-column>
            <bk-table-column :label="$t('lastExecutionStatus')" width="110">
                <template #default="{ row }">
                    <span v-if="row.lastExecutionStatus === 'RUNNING' && row.artifactCount">
                        {{row.currentProgress}}/{{row.artifactCount}}
                    </span>
                    <span v-else class="repo-tag" :class="row.lastExecutionStatus">
                        {{row.lastExecutionStatus ? $t(`asyncPlanStatusEnum.${row.lastExecutionStatus}`) : $t('notExecuted')}}
                    </span>
                </template>
            </bk-table-column>
            <!-- <bk-table-column label="下次执行时间" prop="NEXT_EXECUTION_TIME" width="150" :render-header="renderHeader">
                <template #default="{ row }">{{formatDate(row.nextExecutionTime)}}</template>
            </bk-table-column>
            <bk-table-column :label="$t('creator')" width="90" show-overflow-tooltip>
                <template #default="{ row }">{{userList[row.createdBy] ? userList[row.createdBy].name : row.createdBy}}</template>
            </bk-table-column>
            <bk-table-column :label="$t('createdDate')" prop="CREATED_TIME" width="150" :render-header="renderHeader">
                <template #default="{ row }">{{formatDate(row.createdDate)}}</template>
            </bk-table-column> -->
            <bk-table-column v-if="enablePlanPermission" :render-header="renderHeader" :label="$t('enablePlan')" width="100">
                <template #default="{ row }">
                    <bk-switcher class="m5" v-model="row.enabled" size="small" theme="primary" @change="changeEnabledHandler(row)"></bk-switcher>
                </template>
            </bk-table-column>
            <bk-table-column v-if="executePlanPermission" :label="$t('execute')" width="80">
                <template #default="{ row }">
                    <i class="devops-icon icon-play3 hover-btn inline-block"
                        :class="{ 'disabled': row.lastExecutionStatus === 'RUNNING' }"
                        @click.stop="executePlanHandler(row)">
                    </i>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('operation')" width="100">
                <template #default="{ row }">
                    <operation-list
                        :list="[
                            updatePlanPermission && { label: $t('edit'), clickEvent: () => editPlanHandler(row), disabled: Boolean(row.lastExecutionStatus) },
                            copyPlanPermission && { label: $t('copyPlan'), clickEvent: () => copyPlanHandler(row) },
                            copyPlanPermission && { label: $t('copyUrl'), clickEvent: () => copyUrlHandler(row) },
                            deletePlanPermission && { label: $t('delete'), clickEvent: () => deletePlanHandler(row), disabled: Boolean(row.enabled) },
                            { label: $t('log'), clickEvent: () => showPlanLogHandler(row) }
                        ]"></operation-list>
                </template>
            </bk-table-column>
        </bk-table>

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
        <plan-log v-model="planLog.show" :plan-data="planLog.planData"></plan-log>
        <plan-copy-dialog v-bind="planCopy" @cancel="planCopy.show = false" @refresh="handlerPaginationChange()"></plan-copy-dialog>
        <bk-sideslider :is-show.sync="drawerSlider.isShow" :quick-close="true" :width="currentLanguage === 'zh-cn' ? 724 : 992">
            <div slot="header">{{ drawerSlider.title }}</div>
            <div slot="content" class="plan-side-content">
                <create-plan :rows-data="drawerSlider.rowsData" @close="handleClickCloseDrawer" @confirm="handlerPaginationChange" />
            </div>
        </bk-sideslider>
    </div>
</template>
<script>
    import OperationList from '@repository/components/OperationList'
    import planLog from './planLog'
    import planCopyDialog from './planCopyDialog'
    import { mapState, mapActions } from 'vuex'
    import { formatDate, copyToClipboard } from '@repository/utils'
    import { asyncPlanStatusEnum } from '@repository/store/publicEnum'
    import createPlan from '@repository/views/planManage/createPlan'
    export default {
        name: 'plan',
        components: { planLog, planCopyDialog, OperationList, createPlan },
        data () {
            return {
                ciMode: MODE_CONFIG === 'ci',
                drawerSlider: {
                    isShow: false,
                    title: '',
                    rowsData: {}
                },
                asyncPlanStatusEnum,
                isLoading: false,
                showEnabled: undefined,
                lastExecutionStatus: '',
                planInput: '',
                sortType: 'CREATED_TIME',
                sortDirection: 'DESC',
                planList: [],
                pagination: {
                    count: 0,
                    current: 1,
                    limit: 20,
                    limitList: [10, 20, 40]
                },
                planLog: {
                    show: false,
                    planData: {}
                },
                planCopy: {
                    show: false,
                    name: '',
                    planKey: '',
                    description: ''
                }
            }
        },
        computed: {
            ...mapState(['userList', 'operationPermission']),
            projectId () {
                return this.$route.params.projectId
            },
            // 获取制品分发的全部操作权限
            planOperationPermission () {
                return this.operationPermission?.find((item) => item.resourceCode === 'v_pack_replica')?.actionCodes || []
            },
            // 是否有创建权限
            createPlanPermission () {
                return !this.ciMode ? true : this.planOperationPermission?.includes('create')
            },
            // 是否有编辑权限
            updatePlanPermission () {
                return !this.ciMode ? true : this.planOperationPermission?.includes('create')
            },
            // 是否有启用分发计划的权限
            enablePlanPermission () {
                return !this.ciMode ? true : this.planOperationPermission?.includes('create')
            },
            // 是否有执行分发计划权限
            executePlanPermission () {
                return !this.ciMode ? true : this.planOperationPermission?.includes('create')
            },
            // 是否有复制计划及复制URL权限
            copyPlanPermission () {
                return !this.ciMode ? true : this.planOperationPermission?.includes('create')
            },
            // 是否有删除计划权限
            deletePlanPermission () {
                return !this.ciMode ? true : this.planOperationPermission?.includes('delete')
            }
        },
        created () {
            this.handlerPaginationChange()
        },
        methods: {
            formatDate,
            ...mapActions([
                'getPlanList',
                'changeEnabled',
                'executePlan',
                'deletePlan',
                'getPlanOperationPermission'
            ]),
            renderHeader (h, data) {
                const directive = {
                    name: 'bkTooltips',
                    content: '启动计划后，将无法删除计划',
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
                    this.$t('enablePlan') // 子节点（文本）
                )
            },
            getExecutionStrategy ({ replicaType, setting: { executionStrategy } }) {
                return replicaType === 'REAL_TIME'
                    ? this.$t('realTimeSync')
                    : {
                        IMMEDIATELY: this.$t('executeImmediately'),
                        SPECIFIED_TIME: this.$t('designatedTime'),
                        CRON_EXPRESSION: this.$t('timedExecution')
                    }[executionStrategy]
            },
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getPlanListHandler()
            },
            getPlanListHandler () {
                this.isLoading = true
                return this.getPlanList({
                    projectId: this.$route.params.projectId,
                    name: this.planInput || undefined,
                    enabled: this.showEnabled || undefined,
                    lastExecutionStatus: this.lastExecutionStatus || undefined,
                    sortType: this.sortType,
                    sortDirection: this.sortDirection,
                    current: this.pagination.current,
                    limit: this.pagination.limit
                }).then(({ records, totalRecords }) => {
                    this.planList = records
                    this.pagination.count = totalRecords
                }).finally(() => {
                    this.isLoading = false
                })
            },
            executePlanHandler ({ key, name, lastExecutionStatus }) {
                if (lastExecutionStatus === 'RUNNING') return
                this.$confirm({
                    theme: 'warning',
                    message: this.$t('planConfirmExecuteMsg', [name]),
                    confirmFn: () => {
                        return this.executePlan({
                            projectId: this.projectId,
                            key
                        }).then(() => {
                            this.getPlanListHandler()
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('executePlan') + this.$t('space') + this.$t('success')
                            })
                        })
                    }
                })
            },
            handleClickCloseDrawer () {
                this.drawerSlider.isShow = false
            },
            handleClickCreatePlan () {
                this.drawerSlider = {
                    isShow: true,
                    title: this.$t('createPlan'),
                    rowsData: {
                        ...this.$route.params,
                        routeName: 'createPlan'
                    }
                }
            },
            handleSortChange ({ prop, order }) {
                this.sortType = order ? prop : 'CREATED_TIME'
                this.sortDirection = order === 'ascending' ? 'ASC' : 'DESC'
                this.getPlanListHandler()
            },
            editPlanHandler ({ name, key, lastExecutionStatus }) {
                if (lastExecutionStatus) return
                this.drawerSlider = {
                    isShow: true,
                    title: this.$t('editPlan'),
                    rowsData: {
                        ...this.$route.params,
                        planId: key,
                        planName: name,
                        routeName: 'editPlan'
                    }
                }
                // this.$router.push({
                //     name: 'editPlan',
                //     params: {
                //         ...this.$route.params,
                //         planId: key
                //     },
                //     query: {
                //         planName: name
                //     }
                // })
            },
            copyPlanHandler ({ name, key, description }) {
                this.planCopy = {
                    show: true,
                    name,
                    planKey: key,
                    description
                }
            },
            deletePlanHandler ({ key, name }) {
                this.$confirm({
                    theme: 'danger',
                    message: this.$t('planConfirmDeleteMsg', [name]),
                    confirmFn: () => {
                        return this.deletePlan({
                            projectId: this.projectId,
                            key
                        }).then(() => {
                            this.handlerPaginationChange()
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('deletePlan') + this.$t('space') + this.$t('success')
                            })
                        })
                    }
                })
            },
            copyUrlHandler ({ key }) {
                const url = window.location.origin + `/replication/api/task/execute/${this.projectId}/${key}`
                copyToClipboard(url).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('copyUrlSuccessTip')
                    })
                }).catch(() => {
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('copy') + this.$t('space') + this.$t('fail')
                    })
                })
            },
            changeEnabledHandler ({ key, enabled }) {
                this.changeEnabled({
                    projectId: this.projectId,
                    key
                }).then(res => {
                    this.$bkMessage({
                        theme: 'success',
                        message: `${enabled ? this.$t('enablePlanSuccess') : this.$t('stopPlanSuccess')}`
                    })
                }).finally(() => {
                    this.getPlanListHandler()
                })
            },
            showPlanDetailHandler ({ name, key }) {
                this.drawerSlider = {
                    isShow: true,
                    title: `${name}` + this.$t('space') + this.$t('detail'),
                    rowsData: {
                        ...this.$route.params,
                        planId: key,
                        routeName: 'planDetail'
                    }
                }
                // this.$router.push({
                //     name: 'planDetail',
                //     params: {
                //         ...this.$route.params,
                //         planId: key
                //     }
                // })
            },
            showPlanLogHandler (row) {
                this.planLog.show = true
                this.planLog.planData = row
            }
        }
    }
</script>
<style lang="scss" scoped>
.plan-container {
    height: 100%;
    overflow: hidden;
    background-color: white;
    .plan-table {
        ::v-deep .selected-header {
            color: var(--primaryColor);
        }
    }
}
.plan-side-content{
    height: 100%;
}
</style>
