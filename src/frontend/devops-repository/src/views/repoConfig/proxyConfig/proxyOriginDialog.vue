<template>
    <canway-dialog
        v-model="show"
        width="600"
        height-num="463"
        :title="editProxyData.type === 'add' ? $t('addProxy') : $t('editProxy')"
        @cancel="$emit('cancel')"
        @confirm="confirmProxyData">
        <label class="ml20 mr20 mb10 form-label">{{ $t('baseInfo') }}</label>
        <bk-form class="ml20 mr20" ref="proxyOrigin" :label-width="85" :model="editProxyData" :rules="rules">
            <bk-form-item :label="$t('type')" property="public">
                <bk-radio-group v-model="editProxyData.public">
                    <bk-radio :value="true">{{ $t('publicProxy') }}</bk-radio>
                    <bk-radio class="ml20" :value="false">{{ $t('privateProxy') }}</bk-radio>
                </bk-radio-group>
            </bk-form-item>
            <bk-form-item :label="$t('name')" :required="true" property="name" error-display-type="normal">
                <bk-input v-model.trim="editProxyData.name" maxlength="32" show-word-limit></bk-input>
            </bk-form-item>
            <bk-form-item :label="$t('address')" :required="true" property="url" error-display-type="normal">
                <bk-input v-model.trim="editProxyData.url"></bk-input>
            </bk-form-item>
        </bk-form>
        <label class="ml20 mr20 mt20 mb10 form-label">{{$t('credentialInformation')}}</label>
        <bk-form class="ml20 mr20" :label-width="85">
            <bk-form-item :label="$t('account')" property="username">
                <bk-input v-model.trim="editProxyData.username"></bk-input>
            </bk-form-item>
            <bk-form-item :label="$t('password')" property="password">
                <bk-input type="password" v-model.trim="editProxyData.password"></bk-input>
            </bk-form-item>
        </bk-form>
        <label class="ml20 mr20 mt20 mb10 form-label">{{$t('networkProxy')}}</label>
        <bk-form class="ml20 mr20" ref="proxyNetworkRefs" :label-width="85" :model="editNetworkProxyData" :rules="networkRules">
            <bk-form-item :label="$t('proxySwitch')" property="switcher">
                <template>
                    <bk-switcher v-model="editNetworkProxyData.switcher" theme="primary"></bk-switcher>
                    <span>{{editNetworkProxyData.switcher ? $t('open') : $t('close')}}</span>
                </template>
            </bk-form-item>
            <bk-form-item v-if="editNetworkProxyData.switcher" label="IP" property="host" :required="true" error-display-type="normal">
                <bk-input v-model.trim="editNetworkProxyData.host"></bk-input>
            </bk-form-item>
            <bk-form-item v-if="editNetworkProxyData.switcher" :label="$t('port')" property="port" :required="true" error-display-type="normal">
                <bk-input v-model.trim="editNetworkProxyData.port"></bk-input>
                <!-- type="number" :max="65535" :min="1"  -->
            </bk-form-item>
            <bk-form-item v-if="editNetworkProxyData.switcher" :label="$t('account')" property="username">
                <bk-input v-model.trim="editNetworkProxyData.username"></bk-input>
            </bk-form-item>
            <bk-form-item v-if="editNetworkProxyData.switcher" :label="$t('password')" property="password">
                <bk-input type="password" v-model.trim="editNetworkProxyData.password"></bk-input>
            </bk-form-item>
        </bk-form>
        <label class="ml20 mr20 mt20 mb10 form-label">{{$t('cache')}}</label>
        <bk-form class="ml20 mr20" ref="cacheRefs" :label-width="85" :model="editCacheData" :rules="cacheRules">
            <bk-form-item :label="$t('cacheSwitch')" property="switcher">
                <template>
                    <bk-switcher v-model="editCacheData.enabled" theme="primary"></bk-switcher>
                    <span>{{editCacheData.enabled ? $t('open') : $t('close')}}</span>
                </template>
            </bk-form-item>
            <bk-form-item v-if="editCacheData.enabled" :label="$t('expiration')" property="expiration" :required="true" error-display-type="normal">
                <bk-input type="number" v-model.trim="editCacheData.expiration"></bk-input>
            </bk-form-item>
        </bk-form>
    </canway-dialog>
