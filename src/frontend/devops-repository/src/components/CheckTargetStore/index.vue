<template>
    <canway-dialog
        v-model="show"
        width="520"
        height-num="410"
        :title="title"
        @cancel="cancel">
        <div class="check-target-body">
            <bk-table
                ref="checkStoreTableRef"
                class="mt10"
                :data="storeList"
                height="calc(100% - 10px)"
                :outer-border="false"
                :row-border="false"
                size="small"
                v-bkloading="{ isLoading }"
                @scroll-end="onScrolled"
                @selection-change="onSelectChange">
                <template #empty>
                    <empty-data :is-loading="isLoading"></empty-data>
                </template>
                <bk-table-column type="selection" width="60"></bk-table-column>
                <bk-table-column :label="$t('repoName')" show-overflow-tooltip>
                    <template #default="{ row }">
                        <span>{{replaceRepoName(row.name)}}</span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('storeTypes')" width="120">
                    <template #default="{ row }">
                        <Icon class="mr5" :name="(row.category.toLowerCase() || 'local') + '-store'" size="16" />
                        <span>{{ $t((row.category.toLowerCase() || 'local') + 'Store')}}</span>
                    </template>
                </bk-table-column>
            </bk-table>
        </div>
        <template #footer>
            <bk-button @click="cancel">{{$t('cancel')}}</bk-button>
            <bk-button class="ml10" :loading="loading" theme="primary" @click="confirm">{{$t('confirm')}}</bk-button>
        </template>
    </canway-dialog>
</template>
<script>
    import { mapActions } from 'vuex'
    import { cloneDeep } from 'lodash'
    export default {
        name: 'checkTargetStore',
        components: { },
        props: {
            title: {
                type: String,
                default: '选择存储库'
            },
            // 当前选择的制品类型
            repoType: {
                type: String,
                default: ''
            },
            // 默认选中的数据
            checkList: {
                type: Array,
                default: () => []
            }
        },
        data () {
            return {
                show: false,
                isLoading: false,
                hasNext: true,
                storeList: [],
                pagination: {
                    count: 0,
                    current: 1,
                    limit: 10,
                    limitList: [10, 20, 40]
                },
                newCheckedList: [], // 表格中选中的数据
                tempCheckList: [], // 在加载下一页时临时存储之前页面选中的数据
                currentPageList: [] // 当前分页的接口返回的数据
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId || ''
            }
        },
        watch: {
            show (val) {
                if (val) {
                    this.handlerPaginationChange()
                    // this.setCheckData()
                } else {
                    this.storeList = []
                }
            }
        },
        methods: {
            ...mapActions([
                'getRepoList'
            ]),
            // 滚动条划到底部的方法
            onScrolled () {
                this.hasNext && this.handlerPaginationChange({ current: this.pagination.current + 1 })
            },
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}, load) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getStoreList()
            },
            // 获取列表数据
            getStoreList () {
                // 在获取下一页数据之前需要先将之前选中的数据临时存储起来
                this.tempCheckList = cloneDeep(this.newCheckedList)
                this.isLoading = true
                this.getRepoList({
                    projectId: this.projectId,
                    current: this.pagination.current,
                    limit: this.pagination.limit,
                    type: this.repoType,
                    category: 'LOCAL,REMOTE'
                }).then((res) => {
                    this.currentPageList = res?.records || []
                    this.storeList = this.storeList.concat(res.records)
                    if (res.records.length < this.pagination.limit) {
                        this.hasNext = false
                    } else {
                        this.hasNext = true
                    }
                    // 因为是滚动分页，所以每次重新加载数据之后都要重新设置选中状态
                    this.setCheckData()
                }).finally(() => {
                    this.isLoading = false
                })
            },
            onSelectChange (selection) {
                this.newCheckedList = selection
            },
            // 打开弹窗时设置初始化选中的数据
            setCheckData () {
                this.$nextTick(() => {
                    if (this.checkList.length > 0) {
                        // 获取当前页的默认选中数据
                        const selected = this.currentPageList.filter(store => this.checkList.map(item => item.name).includes(store.name))
                        // 获取之前页面的选中数据
                        const selectedTwo = this.storeList.filter(store => this.tempCheckList.map(item => item.name).includes(store.name))
                        // 获取当前表格全部选中的数据
                        const selectList = selected.concat(selectedTwo)
                        selectList.forEach(item => {
                            this.$refs.checkStoreTableRef && this.$refs.checkStoreTableRef.toggleRowSelection(item, true)
                        })
                    }
                })
            },
            cancel () {
                this.show = false
            },
            confirm () {
                this.$emit('checkedTarget', this.newCheckedList)
                this.show = false
            }
        }
    }
</script>
<style lang="scss" scoped>

</style>
