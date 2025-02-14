<template>
    <bk-dialog
        v-model="config.isShow"
        header-position="left"
        theme="danger"
        :title="$t('deleteProject')"
        width="540"
        :cancel-text="$t('cancel')"
        :ok-text="$t('confirm')"
        :auto-close="false"
        @value-change="handleClickClearError"
        @confirm="handleClickConfirm"
    >
        <bk-alert type="error">
            <div slot="title">{{$t('deleteProjectInfo')}}</div>
        </bk-alert>
        <p>{{$t('deleteProjectConfirmContentInfo')}}<bk-tag theme="warning">{{ config.confirmName }}</bk-tag></p>
        <bk-form :model="form" :rules="rules" ref="validateForm" :label-width="0">
            <bk-form-item :property="'confirmName'" :error-display-type="'normal'">
                <bk-input :clearable="true" v-model="form.confirmName" :placeholder="$t('deleteProjectPlaceholder')" />
            </bk-form-item>
        </bk-form>
    </bk-dialog>
</template>

<script>
    export default {
        props: {
            config: {
                type: Object,
                default: () => {}
            }
        },
        data () {
            return {
                form: {
                    confirmName: ''
                },
                rules: {
                    confirmName: [
                        {
                            required: true,
                            message: this.$t('deleteProjectPlaceholder'),
                            trigger: 'blur'
                        },
                        {
                            validator: (val) => {
                                const { confirmName } = this.config
                                if (val === confirmName) {
                                    return true
                                }
                                return false
                            },
                            message: this.$t('deleteProjectErrorInfo'),
                            trigger: 'blur'
                        }
                    ]
                }
            }
        },
        methods: {
            handleClickConfirm () {
                this.$refs.validateForm.validate().then(validator => {
                    if (validator) {
                        this.$emit('confirm', this.form.confirmName)
                    }
                }).catch(err => {
                    console.log(err.message)
                })
            },
            // 清除表单错误提示
            handleClickClearError () {
                this.form.confirmName = ''
                this.$refs.validateForm.clearError()
            }
        }
    }
</script>

<style lang="scss" scoped>
::v-deep .bk-alert{
    padding: 0 16px;
    display: flex;
    align-items: center;
}
</style>
