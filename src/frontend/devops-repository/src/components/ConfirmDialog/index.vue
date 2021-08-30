<template>
    <canway-dialog
        v-model="show"
        width="480"
        title="操作确认"
        @cancel="cancel">
        <div class="confirm-body flex-center">
            <i :class="`devops-icon icon-${getIcon()}`"></i>
            <span class="ml10">{{ message }}</span>
        </div>
        <template #footer>
            <bk-button @click="cancel">{{$t('cancel')}}</bk-button>
            <bk-button class="ml10" :loading="loading" :theme="theme" @click="confirm">{{$t('confirm')}}</bk-button>
        </template>
    </canway-dialog>
</template>
<script>
    import Vue from 'vue'
    export default {
        name: 'confirmDialog',
        data () {
            return {
                show: false,
                loading: false,
                theme: 'warning',
                message: '',
                confirmFn: () => {}
            }
        },
        mounted () {
            Vue.prototype.$confirm = this.showConfiirmDialog
        },
        methods: {
            getIcon () {
                switch (this.theme) {
                    case 'success':
                        return 'check-1'
                    case 'warning':
                        return 'exclamation'
                    case 'danger':
                        return 'close'
                }
            },
            showConfiirmDialog ({ theme, message, confirmFn }) {
                this.show = true
                this.loading = false
                this.theme = theme
                this.message = message
                this.confirmFn = confirmFn
            },
            confirm () {
                this.loading = true
                const res = this.confirmFn()
                if (res instanceof Promise) {
                    res.then(this.cancel)
                } else {
                    this.cancel()
                }
            },
            cancel () {
                this.loading = false
                this.show = false
            }
        }
    }
</script>
<style lang="scss" scoped>
.confirm-body {
    height: 120px;
    .devops-icon {
        width: 38px;
        height: 38px;
        color: #fff;
        border-radius: 50%;
    }
    .icon-exclamation {
        padding: 9px;
        font-size: 20px;
        background-color: #ffb848;
    }
    .icon-close {
        padding: 11px;
        font-size: 16px;
        background-color: #F16965;
    }
}
</style>
