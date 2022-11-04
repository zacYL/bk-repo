<template>
    <bk-dialog
        v-model="config.isShow"
        header-position="left"
        theme="danger"
        title="项目删除"
        width="540"
        ok-text="确认"
        :auto-close="false"
        @value-change="handleClickClearError"
        @confirm="handleClickConfirm"
    >
        <bk-alert type="error">
            <div slot="title">项目一旦被永久删除，就无法恢复，删除项目将立即删除其存储库和所有相关资源。</div>
        </bk-alert>
        <p>请输入以下内容进行确认：<bk-tag theme="warning">{{ config.confirmName }}</bk-tag></p>
        <bk-form :model="form" :rules="rules" ref="validateForm" :label-width="0">
            <bk-form-item :property="'confirmName'" :error-display-type="'normal'">
                <bk-input :clearable="true" v-model="form.confirmName" placeholder="请输入要删除的项目名称" />
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
                            message: '请输入要删除的项目名称',
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
                            message: '项目名称不匹配',
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

<style lang="scss" scoped></style>
