<!--
 * @Date: 2025-01-09 15:22:35
 * @LastEditors: xiaoshan
 * @LastEditTime: 2025-01-23 14:50:22
 * @FilePath: /artifact/src/frontend/devops-repository-ci/src/views/repoCommon/components/repoLIstDialog.vue
-->
<template>
    <div>
        <canway-dialog
            v-model="isShow"
            :title="dialogTitle"
            width="600"
            height-num="563"
            @cancel="close">
            <template>
                <bk-input
                    class="w250"
                    v-model.trim="importantSearch"
                    :placeholder="$t('keyWordEnter')"
                    clearable
                    right-icon="bk-icon icon-search">
                </bk-input>
                <div class="content" v-if="computedDataList.length">
                    <div class="flex-align-center flex-1 content-item" style="cursor: pointer;" :class="selectData.name === item.name ? 'active' : ''" v-for="item in computedDataList" :key="item.id" @click="selectData = item">
                        <Icon size="16" :name="item.type.toLowerCase()" />
                        <span class="ml10 flex-1 text-overflow" :title="item.name">{{ item.name }}</span>
                    </div>
                </div>
                <span v-else class="flex-center mt40"> {{$t('noRepositoryData')}}</span>
            </template>
            <template #footer>
                <bk-button @click="close">{{ $t('cancel') }}</bk-button>
                <bk-button class="ml10" :loading="isLoading" theme="primary" :disabled="!selectData.name" @click="submit">{{ $t('confirm') }}</bk-button>
            </template>
        </canway-dialog>
    </div>
</template>
<script>
    export default {
        data () {
            return {
                row: {},
                importantSearch: '',
                selectData: {},
                dataList: [],
                srcRepoName: '',
                showNoDataInfo: true,
                isLoading: false,
                isShow: false,
                dialogTitle: '',
                operationType: ''
            }
        },
        computed: {
            computedDataList () {
                return this.dataList.filter(item => item.name.includes(this.importantSearch) && item.name !== this.srcRepoName && item.category === 'LOCAL')
            }
        },
        methods: {
            loading (loading = true) {
                this.$nextTick(() => {
                    this.isLoading = loading
                })
            },
            close () {
                // 重置data数据
                Object.assign(this.$data, this.$options.data())
            },
            open ({ row, operationType, dialogTitle, dataList, srcRepoName }) {
                this.row = row
                this.isShow = true
                this.operationType = operationType
                this.dialogTitle = dialogTitle
                this.dataList = dataList
                this.srcRepoName = srcRepoName
            },
            submit () {
                this.$emit('confirm', this.operationType, { ...this.selectData, version: this.row.name })
            }
        }
    }
</script>
<style lang="scss" scoped>
.content{
    max-height: 300px;
    overflow: auto;
    margin-top: 20px;
    .content-item{
        line-height: 40px;
        &:hover{
            background: #ebf2ff;
        }
        &.active{
            background: #f5f9ff;
        }
    }
}
</style>
