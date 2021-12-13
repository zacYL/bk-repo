<template>
    <canway-dialog
        v-model="genericShare.show"
        :title="genericShare.title"
        width="520"
        :height-num="342"
        @cancel="cancel">
        <div v-if="shareUrl" class="share-result">
            <bk-form class="flex-1" form-type="vertical">
                <bk-form-item label="共享链接地址">
                    <bk-input :value="shareUrl" readonly></bk-input>
                </bk-form-item>
                <bk-form-item>
                    <bk-button theme="primary" @click="copyShareUrl(shareUrl)">复制链接</bk-button>
                </bk-form-item>
            </bk-form>
            <div class="ml20 flex-column">
                <span class="qrcode-label">移动端二维码下载</span>
                <QRCode class="share-qrcode" :text="shareUrl" :size="150" />
            </div>
        </div>
        <bk-form v-else style="margin-top:-15px" ref="genericShareForm" :label-width="90"
            :model="genericShare" :rules="rules" form-type="vertical">
            <bk-form-item label="授权用户" property="user">
                <bk-tag-input
                    v-model="genericShare.user"
                    :list="Object.values(userList).filter(user => user.id !== 'anonymous')"
                    :search-key="['id', 'name']"
                    placeholder="授权访问用户，为空则任意用户可访问，按Enter键确认"
                    trigger="focus"
                    allow-create
                    has-delete-icon>
                </bk-tag-input>
            </bk-form-item>
            <!-- <bk-form-item label="授权IP" property="ip">
                <bk-tag-input
                    v-model="genericShare.ip"
                    placeholder="授权访问IP，为空则任意IP可访问，按Enter键确认"
                    trigger="focus"
                    allow-create>
                </bk-tag-input>
            </bk-form-item> -->
            <bk-form-item label="访问次数" property="permits" error-display-type="normal">
                <bk-input v-model.trim="genericShare.permits" placeholder="请输入数字，小于等于0则永久有效"></bk-input>
            </bk-form-item>
            <bk-form-item :label="`${$t('validity')}(${$t('day')})`" property="time" error-display-type="normal">
                <bk-input v-model.trim="genericShare.time" placeholder="请输入数字，小于等于0则永久有效"></bk-input>
            </bk-form-item>
        </bk-form>
        <div slot="footer">
            <bk-button theme="default" @click="shareUrl ? sendEmail() : cancel()">{{ shareUrl ? '发送邮件' : $t('cancel') }}</bk-button>
            <bk-button class="ml10" :loading="genericShare.loading" theme="primary" @click="shareUrl ? cancel() : submit()">{{$t('confirm')}}</bk-button>
        </div>
    </canway-dialog>
</template>
<script>
    import Clipboard from 'clipboard'
    import { mapActions, mapState } from 'vuex'
    import QRCode from '@repository/components/QRCode'
    export default {
        name: 'genericShare',
        components: { QRCode },
        data () {
            return {
                shareUrl: '',
                genericShare: {
                    show: false,
                    loading: false,
                    title: '',
                    type: '',
                    path: '',
                    user: [],
                    ip: [],
                    permits: 0,
                    time: 0
                },
                // genericShare Rules
                rules: {
                    permits: [
                        {
                            regex: /^-?[0-9]*$/,
                            message: '请输入数字',
                            trigger: 'blur'
                        }
                    ],
                    time: [
                        {
                            regex: /^-?[0-9]*$/,
                            message: '请输入数字',
                            trigger: 'blur'
                        }
                    ]
                }
            }
        },
        computed: {
            ...mapState(['userList']),
            projectId () {
                return this.$route.params.projectId
            },
            repoName () {
                return this.$route.query.repoName
            }
        },
        methods: {
            ...mapActions(['shareArtifactory']),
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
                const { path, user, time, permits } = this.genericShare
                this.genericShare.loading = true
                this.shareArtifactory({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    fullPathSet: [path],
                    type: 'DOWNLOAD',
                    host: location.origin,
                    needsNotify: true,
                    // ...(data.ip.length ? { authorizedIpSet: data.ip } : {}),
                    ...(user.length ? { authorizedUserSet: user } : {}),
                    ...(Number(time) > 0 ? { expireSeconds: Number(time) * 86400 } : {}),
                    ...(Number(permits) > 0 ? { permits: Number(permits) } : {})
                }).then(([{ url }]) => {
                    this.shareUrl = url
                }).finally(() => {
                    this.genericShare.loading = false
                })
            },
            copyShareUrl (text) {
                const clipboard = new Clipboard('body', {
                    text: () => text
                })
                clipboard.on('success', (e) => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('copy') + this.$t('success')
                    })
                    clipboard.destroy()
                })
                clipboard.on('error', (e) => {
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('copy') + this.$t('fail')
                    })
                    clipboard.destroy()
                })
            },
            sendEmail () {}
        }
    }
</script>
<style lang="scss" scoped>
.share-result {
    display: flex;
    height: 196px;
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
