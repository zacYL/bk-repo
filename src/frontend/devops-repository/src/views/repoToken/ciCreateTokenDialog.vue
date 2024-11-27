<template>
    <canway-dialog
        v-model="show"
        width="560"
        height-num="270"
        :title="$t('generateCiToken')"
        @cancel="cancel">
        <div v-if="token" class="flex-align-center">
            <i class="flex-center devops-icon icon-check-1"></i>
            <div style="max-width: 450px;">
                <span class="token-title">{{ $t('generateCiToken') + $t('space') + $t('success') }}</span>
                <div @click="copyToken(token)" class="mt10 mb10 hover-btn flex-align-center">
                    <span class="token-value">
                        {{ $t('tokenIs') + $t('space') + token }}
                    </span>
                    <i class="ml10 devops-icon icon-clipboard"></i>
                </div>
                <span class="token-tip">{{ $t('tokenCopyTip') }}</span>
            </div>
        </div>
        <bk-form v-else :label-width="92" :model="tokenFormData" :rules="rules" ref="tokenForm">
            <bk-form-item :label="$t('name')" required property="name" error-display-type="normal">
                <bk-input v-model.trim="tokenFormData.name" maxlength="32" show-word-limit></bk-input>
            </bk-form-item>
            <!-- 过期时间 -->
            <bk-form-item :property="'expiredTime'" :label="$t('expire')">
                <bk-date-picker
                    transfer
                    v-model="tokenFormData.expiredTime"
                    class="mt5"
                    style="width: 428px;"
                    :options="{ disabledDate: function (time) {
                        return time.getTime() <= Date.now()
                    } }"
                    :shortcuts="shortcuts"
                    :placeholder="$t('chooseExpire')">
                </bk-date-picker>
            </bk-form-item>
            <bk-form-item :label="$t('tokenScope')" required property="tokenScope" error-display-type="normal">
                <bk-checkbox
                    disabled
                    checked
                    v-model="tokenScope">
                    {{$t('CPack')}}
                </bk-checkbox>
                <span class="token-scope-info">{{$t('CPackTokenInfo')}}</span>
            </bk-form-item>
        </bk-form>
        <template #footer>
            <bk-button v-if="token" theme="primary" @click="cancel">{{$t('confirm')}}</bk-button>
            <template v-else>
                <bk-button @click="cancel">{{$t('cancel')}}</bk-button>
                <bk-button class="ml10" :loading="loading" theme="primary" @click="confirm">{{$t('generate')}}</bk-button>
            </template>
        </template>
    </canway-dialog>
</template>
<script>
    import { mapActions } from 'vuex'
    import moment from 'moment'
    import { copyToClipboard } from '@repository/utils'
    export default {
        name: 'createToken',
        data () {
            return {
                show: false,
                loading: false,
                tokenFormData: {
                    name: '',
                    expiredTime: '',
                    tokenScope: true
                },
                rules: {
                    name: [
                        {
                            required: true,
                            message: this.$t('pleaseInput') + this.$t('space') + 'Token' + this.$t('name'),
                            trigger: 'blur'
                        }
                    ]
                },
                shortcuts: [
                    {
                        text: this.$t('lastDays', [7]),
                        value () {
                            const start = new Date()
                            start.setTime(start.getTime() + 3600 * 1000 * 24 * 7)
                            return start
                        }
                    },
                    {
                        text: this.$t('lastDays', [30]),
                        value () {
                            const start = new Date()
                            start.setTime(start.getTime() + 3600 * 1000 * 24 * 30)
                            return start
                        }
                    },
                    {
                        text: this.$t('lastDays', [90]),
                        value () {
                            const start = new Date()
                            start.setTime(start.getTime() + 3600 * 1000 * 24 * 90)
                            return start
                        }
                    },
                    {
                        text: this.$t('lastYears', [1]),
                        value () {
                            const start = new Date()
                            start.setTime(start.getTime() + 3600 * 1000 * 24 * 365)
                            return start
                        }
                    }
                ],
                token: ''
            }
        },
        methods: {
            ...mapActions(['ciCreateToken']),
            showDialogHandler () {
                this.show = true
                this.tokenFormData = {
                    name: '',
                    expiredTime: ''
                }
                this.token = ''
                this.$refs.tokenForm && this.$refs.tokenForm.clearError()
            },
            async confirm () {
                await this.$refs.tokenForm.validate()
                this.loading = true
                this.ciCreateToken({
                    tokenName: this.tokenFormData.name,
                    tokenScope: ['CPack'],
                    expiredTime: moment(this.tokenFormData.expiredTime).format('YYYY-MM-DD')
                }).then((res) => {
                    this.$emit('token', res)
                    this.token = res
                }).finally(() => {
                    this.loading = false
                })
            },
            cancel () {
                this.show = false
                if (this.token) {
                    this.$emit('refresh')
                }
            },
            copyToken (text) {
                copyToClipboard(text).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('copy') + this.$t('space') + this.$t('success')
                    })
                }).catch(() => {
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('copy') + this.$t('space') + this.$t('fail')
                    })
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.icon-check-1 {
    width: 58px;
    height: 58px;
    margin: 0 auto;
    line-height: 58px;
    font-size: 30px;
    color: white;
    border-radius: 50%;
    background-color: var(--successColor);
}
.token-title {
    font-size: 17px;
    font-weight: bold;
}
.token-tip {
    color: var(--warningColor);
}
.token-scope-info{
    display: block;
    color: #8797aa;
    margin: 0 0 0 20px;
}
.token-value{
    max-width: 350px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
}
</style>
