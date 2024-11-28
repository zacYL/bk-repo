<!--
 * @Date: 2024-11-21 14:16:15
 * @LastEditors: xiaoshan
 * @LastEditTime: 2024-11-21 15:04:37
 * @FilePath: /artifact/src/frontend/devops-repository/src/views/repoScan/scanMain/components/components/AddBlackWhiteRepoDialog.vue
-->
<template>
    <bk-dialog
        v-model="visible"
        :title="title"
        header-position="left"
        width="380"
        ext-cls="add-black-white-repo-dialog"
        @cancel="cancel">
        <bk-form
            class="mr10" :label-width="135"
            form-type="vertical" :model="form" :rules="rules" ref="Form">
            <bk-form-item label="" :required="true" property="name" error-display-type="normal">
                <bk-input type="text" :placeholder="$t('artifactNamePlaceholder')" v-model.trim="form.name" maxlength="32" show-word-limit></bk-input>
            </bk-form-item>
        </bk-form>
        <template #footer>
            <bk-button theme="default" @click="cancel">{{$t('cancel')}}</bk-button>
            <bk-button class="ml10" :loading="btnLoading" theme="primary" @click="submit">{{$t('confirm')}}</bk-button>
        </template>
    </bk-dialog>
</template>
<script>
    import { cloneDeep } from 'lodash'
    const baseForm = {
        name: ''
    }
    export default {
        name: 'createScan',
        props: {
            visible: Boolean,
            title: String
        },
        data () {
            return {
                form: cloneDeep(baseForm),
                rules: {
                    name: [
                        {
                            required: true,
                            message: this.$t('artifactNamePlaceholder'),
                            trigger: 'blur'
                        }
                    ]
                },
                btnLoading: false
            }
        },
        watch: {
            visible (val) {
                if (val) {
                    this.form = cloneDeep(baseForm)
                }
            }
        },
        methods: {
            cancel () {
                this.$emit('cancel')
            },
            submit () {
                this.btnLoading = true
                const cb = () => {
                    this.btnLoading = false
                }
                this.$refs.Form.validate().then(() => {
                    this.$emit('submit', this.form, cb)
                }).catch(() => {
                    cb()
                })
            }
        }
    }
</script>
<style lang="scss">
.add-black-white-repo-dialog {
    .bk-dialog-body {
        min-height: unset !important;
    }
}
</style>
