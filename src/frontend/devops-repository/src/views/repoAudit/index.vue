<template>
    <div class="audit-container" v-bkloading="{ isLoading }">
        <div class="ml20 mr20 mt10 flex-align-center">
            <bk-select
                v-model="query.projectId"
                class="mr10 w250"
                searchable
                :placeholder="$t('viewProjectLogPlaceholder')"
                @change="handlerPaginationChange()"
                :enable-virtual-scroll="projectList && projectList.length > 3000"
                :list="projectList">
                <bk-option v-for="option in projectList"
                    :key="option.id"
                    :id="option.id"
                    :name="option.name">
                </bk-option>
            </bk-select>
            <bk-date-picker
                v-model="query.time"
                class="mr10 w250"
                :shortcuts="shortcuts"
                type="daterange"
                :placeholder="$t('selectDatePlaceholder')"
                @change="handlerPaginationChange()">
            </bk-date-picker>
            <bk-select
                class="mr10 w250"
                v-model="query.user"
                clearable
                searchable
                :placeholder="$t('selectUserMsg')"
                @change="handlerPaginationChange()"
                :enable-virtual-scroll="Object.values(userList).length > 3000"
                :list="Object.values(userList).filter(user => user.id !== 'anonymous')">
                <bk-option v-for="option in Object.values(userList).filter(user => user.id !== 'anonymous')"
                    :key="option.id"
                    :id="option.id"
                    :name="option.name">
                </bk-option>
            </bk-select>
            <bk-select
                class="mr10 w250"
                v-model="query.eventTypes"
                clearable
                searchable
                multiple
                show-select-all
                :placeholder="$t('selectOperationEvent')"
                @change="handlerPaginationChange()"
                :list="logEventTypeList">
                <bk-option
                    v-for="(item, index) in logEventTypeList"
                    :key="index"
                    :id="item[0]"
                    :name="item[1]"
                >
                </bk-option>
            </bk-select>
        </div>
        <bk-table
            class="mt10"
            height="calc(100% - 100px)"
            :data="auditList"
            :outer-border="false"
            :row-border="false"
            size="small">
            <template #empty>
                <empty-data :is-loading="isLoading" :search="Boolean(isSearching)"></empty-data>
            </template>
            <bk-table-column :label="$t('project')" show-overflow-tooltip>
                <template #default="{ row }">{{ getProjectName(row.content.projectId) }}</template>
            </bk-table-column>
            <bk-table-column :label="$t('operatingTime')" width="150">
                <template #default="{ row }">{{ formatDate(row.createdDate) }}</template>
            </bk-table-column>
            <bk-table-column :label="$t('operator')" show-overflow-tooltip>
                <template #default="{ row }"> {{ userList[row.userId] ? userList[row.userId].name : row.userId }}</template>
            </bk-table-column>
            <bk-table-column :label="$t('event')" show-overflow-tooltip>
                <template #default="{ row }">{{ row.operate }}</template>
            </bk-table-column>
            <bk-table-column :label="$t('operationObject')" show-overflow-tooltip>
                <template #default="{ row }">
                    <Icon class="mr5 table-svg" v-if="row.content.repoType"
                        :name="row.content.repoType.toLowerCase()" size="16">
                    </Icon>
                    <span class="mr20" v-for="item in row.content.resKey.split('::').filter(Boolean)" :key="item">
                        {{ row.content.repoType ? item : (userList[item] ? userList[item].name : item) }}
                    </span>
                    <span>{{ row.content.des }}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('client') + $t('space') + 'IP'" prop="clientAddress" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('result')" width="110">
                <template #default="{ row }">
                    <span class="repo-tag" :class="[row.result ? 'SUCCESS' : 'FAILED']">{{ row.result ? $t('success') : $t('fail') }}</span>
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
    </div>
</template>
<script>
    import { mapState, mapActions } from 'vuex'
    import { formatDate } from '@repository/utils'
    const nowTime = new Date(
        `${new Date().getFullYear()}-${new Date().getMonth() + 1}-${new Date().getDate()}`
    ).getTime() + 3600 * 1000 * 24
    export default {
        name: 'audit',
        data () {
            return {
                isLoading: false,
                query: {
                    projectId: this.$route.params.projectId,
                    user: '',
                    time: [new Date(nowTime - 3600 * 1000 * 24 * 7), new Date(nowTime)],
                    eventTypes: []
                },
                logEventTypeList: [],
                auditList: [],
                pagination: {
                    count: 0,
                    current: 1,
                    limit: 20,
                    selectionCount: 0,
                    limitList: [10, 20, 40]
                },
                shortcuts: [
                    {
                        text: this.$t('lastSevenDays'),
                        value () {
                            return [new Date(nowTime - 3600 * 1000 * 24 * 7), new Date(nowTime)]
                        }
                    },
                    {
                        text: this.$t('lastFifteenDays'),
                        value () {
                            return [new Date(nowTime - 3600 * 1000 * 24 * 15), new Date(nowTime)]
                        }
                    },
                    {
                        text: this.$t('lastThirtyDays'),
                        value () {
                            return [new Date(nowTime - 3600 * 1000 * 24 * 30), new Date(nowTime)]
                        }
                    }
                ]
            }
        },
        computed: {
            ...mapState(['projectList', 'userList']),
            isSearching () {
                const { startTime, endTime, user } = this.$route.query
                return startTime || endTime || user
            }
        },
        created () {
            const { startTime, endTime, user, projectId } = this.$route.query
            startTime && endTime && (this.query.time = [new Date(startTime), new Date(endTime)])
            user && (this.query.user = user)
            projectId && (this.query.projectId = projectId)
            this.handlerPaginationChange()
        },
        methods: {
            formatDate,
            ...mapActions([
                'getAuditList',
                'getLogEventType'
            ]),
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                let [startTime, endTime] = this.query.time
                startTime = startTime instanceof Date ? startTime.toISOString() : undefined
                endTime = endTime instanceof Date ? endTime.toISOString() : undefined
                const query = {
                    projectId: this.query.projectId || undefined,
                    startTime,
                    endTime,
                    user: this.query.user || undefined,
                    eventType: this.query.eventTypes.join(',')
                }
                this.$router.replace({
                    query: {
                        ...this.$route.query,
                        ...query
                    }
                })
                this.getAuditListHandler(query)
            },
            getAuditListHandler (query) {
                this.isLoading = true
                this.getAuditList({
                    ...query,
                    current: this.pagination.current,
                    limit: this.pagination.limit
                }).then(({ records, totalRecords }) => {
                    this.auditList = records
                    this.pagination.count = totalRecords
                }).finally(() => {
                    this.isLoading = false
                })
                
                // 获取审计日志事件类型列表
                this.getLogEventType().then(data => {
                    this.logEventTypeList = Object.entries(data)
                })
            },
            getProjectName (id) {
                const project = this.projectList.find(project => project.id === id)
                return project ? project.name : '/'
            }
        }
    }
</script>
<style lang="scss" scoped>
.audit-container {
    height: 100%;
    background-color: white;
}
</style>
