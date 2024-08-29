<template>
    <div class="proxy-config-container">
        <div class="mb10 flex-between-center">
            <bk-button icon="plus" theme="primary" @click="addProxy">{{ $t('add') }}</bk-button>
            <span class="proxy-config-tips">{{$t('proxyConfigTips')}}</span>
        </div>
        <div class="proxy-head">
            <div class="proxy-index"></div>
            <div class="proxy-origin">{{$t('name')}}</div>
            <div class="proxy-type">{{$t('type')}}</div>
            <div class="proxy-address">{{$t('address')}}</div>
            <div class="proxy-switcher">{{$t('networkProxy')}}</div>
            <div class="cache-switcher">{{$t('cache')}}</div>
            <div class="proxy-operation">{{$t('operation')}}</div>
        </div>
        <draggable v-if="proxyList.length" v-model="proxyList" :options="{ animation: 200 }" @update="debounceSaveProxy">
            <div class="proxy-item" v-for="proxy in proxyList" :key="proxy.name + Math.random()">
                <div class="proxy-index flex-center"><Icon name="drag" size="16" /></div>
                <div class="proxy-origin">{{proxy.name}}</div>
                <div class="proxy-type">{{proxy.public ? $t('publicProxy') : $t('privateProxy')}}</div>
                <div class="proxy-address">{{proxy.url}}</div>
                <div class="proxy-switcher">{{ isEmpty(proxy.networkProxy) ? $t('closed') : $t('opened')}}</div>
                <div class="cache-switcher">{{ isEmpty(proxy.cache) ? $t('closed') : $t('opened')}}</div>
                <div class="flex-align-center proxy-operation">
                    <Icon class="mr10 hover-btn" size="24" name="icon-edit" @click.native.stop="editProxy(proxy)" />
                    <Icon class="hover-btn" size="24" name="icon-delete" @click.native.stop="deleteProxy(proxy)" />
                </div>
            </div>
        </draggable>
        <empty-data v-else ex-style="margin-top:130px;" :title="$t('noProxySourceConfigTitle')" :sub-title="$t('noProxySourceConfigSubTitle')"></empty-data>
        <proxy-origin-dialog :show="showProxyDialog" :proxy-data="proxyData" @confirm="confirmProxyData" @cancel="cancelProxy"></proxy-origin-dialog>
    </div>
</template>
<script>
    import draggable from 'vuedraggable'
    import proxyOriginDialog from './proxyOriginDialog'
    import { mapActions } from 'vuex'
    import { debounce } from '@repository/utils'
    import { isEmpty } from 'lodash'
    export default {
        name: 'proxyConfig',
        components: { draggable, proxyOriginDialog },
        props: {
            baseData: Object
        },
        data () {
            return {
                showProxyDialog: false,
                saveLoading: false,
                // 当前仓库的代理源
                proxyList: [],
                proxyData: {},
                debounceSaveProxy: null,
                isEmpty
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            repoName () {
                return this.$route.query.repoName
            },
            repoType () {
                return this.$route.params.repoType
            }
        },
        watch: {
            baseData: {
                handler () {
                    this.proxyList = this.baseData.configuration.proxy?.channelList || []
                },
                immediate: true
            }
        },
        created () {
            this.debounceSaveProxy = debounce(this.saveProxy)
        },
        methods: {
            ...mapActions(['updateRepoInfo']),
            addProxy () {
                this.showProxyDialog = true
                this.proxyData = {
                    type: 'add'
                }
            },
            editProxy (row) {
                this.showProxyDialog = true
                this.proxyData = {
                    type: 'edit',
                    ...row
                }
            },
            deleteProxy (row) {
                this.proxyList.splice(this.proxyList.findIndex(v => v.name === row.name), 1)
                this.debounceSaveProxy()
            },
            confirmProxyData ({ name, data }) {
                const updateData = {
                    public: data.public,
                    name: data.name,
                    url: data.url,
                    ...(data.username
                        ? {
                            username: data.username,
                            password: data.password
                        }
                        : {})
                }
                if (data.networkProxy.host) {
                    // 当网络代理的 host 存在时表明配置了网络代理，此时请求时就需要添加 networkProxy对象
                    updateData.networkProxy = { ...data.networkProxy }
                }
                if (data.cache?.enabled) {
                    updateData.cache = { ...data.cache }
                }
                // 添加
                if (data.type === 'add') {
                    this.proxyList.push(updateData)
                // 编辑
                } else if (data.type === 'edit') {
                    this.proxyList.splice(this.proxyList.findIndex(v => v.name === name), 1, updateData)
                }
                this.cancelProxy()
                this.debounceSaveProxy()
            },
            cancelProxy () {
                this.showProxyDialog = false
            },
            saveProxy () {
                const names = this.proxyList.map(v => v.name)
                if (names.length !== new Set(names).size) {
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('sameProxyExist')
                    })
                    return
                }
                if (this.saveLoading) return
                this.saveLoading = true
                this.updateRepoInfo({
                    projectId: this.projectId,
                    name: this.repoName,
                    body: {
                        configuration: {
                            ...this.baseData.configuration,
                            proxy: {
                                channelList: this.proxyList
                            }
                        }
                    }
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('save') + this.$t('space') + this.$t('success')
                    })
                }).finally(() => {
                    this.saveLoading = false
                    this.$emit('refresh')
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.proxy-config-container {
    .proxy-config-tips {
        padding: 5px 20px;
        font-size: 12px;
        color: var(--primaryHoverColor);
        background-color: var(--bgHoverLighterColor);
    }
    .proxy-item,
    .proxy-head {
        display: flex;
        align-items: center;
        height: 40px;
        line-height: 40px;
        border-bottom: 1px solid var(--borderColor);
        .proxy-index {
            flex-basis: 50px;
        }
        .proxy-origin {
            flex:2;
        }
        .proxy-type {
            flex: 1;
        }
        .proxy-address {
            flex: 6;
        }
        .proxy-switcher {
            flex: 2;
        }
        .cache-switcher {
            flex: 2;
        }
        .proxy-operation {
            flex:1;
        }
    }
    .proxy-head {
        color: var(--fontSubsidiaryColor);
        background-color: var(--bgColor);
    }
    .proxy-item {
        cursor: move;
    }
}
</style>
