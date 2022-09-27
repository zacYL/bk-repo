<template>
    <canway-dialog
        v-model="value"
        title="编辑制品白名单"
        :height-num="400"
        @cancel="handleClickCancel"
    >
        <bk-form class="mr10" :label-width="90">
            <bk-form-item label="制品类型">
                <bk-select
                    class="w250"
                    v-model="form.artifactType"
                    placeholder="请选择制品类型"
                    :clearable="false">
                    <bk-option v-for="item in artifactTypeOptions" :key="item.type" :id="item.type" :name="item.name"></bk-option>
                </bk-select>
            </bk-form-item>
        </bk-form>
        <template #footer>
            <bk-button theme="default" @click="handleClickCancel">{{$t('cancel')}}</bk-button>
            <bk-button theme="primary" @click="handleClickConfirm">{{$t('confirm')}}</bk-button>
        </template>
    </canway-dialog>
</template>

<script>
    export default {
        props: {

        },
        data () {
            return {
                form: {
                    artifactType: ''
                },
                artifactTypeOptions: [
                    {
                        name: 'Maven',
                        type: 'Maven'
                    }
                ]
            }
        },
        methods: {
            // 关闭弹框
            handleClickCancel () {
                this.$emit('close')
            },
            // 提交弹框
            handleClickConfirm () {
                this.$$emit('confirm')
            }
        }
    }
</script>

<style lang="scss" scoped></style>
