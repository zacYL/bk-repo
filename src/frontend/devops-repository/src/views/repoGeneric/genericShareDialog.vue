<template>
    <canway-dialog
        v-model="genericShare.show"
        :title="genericShare.title"
        width="520"
        :height-num="shareUrl ? 352 : 282"
        @cancel="cancel">
        <div v-if="shareUrl" class="share-result">
            <bk-form class="flex-1" form-type="vertical">
                <bk-form-item :label="$t('genericShareUrl')">
                    <bk-input :value="shareUrl" readonly></bk-input>
                    <bk-button class="mt5" theme="primary" @click="copyShareUrl(shareUrl)">{{$t('copyGenericShareUrl')}}</bk-button>
                </bk-form-item>
                <bk-form-item :label="$t('genericEmailShare')">
                    <bk-select
                        v-model="genericShare.user"
                        multiple
                        clearable
                        searchable
                        :placeholder="$t('selectUserMsg')"
                        :title="genericShare.user.map(u => userList[u] ? userList[u].name : u)"
                        :enable-virtual-scroll="Object.values(userList).length > 3000"
                        :list="Object.values(userList).filter(user => user.id !== 'anonymous')">
                        <bk-option v-for="option in Object.values(userList).filter(user => user.id !== 'anonymous')"
                            :key="option.id"
                            :id="option.id"
                            :name="option.name">
                        </bk-option>
                    </bk-select>
                    <bk-button class="mt5" :disabled="!Boolean(genericShare.user.length)" theme="primary" :loading="sending" @click="sendEmailHandler">{{$t('sendEmail')}}</bk-button>
                </bk-form-item>
            </bk-form>
            <div class="ml20 flex-column">
                <span class="qrcode-label">{{$t('mobileQcCodeDownload')}}</span>
                <QRCode class="share-qrcode" :text="shareUrl" :size="150" />
            </div>
        </div>
        <bk-form v-else style="margin-top:-15px" ref="genericShareForm" :label-width="90" form-type="vertical">
            <bk-form-item :label="$t('visits')">
                <bk-input v-model="genericShare.permits" :placeholder="$t('sharePlaceholder2')"></bk-input>
            </bk-form-item>
            <bk-form-item :label="$t('validity')">
                <bk-select
                    v-model="genericShare.time"
                    :clearable="false"
                    :placeholder="$t('sharePlaceholder3')">
                    <bk-option :id="1" name="1"></bk-option>
                    <bk-option :id="7" name="7"></bk-option>
                    <bk-option :id="30" name="30"></bk-option>
                    <bk-option :id="0" :name="$t('permanent')"></bk-option>
                </bk-select>
            </bk-form-item>
        </bk-form>
        <template #footer>
            <bk-button v-if="!shareUrl" theme="default" @click="cancel">{{ $t('cancel') }}</bk-button>
            <bk-button class="ml10" :loading="genericShare.loading" theme="primary" @click="shareUrl ? cancel() : submit()">{{$t('confirm')}}</bk-button>
        </template>
    </canway-dialog>
</template>
<script>
    import QRCode from '@repository/components/QRCode'
    import { mapActions, mapState } from 'vuex'
    import { copyToClipboard } from '@repository/utils'
    export default {
        name: 'genericShare',
        components: { QRCode },
        data () {
            return {
                shareUrl: '',
                sending: false,
                genericShare: {
                    projectId: '',
                    repoName: '',
                    show: false,
                    loading: false,
                    title: '',
                    path: '',
                    user: [],
                    ip: [],
                    permits: '',
                    time: 0
                }
            }
        },
        computed: {
            ...mapState(['userList'])
        },
        methods: {
            ...mapActions(['shareArtifactory', 'sendEmail']),
            setData (data) {
                this.genericShare = {
                    ...this.genericShare,
                    ...data
                }
                this.shareUrl = ''
                this.$refs.genericShareForm && this.$refs.genericShareForm.clearError()
            },
            submit () {
                this.$refs.genericShareForm.validate().then(() => {
                    this.submitShare()
                })
            },
            cancel () {
                this.$refs.genericShareForm && this.$refs.genericShareForm.clearError()
                this.genericShare.show = false
            },
            submitShare () {
                const { projectId, repoName, path, time, permits } = this.genericShare
                this.genericShare.loading = true
                this.shareArtifactory({
                    projectId,
                    repoName,
                    fullPathSet: [path],
                    type: 'DOWNLOAD',
                    host: `${location.origin}/web/generic`,
                    // needsNotify: Boolean(user.length),
                    // ...(data.ip.length ? { authorizedIpSet: data.ip } : {}),
                    // ...(user.length ? { authorizedUserSet: user } : {}),
                    ...(Number(time) > 0 ? { expireSeconds: Number(time) * 86400 } : {}),
                    ...(Number(permits) > 0 ? { permits: Number(permits) } : {})
                }).then(([{ url }]) => {
                    this.shareUrl = url
                }).finally(() => {
                    this.genericShare.loading = false
                })
            },
            copyShareUrl (text) {
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
            },
            sendEmailHandler () {
                const users = this.genericShare.user
                if (this.sending || !users.length) return
                this.sending = true
                this.sendEmail({
                    url: this.shareUrl,
                    users
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('sendEmail') + this.$t('space') + this.$t('success')
                    })
                }).finally(() => {
                    this.sending = false
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.share-result {
    display: flex;
    height: 206px;
    margin-top: -15px;
    .qrcode-label {
        color: var(--fontPrimaryColor);
        font-size: 12px;
        line-height: 32px;
    }
    .share-qrcode {
        border: 1px dashed;
    }
}
</style>