</template>
<script>
    import { isEmpty } from 'lodash'
    export default {
        name: 'proxyOriginDialog',
        props: {
            show: Boolean,
            proxyData: Object
        },
        data () {
            const oldEditNetworkProxyData = {
                switcher: false,
                host: '',
                port: '',
                username: '',
                password: ''
            }
            const defaultCacheData = {
                enabled: true,
                expiration: 120
            }
            return {
                editProxyData: {
                    public: true, // 公有 or 私有
                    type: '', // 添加 or 编辑
                    name: '',
                    url: '',
                    username: '',
                    password: ''
                },
                oldEditNetworkProxyData,
                defaultCacheData,
                editNetworkProxyData: { ...oldEditNetworkProxyData },
                editCacheData: { ...defaultCacheData },
                rules: {
                    name: [
                        {
                            required: true,
                            message: this.$t('proxyNameRule'),
                            trigger: 'blur'
                        }
                    ],
                    url: [
                        {
                            required: true,
                            message: this.$t('proxyUrlRule'),
                            trigger: 'blur'
                        }
                    ]
                },
                networkRules: {
                    host: [
                        {
                            required: true,
                            message: this.$t('proxyIpPlaceholder'),
                            trigger: 'blur'
                        }
                    ],
                    port: [
                        {
                            required: true,
                            message: this.$t('proxyPortPlaceholder'),
                            trigger: 'blur'
                        }
                    ]
                },
                cacheRules: {
                    expiration: [
                        {
                            required: true,
                            message: this.$t('cacheExpirationPlaceholder'),
                            trigger: 'blur'
                        }
                    ]
                }
            }
        },
        watch: {
            proxyData (data) {
                // 在打开弹窗时先将之前的数据校验结果去除
                this.$refs.proxyOrigin && this.$refs.proxyOrigin.clearError()
                this.$refs.proxyNetworkRefs && this.$refs.proxyNetworkRefs.clearError()
                this.$refs.cacheRefs && this.$refs.cacheRefs.clearError()
                if (data.type === 'add') {
                    this.editProxyData = {
                        public: true,
                        type: 'add',
                        name: '',
                        url: '',
                        username: '',
                        password: ''
                    }
                    // 初始化网络代理配置form表单
                    this.editNetworkProxyData = {
                        ...this.oldEditNetworkProxyData
                    }
                    this.editCacheData = { ...this.defaultCacheData }
                } else {
                    this.editProxyData = {
                        ...this.editProxyData,
                        ...data
                    }
                    if (isEmpty(data.networkProxy)) {
                        // 当返回的不存在networkProxy字段时表明之前没有配置网络代理
                        this.editNetworkProxyData = {
                            ...this.oldEditNetworkProxyData
                        }
                    } else {
                        // 此时表明之前配置了网络代理,此时需要将 switcher 字段置为true
                        this.editNetworkProxyData = {
                            ...this.oldEditNetworkProxyData,
                            ...data.networkProxy,
                            switcher: true
                        }
                    }
                    if (isEmpty(data.cache)) {
                        // 当返回的不存在cache字段时表明之前没有配置缓存
                        this.editCacheData = { ...this.defaultCacheData }
                    } else {
                        // 此时表明之前配置了缓存,此时需要将enabled字段置为true
                        this.editCacheData = {
                            ...this.defaultCacheData,
                            ...data.cache,
                            enabled: true
                        }
                    }
                }
            }
        },
        methods: {
            async confirmProxyData () {
                await this.$refs.proxyOrigin.validate()
                if (this.editNetworkProxyData.switcher) {
                    await this.$refs.proxyNetworkRefs.validate()
                    // 根据后台要求，在用户不填username或password时将该字段删除
                    if (!this.editNetworkProxyData.username) {
                        delete this.editNetworkProxyData.username
                    }
                    if (!this.editNetworkProxyData.password) {
                        delete this.editNetworkProxyData.password
                    }
                } else {
                    this.editNetworkProxyData = this.oldEditNetworkProxyData
                }
                delete this.editNetworkProxyData.switcher
                const backData = {
                    name: this.proxyData.name,
                    data: this.editProxyData
                }
                backData.data.networkProxy = { ...this.editNetworkProxyData }
                this.$emit('confirm', backData)
            }
        }
    }
</script>
<style lang="scss" scoped>
.form-label {
    display: block;
    font-weight: bold;
}
</style>
