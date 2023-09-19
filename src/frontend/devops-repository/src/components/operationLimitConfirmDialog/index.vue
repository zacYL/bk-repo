<template>
    <canway-dialog
        v-model="dialogData.show"
        width="380"
        height-num="221"
        :title="title"
        @cancel="cancel">
        <div>
            <div v-if="!dialogData.limitStatus" class="flex-align-center">
                <span>{{$t(`${dialogData.limitType }ReasonInfo`)}}</span>
                <bk-input
                    class="ml10 w250"
                    v-model.trim="dialogData.limitReason"
                    maxlength="30"
                    show-word-limit
                    :placeholder="$t('pleaseInput') + $t('space') + $t(`${dialogData.limitType}ReasonInfo`)"
                    clearable>
                </bk-input>
            </div>
            <div :class="dialogData.limitStatus ? 'flex-center' : 'mt30'">{{dialogData.message}}</div>
        </div>
        <template #footer>
            <bk-button @click="cancel">{{$t('cancel')}}</bk-button>
            <bk-button class="ml10" :loading="dialogData.loading" :theme="dialogData.theme || 'primary'" @click="confirm">{{$t('confirm')}}</bk-button>
        </template>
    </canway-dialog>
</template>
<script>
   
    export default {
        name: 'operationLimitConfirmDialog',
        props: {
            title: {
                type: String,
                default () {
                    return this.$t('operationConfirmation')
                }
            }
        },
        data () {
            return {
                dialogData: {
                    show: false,
                    loading: false,
                    limitReason: '',
                    limitType: '',
                    theme: '',
                    message: '',
                    limitStatus: false,
                    name: ''
                }
            }
        },
        methods: {
            setData (data) {
                this.dialogData = {
                    ...this.dialogData,
                    ...data
                }
            },
            confirm () {
                this.dialogData.loading = true
                this.$emit('confirm', this.dialogData)
            },
            cancel () {
                this.dialogData.loading = false
                this.dialogData.show = false
            }
        }
    }
</script>
<style lang="scss" scoped>
</style>
