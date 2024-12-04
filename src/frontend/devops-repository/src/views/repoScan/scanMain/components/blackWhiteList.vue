<template>
    <div class="bw-container" v-bkloading="{ isLoading }">
        <div class="ml20 mr20 mt10 flex-between-center">
            <DefaultTabBox
                :tabs="tabList"
                id-key="name"
                label-key="label"
                :current-tab="currentTab"
                @tabChang="tabChang" />
            <div class="flex-align-center">
                <bk-button theme="primary" class="mr10" @click="addBlackWhiteRepo">{{$t('add')}}</bk-button>
                <FilterCondition :with-params="!!(Object.keys(getRealParams).length)" @confirm="search" @reset="reset" />
            </div>
        </div>
        <bk-table
            class="mt10 scan-table"
            height="calc(100% - 100px)"
            :data="blackWhiteList"
            :outer-border="false"
            :row-border="false"
            row-key="id"
            size="small">
            <template #empty>
                <empty-data :is-loading="isLoading" :search="Boolean(scanName)"></empty-data>
            </template>
            <!-- 制品包名称 -->
            <bk-table-column :label="$t('composerInputLabel')" show-overflow-tooltip prop="key">
                <template #default="{ row }">
                    <span class="hover-btn">{{row.key}}</span>
                </template>
            </bk-table-column>
            <!-- 版本 -->
            <bk-table-column :label="$t('version')" prop="version">
            </bk-table-column>
            <!-- 仓库类型 -->
            <bk-table-column :label="$t('storeTypes')" prop="packageType">
            </bk-table-column>
            <!-- 有效时间 -->
            <bk-table-column v-if="type === 'white'" :label="$t('validTime')" prop="expireDate">
                <template #default="{ row }">{{formatDate(row.expireDate)}}</template>
            </bk-table-column>
            <!-- 操作 -->
            <bk-table-column :label="$t('operation')">
                <template #default="{ row }">
                    <bk-button theme="primary" text @click="removeBlackWhiteList(row)">{{$t('delete')}}</bk-button>
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
        <AddBlackWhiteRepoDialog :title="addConfig.title" :visible="addConfig.visible" @cancel="hideBlackWhiteRepo" @submit="addBlackWhiteRepoSubmit" />
    </div>
</template>
<script>
    import { cloneDeep } from 'lodash'
    import { mapActions } from 'vuex'
    import DefaultTabBox from '@repository/components/DefaultTabBox'
    import FilterCondition from './components/FilterCondition.vue'
    import AddBlackWhiteRepoDialog from './components/AddBlackWhiteRepoDialog.vue'
    import { formatDate } from '@repository/utils'
    const paginationParams = {
        count: 0,
        current: 1,
        limit: 20,
        limitList: [10, 20, 40]
    }
    export default {
        name: 'black-white-list',
        components: {
            DefaultTabBox,
            FilterCondition,
            AddBlackWhiteRepoDialog
        },
        props: {
            projectId: String
        },
        data () {
            return {
                isLoading: false,
                tabList: [
                    {
                        name: 'white',
                        label: this.$t('whiteList')
                    },
                    {
                        name: 'black',
                        label: this.$t('blackList')
                    }
                ],
                type: 'white',
                pagination: cloneDeep(paginationParams),
                filterParams: null,

                addConfig: {
                    title: '',
                    visible: false
                },
                selectParams: {}
            }
        },
        computed: {
            currentTab () {
                return this.tabList.find(tab => tab.name === this.type)
            },
            // 必选查询条件
            defaultFilterParams () {
                return {
                    field: 'projectId',
                    value: this.projectId,
                    operation: 'EQ'
                }
            },
            getRealParams () {
                const OBj = {}
                Object.keys(this.selectParams).forEach(key => {
                    if (this.selectParams[key] !== '') {
                        OBj[key] = this.selectParams[key]
                    }
                })
                return OBj
            },
            listParams () {
                return {
                    projectId: this.projectId,
                    pageNumber: this.pagination.current,
                    pageSize: this.pagination.limit,
                    pass: this.type === 'white',
                    ...Object.keys(this.getRealParams).length ? this.getRealParams : {}
                }
            }
        },
        created () {
            this.handlerPaginationChange({ current: 1, limit: 20 })
        },
        methods: {
            formatDate,
            ...mapActions([
                'createBlackWhiteList',
                'deleteBlackWhiteList',
                'getBlackWhiteRecords'
            ]),
            addBlackWhiteRepo () {
                this.addConfig.visible = true
                this.addConfig.title = this.type === 'white' ? (this.$t('add') + this.$t('whiteList')) : (this.$t('add') + this.$t('blackList'))
            },
            hideBlackWhiteRepo () {
                this.addConfig.visible = false
            },
            addBlackWhiteRepoSubmit (form, cb) {
                this.createBlackWhiteList({
                    body: {
                        packageType: form.repoType.toLocaleUpperCase(),
                        projectId: this.projectId,
                        key: ['maven', 'gradle'].includes(form.repoType) ? (form.groupID + ':' + form.name) : form.name,
                        pass: this.type === 'white',
                        version: form.version,
                        versionRuleType: form.operator
                    }
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('addSuccess')
                    })
                    this.getBlackWhiteList({ current: 1 })
                    this.hideBlackWhiteRepo()
                }).catch(() => {
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('addFail')
                    })
                }).finally(() => {
                    cb && cb()
                })
            },
            // 分页
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getBlackWhiteList()
            },
            // 获取黑白名单列表
            getBlackWhiteList () {
                this.isLoading = true
                
                return this.getBlackWhiteRecords(
                    this.listParams
                ).then(res => {
                    this.blackWhiteList = res.records
                    this.pagination.count = res.totalRecords
                }).finally(() => {
                    this.isLoading = false
                })
            },
            // 搜索
            search (selectParams, cb) {
                this.selectParams = selectParams
                this.$nextTick(() => {
                    this.getBlackWhiteList({ current: 1 }).then(() => {
                        cb && cb()
                    })
                })
            },
            reset (cb) {
                this.selectParams = {}
                this.$nextTick(() => {
                    this.getBlackWhiteList({ current: 1 }).then(() => {
                        cb && cb()
                    })
                })
            },
            tabChang (val) {
                this.type = val.name
                this.$nextTick(() => {
                    this.getBlackWhiteList({ current: 1 })
                })
            },
            removeBlackWhiteList (row) {
                this.$bkInfoDevopsConfirm({
                    subTitle: this.$t('deleteBlackWhiteTips', [this.active === 'white' ? this.$t('whiteList') : this.$t('blackList')]),
                    theme: 'danger',
                    confirmFn: () => {
                        const body = {
                            packageType: row.packageType,
                            projectId: this.projectId,
                            key: row.key,
                            version: row.version,
                            versionRuleType: row.versionRuleType,
                            pass: this.type === 'white'
                        }
                        this.deleteBlackWhiteList({ body }).then(() => {
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('removeSuccess')
                            })
                            let current = 0
                            if (this.blackWhiteList.length === 1) {
                                if (this.pagination.current === 1) {
                                    current = 1
                                } else {
                                    current = this.pagination.current - 1
                                }
                            } else {
                                current = this.pagination.current
                            }
                            this.getBlackWhiteList({ current: current })
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
.bw-container {
    height: 100%;
    width: 100%;
    background-color: white;
}
</style>
