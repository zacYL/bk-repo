<template>
    <div class="audit-container" v-bkloading="{ isLoading }">
        <div class="mb10 flex-align-center">
            <bk-date-picker
                v-model="query.time"
                :shortcuts="shortcuts"
                type="datetimerange"
                placeholder="操作日期时间范围">
            </bk-date-picker>
            <bk-select
                class="ml10 w140"
                v-model="query.user"
                searchable
                placeholder="操作用户"
                :enable-virtual-scroll="Object.values(userList).length > 3000"
                :list="Object.values(userList)">
                <bk-option
                    v-for="option in Object.values(userList)"
                    :key="option.id"
                    :id="option.id"
                    :name="option.name">
                </bk-option>
            </bk-select>
            <bk-button class="ml10 pl5 pr5" :theme="'primary'" @click="handlerPaginationChange()" icon="search">
                {{ $t('search') }}
            </bk-button>
        </div>
        <bk-table
            height="calc(100% - 84px)"
            :data="auditList"
            :outer-border="false"
            :row-border="false"
            size="small">
            <bk-table-column label="操作时间" width="200">
                <template #default="{ row }">
                    {{ formatDate(row.createdDate) }}
                </template>
            </bk-table-column>
            <bk-table-column label="操作用户" width="100">
                <template #default="{ row }">
                    {{ userList[row.userId] ? userList[row.userId].name : row.userId }}
                </template>
            </bk-table-column>
            <bk-table-column label="操作事件" width="150">
                <template #default="{ row }">
                    {{ row.operateType + row.resourceType }}
                </template>
            </bk-table-column>
            <bk-table-column label="操作对象">
                <template #default="{ row }">
                    <div class="flex-align-center">
                        <Icon class="mr5" v-if="row.content.repoType" :name="row.content.repoType.toLowerCase()" size="16"></Icon>
                        <span :class="row.content.repoType ? 'mr20' : 'mr5 repo-tag'" v-for="item in row.content.resKey.split('::').filter(Boolean)" :key="item">
                            {{ row.content.repoType ? item : (userList[item] ? userList[item].name : item) }}
                        </span>
                        <span>{{ row.content.des }}</span>
                    </div>
                </template>
            </bk-table-column>
            <bk-table-column label="客户端IP" prop="clientAddress" width="130"></bk-table-column>
            <bk-table-column label="结果" width="100">
                <template #default="{ row }">
                    <span class="repo-tag" :class="[row.result ? 'SUCCESS' : 'FAILED']">{{ row.result ? '成功' : '失败' }}</span>
                </template>
            </bk-table-column>
        </bk-table>
        <bk-pagination
            class="mt10"
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
    import { formatDate } from '@/utils'
    export default {
        name: 'audit',
        data () {
            return {
                isLoading: false,
                query: {
                    user: '',
                    time: []
                },
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
                        text: '近7天',
                        value () {
                            const end = new Date()
                            const start = new Date()
                            start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
                            return [start, end]
                        }
                    },
                    {
                        text: '近15天',
                        value () {
                            const end = new Date()
                            const start = new Date()
                            start.setTime(start.getTime() - 3600 * 1000 * 24 * 15)
                            return [start, end]
                        }
                    },
                    {
                        text: '近30天',
                        value () {
                            const end = new Date()
                            const start = new Date()
                            start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
                            return [start, end]
                        }
                    }
                ]
            }
        },
        computed: {
            ...mapState(['userList'])
        },
        created () {
            this.handlerPaginationChange()
        },
        methods: {
            formatDate,
            ...mapActions([
                'getAuditList'
            ]),
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getauditListHandler()
            },
            getauditListHandler () {
                this.isLoading = true
                const [startTime, endTime] = this.query.time
                this.getAuditList({
                    startTime: startTime instanceof Date ? startTime.toISOString() : undefined,
                    endTime: endTime instanceof Date ? endTime.toISOString() : undefined,
                    operator: this.query.user || undefined,
                    current: this.pagination.current,
                    limit: this.pagination.limit
                }).then(({ records, totalRecords }) => {
                    this.auditList = records
                    this.pagination.count = totalRecords
                }).finally(() => {
                    this.isLoading = false
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.audit-container {
    height: 100%;
    padding: 10px 20px;
    .SUCCESS {
        color: #2DCB56;
        background-color: #DCFFE2;
    }
    .FAILED {
        color: #EA3636;
        background-color: #FFDDDD;
    }
}
</style>
