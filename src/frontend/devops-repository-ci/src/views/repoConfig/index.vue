<template>
    <div class="repo-config-container" v-bkloading="{ isLoading }">
        <bk-tab v-if="showTabPanel" class="repo-config-tab page-tab" :class="[tabName === 'baseInfo' ? 'base-info-tab' : '']" type="unborder-card" :active.sync="tabName">
            <bk-tab-panel name="baseInfo" :label="$t('repoBaseInfo')">
                <bk-alert
                    v-if="tipsObj"
                    :style="{
                        marginLeft: currentLanguage === 'zh-cn' ? '82px' : '60px',
                        width: `fix-content`
                    }"
                    class="mt10"
                    type="warning"
                    :title="tipsObj.tips">
                </bk-alert>
                <div>
                    
                    <bk-form ref="repoBaseInfo" class="repo-base-info" :label-width="150" :model="repoBaseInfo" :rules="rules">
                        <bk-form-item :label="$t('repoName')">
                            <div class="flex-align-center">
                                <icon size="20" :name="repoBaseInfo.repoType || repoType" />
                                <span class="ml10">{{replaceRepoName(repoBaseInfo.name || repoName)}}</span>
                            </div>
                        </bk-form-item>
                        <bk-form-item :label="$t('storeTypes')">
                            <div class="flex-align-center">
                                <icon size="20" :name="(repoBaseInfo.category && repoBaseInfo.category.toLowerCase() || 'local') + '-store'" />
                                <span class="ml10">{{$t((repoBaseInfo.category.toLowerCase() || 'local') + 'Store' ) }}</span>
                            </div>
                        </bk-form-item>
                        <bk-form-item :label="$t('repoAddress')">
                            <span>{{repoAddress}}</span>
                        </bk-form-item>
                        <template v-if="repoBaseInfo.category === 'REMOTE'">
                            <bk-form-item :label="$t('remoteProxyAddress')" :required="true" property="url" error-display-type="normal">
                                <bk-input class="w480" v-model.trim="repoBaseInfo.url"></bk-input>
                                <!-- todo 测试链接暂未支持 -->
                                <bk-button v-if="repoBaseInfo.type !== 'GENERIC'" theme="primary" :disabled="disableTestUrl" @click="onClickTestRemoteUrl">{{ $t('testRemoteUrl') }}</bk-button>
                            </bk-form-item>
                            <bk-form-item :label="$t('remoteProxyAccount')" property="credentials.username" error-display-type="normal">
                                <bk-input class="w480" v-model.trim="repoBaseInfo.credentials.username"></bk-input>
                            </bk-form-item>
                            <bk-form-item :label="$t('remoteProxyPassword')" property="credentials.password" error-display-type="normal">
                                <bk-input class="w480" type="password" v-model.trim="repoBaseInfo.credentials.password"></bk-input>
                            </bk-form-item>
                            <bk-form-item :label="$t('networkProxy')" property="switcher">
                                <template>
                                    <bk-switcher v-model="repoBaseInfo.network.switcher" theme="primary"></bk-switcher>
                                    <span>{{repoBaseInfo.network.switcher ? $t('open') : $t('close')}}</span>
                                </template>
                            </bk-form-item>
                            <div v-if="repoBaseInfo.network.switcher" class="pt20 pb20" style="padding-left: 70px;">
                                <bk-form-item label="IP" property="network.proxy.host" :required="true" error-display-type="normal">
                                    <bk-input class="w480" v-model.trim="repoBaseInfo.network.proxy.host"></bk-input>
                                </bk-form-item>
                                <bk-form-item :label="$t('port')" property="network.proxy.port" :required="true" error-display-type="normal">
                                    <bk-input
                                        class="w480"
                                        type="number"
                                        :max="65535"
                                        :min="1"
                                        :class="{ 'bk-form-item is-error': errorProxyPortInfo }"
                                        v-model.trim="repoBaseInfo.network.proxy.port"
                                        @blur="onBlurProxyPort"
                                        @focus="errorProxyPortInfo = false">
                                    </bk-input>
                                    <p class="form-error-tip" v-if="errorProxyPortInfo">{{$t('repositoryProxyPortInfo')}}</p>
                                </bk-form-item>
                                <bk-form-item :label="$t('networkAccount')" property="network.proxy.username">
                                    <bk-input class="w480" v-model.trim="repoBaseInfo.network.proxy.username"></bk-input>
                                </bk-form-item>
                                <bk-form-item :label="$t('networkPassword')" property="network.proxy.password">
                                    <bk-input class="w480" type="password" v-model.trim="repoBaseInfo.network.proxy.password"></bk-input>
                                </bk-form-item>
                            </div>
                            <bk-form-item v-show="['GO'].includes(repoBaseInfo.type) && repoBaseInfo.category === 'REMOTE'" :label="$t('cache')" property="switcher">
                                <template>
                                    <bk-switcher v-model="repoBaseInfo.cache.enabled" theme="primary"></bk-switcher>
                                    <span>{{repoBaseInfo.cache.enabled ? $t('open') : $t('close')}}</span>
                                </template>
                            </bk-form-item>
                            <div v-if="repoBaseInfo.cache.enabled && ['GO'].includes(repoBaseInfo.type)" class="pt20 pb20">
                                <bk-form-item :label="$t('expiration')" property="cache.expiration" :required="true" :rules="rules.cache" error-display-type="normal">
                                    <bk-input :placeholder="$t('cacheExpirationPlaceholder')" class="w480" type="number" v-model.trim="repoBaseInfo.cache.expiration"></bk-input>
                                </bk-form-item>
                            </div>
                        </template>
                        <template v-if="repoBaseInfo.category === 'VIRTUAL'">
                            <bk-form-item :label="$t('select') + $t('space') + $t('storageStore')" property="virtualStoreList" :required="true" error-display-type="normal">
                                <bk-button class="mb10" hover-theme="primary" @click="toCheckedStore">{{ $t('pleaseSelect') }}</bk-button>
                                <div class="virtual-check-container">
                                    <store-sort
                                        v-if="repoBaseInfo.virtualStoreList.length"
                                        :key="repoBaseInfo.virtualStoreList"
                                        ref="storeSortRef"
                                        :sort-list="repoBaseInfo.virtualStoreList"
                                        @update="onUpdateList"></store-sort>
                                </div>
                            </bk-form-item>
                        </template>
                        <template v-if="!['generic', 'composer'].includes(repoType)">
                            <bk-form-item
                                :label="$t('includePath')">
                                <bk-button @click="addPath('includesPath')" class="mr5">{{ $t("addPath") }}</bk-button>
                                <bk-icon-plus type="plus-hint" v-bk-tooltips="includesPathDesc" />
                            </bk-form-item>
                            <bk-form-item
                                v-for="(item, index) in repoBaseInfo.includesPath"
                                required
                                :rules="rules.includesPath"
                                :property="'includesPath.' + index + '.value'"
                                :key="index"
                                style="width: fit-content;"
                                class="mt10">
                                <bk-input v-model="item.value" :placeholder="$t('pleaseInput') + $t('space') + $t('verificationRules')" style="width: 180px;">
                                </bk-input>
                                <Icon class="hover-btn" size="24" name="icon-delete" @click.native.stop="repoBaseInfo.includesPath.splice(index, 1)" style="position: absolute; right: -30px; top: 4px;" />
                            </bk-form-item>
                            <bk-form-item :label="$t('ignorePath')">
                                <bk-button @click="addPath('ignoresPath')">{{ $t("addPath") }}</bk-button>
                            </bk-form-item>
                            <bk-form-item
                                v-for="(item, index) in repoBaseInfo.ignoresPath"
                                :rules="rules.ignoresPath"
                                :property="'ignoresPath.' + index + '.value'"
                                :key="index"
                                style="width: fit-content;"
                                class="mt10">
                                <bk-input v-model="item.value" :placeholder="$t('pleaseInput') + $t('space') + $t('verificationRules')" style="width: 180px;">
                                </bk-input>
                                <Icon class="hover-btn" size="24" name="icon-delete" @click.native.stop="repoBaseInfo.ignoresPath.splice(index, 1)" style="position: absolute; right: -30px; top: 4px;" />
                            </bk-form-item>
                        </template>
                        <bk-form-item :label="$t('accessPermission')">
                            <card-radio-group
                                v-model="available"
                                :list="availableList"
                                :disabled="repoBaseInfo.name === 'pipeline' || repoBaseInfo.name === 'report'"
                            >
                            </card-radio-group>
                        </bk-form-item>

                        <bk-form-item :label="$t('versionStrategy')" v-if="!(repoBaseInfo.category === 'REMOTE') && !(repoBaseInfo.category === 'VIRTUAL') && (repoType === 'maven' || repoType === 'npm')">
                            <div class="flex-align-center">
                                <bk-switcher
                                    v-model="repoBaseInfo.override.switcher"
                                    size="small"
                                    theme="primary"
                                    @change="handleOverrideChange"
                                ></bk-switcher>
                                <span class="ml10">{{$t('coverStrategyInfo')}}</span>
                            </div>
                            <bk-radio-group v-model="repoBaseInfo.override.isFlag" v-if="repoBaseInfo.override.switcher">
                                <bk-radio class="mr20" :value="false">{{$t('notAllowCover')}}</bk-radio>
                                <bk-radio :value="true">{{$t('allowCover')}}</bk-radio>
                            </bk-radio-group>
                        </bk-form-item>
                        <template v-if="repoType === 'generic' && repoBaseInfo.category === 'LOCAL'">
                            <!-- <bk-form-item v-for="type in ['mobile', 'web']" :key="type" -->
                            <bk-form-item v-for="type in ['web']" :key="type"
                                :label="$t(`${type}Download`)" :property="`${type}.enable`">
                                <bk-radio-group v-model="repoBaseInfo[type].enable">
                                    <bk-radio class="mr20" :value="true">{{ $t('open') }}</bk-radio>
                                    <bk-radio :value="false">{{ $t('close') }}</bk-radio>
                                </bk-radio-group>
                                <template v-if="repoBaseInfo[type].enable">
                                    <bk-form-item :label="$t('fileName')" :label-width="80" class="mt10"
                                        :property="`${type}.filename`" required error-display-type="normal">
                                        <bk-input class="w250" v-model.trim="repoBaseInfo[type].filename"></bk-input>
                                        <i class="bk-icon icon-info f14 ml5" v-bk-tooltips="$t('fileNameRule')"></i>
                                    </bk-form-item>
                                    <bk-form-item :label="$t('metadata')" :label-width="80"
                                        :property="`${type}.metadata`" required error-display-type="normal">
                                        <bk-input class="w250" v-model.trim="repoBaseInfo[type].metadata" :placeholder="$t('metadataRule')"></bk-input>
                                    </bk-form-item>
                                </template>
                            </bk-form-item>
                        </template>
                        <template v-if="repoType === 'docker' && (repoBaseInfo.category === 'LOCAL' || repoBaseInfo.category === 'REMOTE')">
                            <bk-form-item :label="$t('enabledLibraryNamespace')">
                                <bk-checkbox v-model="repoBaseInfo.enabledLibraryNamespace"></bk-checkbox>
                            </bk-form-item>
                        </template>
                        <template v-if="!(repoBaseInfo.category === 'REMOTE') && !(repoBaseInfo.category === 'VIRTUAL') && repoType === 'rpm'">
                            <bk-form-item :label="$t('enabledFileLists')">
                                <bk-checkbox v-model="repoBaseInfo.enabledFileLists"></bk-checkbox>
                            </bk-form-item>
                            <bk-form-item :label="$t('repoDataDepth')" property="repodataDepth" error-display-type="normal">
                                <bk-input class="w480" v-model.trim="repoBaseInfo.repodataDepth"></bk-input>
                            </bk-form-item>
                            <bk-form-item :label="$t('groupXmlSet')" property="groupXmlSet" error-display-type="normal">
                                <bk-tag-input
                                    class="w480"
                                    :value="repoBaseInfo.groupXmlSet"
                                    @change="(val) => {
                                        repoBaseInfo.groupXmlSet = val.map(v => {
                                            return v.replace(/^([^.]*)(\.xml)?$/, '$1.xml')
                                        })
                                    }"
                                    :list="[]"
                                    trigger="focus"
                                    :clearable="false"
                                    allow-create
                                    has-delete-icon>
                                </bk-tag-input>
                            </bk-form-item>
                        </template>
                        <bk-form-item :label="$t('description')">
                            <bk-input type="textarea"
                                class="w480"
                                maxlength="200"
                                :rows="6"
                                v-model.trim="repoBaseInfo.description"
                                :placeholder="$t('repoDescriptionPlaceholder')">
                            </bk-input>
                        </bk-form-item>
                        <bk-form-item>
                            <bk-button :loading="repoBaseInfo.loading" theme="primary" @click="saveBaseInfo">{{$t('save')}}</bk-button>
                        </bk-form-item>
                    </bk-form>
                </div>
            </bk-tab-panel>
            <bk-tab-panel v-if="showProxyConfigTab" name="proxyConfig" :label="$t('proxyConfig')">
                <proxy-config :base-data="repoBaseInfo" @refresh="getRepoInfoHandler"></proxy-config>
            </bk-tab-panel>
            <bk-tab-panel v-if="showCleanConfigTab" render-directive="if" name="cleanConfig" :label="$t('cleanSetting')">
                <clean-config :base-data="repoBaseInfo" @refresh="getRepoInfoHandler"></clean-config>
            </bk-tab-panel>
            <bk-tab-panel name="permissionConfig" :label="$t('permissionPath')">
                <permissionConfig :base-data="repoBaseInfo" @refresh="getRepoInfoHandler"></permissionConfig>
            </bk-tab-panel>
        </bk-tab>
        <check-target-store
            ref="checkTargetStoreRef"
            :repo-type="repoBaseInfo.type"
            :check-list="repoBaseInfo.virtualStoreList"
            @checkedTarget="onCheckedTargetStore">
        </check-target-store>
    </div>
</template>
<script>
    import CardRadioGroup from '@repository/components/CardRadioGroup'
    import proxyConfig from '@repository/views/repoConfig/proxyConfig'
    import cleanConfig from '@repository/views/repoConfig/cleanConfig'
    import permissionConfig from '@repository/views/repoConfig/permissionConfig/index'
    import CheckTargetStore from '@repository/components/CheckTargetStore'
    import StoreSort from '@repository/components/StoreSort'
    import { mapState, mapActions } from 'vuex'
    import { isEmpty } from 'lodash'
    import { checkValueIsNullOrEmpty } from '@repository/utils'

    export default {
        name: 'repoConfig',
        components: {
            CardRadioGroup,
            proxyConfig,
            cleanConfig,
            permissionConfig,
            StoreSort,
            CheckTargetStore
        },
        data () {
            return {
                tabName: 'baseInfo',
                isLoading: false,
                repoBaseInfo: {
                    loading: false,
                    repoName: '',
                    public: false,
                    system: false,
                    repoType: '',
                    category: '',
                    enabledLibraryNamespace: false,
                    enabledFileLists: false,
                    repodataDepth: 0,
                    groupXmlSet: [],
                    description: '',
                    override: {
                        switcher: false,
                        isFlag: true
                    },
                    mobile: {
                        enable: false,
                        filename: '',
                        metadata: ''
                    },
                    web: {
                        enable: false,
                        filename: '',
                        metadata: ''
                    },
                    // 远程仓库的地址下面的账号和密码
                    credentials: {
                        username: null,
                        password: null
                    },
                    url: '', // 远程仓库的地址
                    // 远程仓库的网络代理
                    network: {
                        proxy: {
                            host: null,
                            port: null,
                            username: null,
                            password: null
                        }
                    },
                    cache: {
                        enabled: true,
                        expiration: 120
                    },
                    // 虚拟仓库的选中的存储库列表
                    virtualStoreList: [],
                    // deploymentRepo: '' // 虚拟仓库中选择存储的本地仓库
                    includesPath: [],
                    ignoresPath: []
                },
                // 是否展示tab标签页，因为代理设置和清理设置需要根据详情页接口返回的数据判断是否显示，解决异步导致的tab顺序错误的问题
                showTabPanel: false,
                disableTestUrl: false,
                errorProxyPortInfo: false
            }
        },
        computed: {
            ...mapState(['domain']),
            tipsObj () {
                if (this.protocol === 'http' && ['GO'].includes(this.repoBaseInfo.type) && ['project', 'system'].includes(this.available)) {
                    return {
                        tips: this.$t('goWarnTips')
                    }
                }
                return ''
            },
            protocol () {
                return window.location.protocol.replace(':', '')
            },
            projectId () {
                return this.$route.params.projectId
            },
            repoName () {
                return this.$route.query.repoName
            },
            repoType () {
                return this.$route.params.repoType
            },
            showProxyConfigTab () {
                return this.repoBaseInfo.category === 'COMPOSITE' && ['maven', 'npm', 'pypi', 'composer', 'nuget'].includes(this.repoType)
            },
            showCleanConfigTab () {
                return (this.repoBaseInfo.category === 'LOCAL' || this.repoBaseInfo.category === 'COMPOSITE') && ['maven', 'docker', 'npm', 'helm', 'generic'].includes(this.repoType)
            },
            repoAddress () {
                const { repoType, name } = this.repoBaseInfo
                if (repoType === 'docker') {
                    return `${location.protocol}//${this.domain.docker}/${this.projectId}/${name}/`
                }
                return `${location.origin}/${repoType}/${this.projectId}/${name}/`
            },
            available: {
                get () {
                    if (this.repoBaseInfo.name === 'pipeline' || this.repoBaseInfo.name === 'report') {
                        return 'project'
                    }
                    if (this.repoBaseInfo.public) return 'public'
                    if (this.repoBaseInfo.system) return 'system'
                    return 'project'
                },
                set (val) {
                    this.repoBaseInfo.public = val === 'public'
                    this.repoBaseInfo.system = val === 'system'
                }
            },
            availableList () {
                return [
                    { label: this.$t('openProjectLabel'), value: 'project', tip: this.$t('openProjectTip') },
                    { label: this.$t('systemPublic'), value: 'system', tip: this.$t('systemPublicTip') },
                    { label: this.$t('openPublicLabel'), value: 'public', tip: this.$t('openPublicTip') }
                ]
            },
            includesPathDesc () {
                return {
                    allowHtml: true,
                    content: '1',
                    html: `<p>${this.$t('includesPathDesc1')}</p>
                        <p>${this.$t('includesPathDesc2')}</p>
                        <p>${this.$t('includesPathDesc3')}</p>
                        <p>${this.$t('includesPathDesc4')}</p>
                        ${this.repoType === 'maven'
                        ? `<p>${this.$t('includesPathDesc5')}</p>
                           <p>${this.$t('includesPathDesc6')}</p>`
                    : ''}`
                }
            },
            rules () {
                const pathRule = [
                    {
                        required: true,
                        message: this.$t('cantSaveEmptyString'),
                        trigger: 'blur'
                    },
                    {
                        validator: this.checkEmptyPath,
                        message: this.$t('cantSaveEmptyString'),
                        trigger: 'blur'
                    }
                ]
                const includesPathRule = [
                    ...pathRule,
                    {
                        validator: this.checkSamePath('includesPath'),
                        message: this.$t('cantPassSamePath'),
                        trigger: 'blur'
                    }
                ]
                const ignoresPathRule = [
                    ...pathRule,
                    {
                        validator: this.checkSamePath('ignoresPath'),
                        message: this.$t('cantPassSamePath'),
                        trigger: 'blur'
                    }
                ]
                const filenameRule = [
                    {
                        required: true,
                        message: this.$t('pleaseFileName'),
                        trigger: 'blur'
                    }
                ]
                const metadataRule = this.repoBaseInfo.category !== 'REMOTE'
                    ? [
                        {
                            required: true,
                            message: this.$t('pleaseMetadata'),
                            trigger: 'blur'
                        },
                        {
                            regex: /^[^\s]+:[^\s]+/,
                            message: this.$t('metadataRule'),
                            trigger: 'blur'
                        }
                    ]
                    : []
                // 远程仓库的 地址校验规则
                const urlRule = [
                    {
                        required: true,
                        message: this.$t('pleaseInput') + this.$t('space') + this.$t('address'),
                        trigger: 'blur'
                    },
                    {
                        validator: this.checkRemoteUrl,
                        message: this.$t('pleaseInput') + this.$t('space') + this.$t('legit') + this.$t('space') + this.$t('address'),
                        trigger: 'blur'
                    },
                    {
                        validator: this.checkRemoteRepeatUrl,
                        message: this.$t('remoteArtifactUrlInfo'),
                        trigger: 'blur'
                    }
                ]
                // 远程仓库下代理的IP和端口的校验的校验规则
                const proxyHostRule = [
                    {
                        required: true,
                        message: this.$t('pleaseInput') + this.$t('space') + this.$t('networkProxy') + this.$t('space') + 'IP',
                        trigger: 'blur'
                    }
                ]
                const proxyPortRule = [
                    {
                        required: true,
                        message: this.$t('pleaseInput') + this.$t('space') + this.$t('networkProxy') + this.$t('space') + this.$t('port'),
                        trigger: 'blur'
                    }
                ]
                // 虚拟仓库下选择存储库的校验
                const checkStorageRule = [
                    {
                        required: true,
                        message: this.$t('noSelectStorageStore') + this.$t('space') + this.$t('save'),
                        trigger: 'blur'
                    }
                ]
                const cacheRule = [
                    {
                        required: true,
                        message: this.$t('cantSaveEmptyString'),
                        trigger: 'blur'
                    },
                    {
                        regex: /^\d+$/,
                        message: this.$t('nonEmptyPositiveIntegerTip'),
                        trigger: 'blur'
                    }
                ]
                return {
                    repodataDepth: [
                        {
                            regex: /^(0|[1-9][0-9]*)$/,
                            message: this.$t('pleaseInput') + this.$t('space') + this.$t('legit') + this.$t('space') + this.$t('repoDataDepth'),
                            trigger: 'blur'
                        }
                    ],
                    groupXmlSet: [
                        {
                            validator: arr => {
                                return arr.every(v => {
                                    return /\.xml$/.test(v)
                                })
                            },
                            message: this.$t('pleaseInput') + this.$t('space') + this.$t('legit') + this.$t('space') + this.$t('groupXmlSet') + this.$t('space') + `(.xml${this.$t('type')})`,
                            trigger: 'change'
                        }
                    ],
                    ignoresPath: ignoresPathRule,
                    includesPath: includesPathRule,
                    'mobile.filename': filenameRule,
                    'mobile.metadata': metadataRule,
                    'web.filename': filenameRule,
                    'web.metadata': metadataRule,
                    // 远程仓库才应该有地址的校验
                    url: this.repoBaseInfo.category === 'REMOTE' ? urlRule : {},
                    cache: (this.repoBaseInfo.category === 'REMOTE' && ['GO'].includes(this.repoBaseInfo.type)) ? cacheRule : {},
                    // 远程仓库且开启网络代理才应该设置代理的IP和端口的校验
                    'network.proxy.host': (this.repoBaseInfo.category === 'REMOTE' && this.repoBaseInfo.network.switcher) ? proxyHostRule : {},
                    'network.proxy.port': (this.repoBaseInfo.category === 'REMOTE' && this.repoBaseInfo.network.switcher) ? proxyPortRule : {},
                    // 虚拟仓库的选择存储库的校验
                    virtualStoreList: this.repoBaseInfo.category === 'VIRTUAL' ? checkStorageRule : {}
                }
            }
            // 虚拟仓库中选择上传的目标仓库的下拉列表数据
            // deploymentRepoCheckList () {
            //     return this.repoBaseInfo.virtualStoreList.filter(item => item.category === 'LOCAL')
            // }
        },
        watch: {
            repoType: {
                handler (type) {
                    type && this.getDomain(type)
                },
                immediate: true
            },
            'repoBaseInfo.network.switcher' (val) {
                !val && (this.errorProxyPortInfo = false)
            }
        },
        created () {
            if (!this.repoName || !this.repoType) this.toRepoList()
            this.getRepoInfoHandler()
        },
        methods: {
            ...mapActions(['getRepoInfo', 'updateRepoInfo', 'getDomain', 'testRemoteUrl']),
            addPath (key) {
                this.repoBaseInfo[key].push({ value: '' })
            },
            // 打开选择存储库弹窗
            toCheckedStore () {
                this.$refs.checkTargetStoreRef && (this.$refs.checkTargetStoreRef.show = true)
            },
            // 当删除了选中的存储库时
            onUpdateList (list) {
                this.repoBaseInfo.virtualStoreList = list
            },
            onBlurProxyPort () {
                this.errorProxyPortInfo = isNaN(Number(this.repoBaseInfo.network.proxy.port))
            },
            // 选中的存储库弹窗确认事件
            onCheckedTargetStore (list) {
                this.repoBaseInfo.virtualStoreList = list
            },
            checkEmptyPath (val) {
                const v = val || ''
                return !!(v.trim())
            },
            checkSamePath (key) {
                return (val) => {
                    const num = this.repoBaseInfo[key].filter(item => {
                        return item.value === val
                    }).length
                    return !(num > 1)
                }
            },
            checkRemoteUrl (val) {
                const reg = /^https?:\/\/(([a-zA-Z0-9_-])+(\.)?)*(:\d+)?(\/((\.)?(\?)?=?&?[a-zA-Z0-9_-](\?)?)*)*$/
                return reg.test(val)
            },
            // 校验当前输入的远程代理源地址是否是当前仓库
            checkRemoteRepeatUrl (val) {
                let urlSplicing
                if (this.repoBaseInfo.type?.toLowerCase() === 'docker') {
                    urlSplicing = `${location.protocol}//${this.domain.docker}/${this.projectId}/${this.repoBaseInfo.name}`
                } else {
                    const originHref = window.location.origin
                    urlSplicing = originHref + '/' + this.repoBaseInfo.type?.toLowerCase() + '/' + this.projectId + '/' + this.repoBaseInfo.name
                }
                // 特殊的，地址后面需要再添加一个 / 的
                const urlSplicingSpecial = urlSplicing + '/'
                return !(urlSplicing === val || urlSplicingSpecial === val)
            },
            // 创建远程仓库弹窗中测试远程链接
            onClickTestRemoteUrl () {
                if (this.repoBaseInfo.network.switcher && (!this.repoBaseInfo.network.proxy.host || !this.repoBaseInfo.network.proxy.port)) {
                    this.$bkMessage({
                        theme: 'warning',
                        limit: 3,
                        message: this.$t('pleaseInput') + this.$t('space') + this.$t('legit') + this.$t('space') + this.$t('networkProxy')
                    })
                    return
                }
                if (this.repoBaseInfo.network.switcher && this.errorProxyPortInfo) return
                if (!this.repoBaseInfo?.url || isEmpty(this.repoBaseInfo.url) || !this.checkRemoteUrl(this.repoBaseInfo?.url)) {
                    this.$bkMessage({
                        theme: 'warning',
                        limit: 3,
                        message: this.$t('pleaseInput') + this.$t('space') + this.$t('legit') + this.$t('space') + this.$t('address')
                    })
                } else {
                    const body = {
                        type: this.repoBaseInfo.type.toUpperCase(),
                        url: this.repoBaseInfo.url,
                        network: {
                            proxy: null
                        }
                    }
                    if (checkValueIsNullOrEmpty(this.repoBaseInfo.credentials.username)
                        && checkValueIsNullOrEmpty(this.repoBaseInfo.credentials.password)) {
                        body.credentials = {
                            username: null,
                            password: null
                        }
                    } else {
                        body.credentials = this.repoBaseInfo.credentials
                    }
                    if (this.repoBaseInfo.network.switcher) {
                        body.network.proxy = this.repoBaseInfo.network.proxy
                        if (checkValueIsNullOrEmpty(this.repoBaseInfo.network.proxy?.username)
                            && checkValueIsNullOrEmpty(this.repoBaseInfo.network.proxy?.password)) {
                            body.network.proxy.username = null
                            body.network.proxy.password = null
                        }
                    }
                    this.disableTestUrl = true
                    this.testRemoteUrl({ body }).then((res) => {
                        if (res.success) {
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('successConnectServer')
                            })
                        } else {
                            this.$bkMessage({
                                theme: 'error',
                                message: this.$t('connectFailed') + this.$t('space') + `: ${res.message}`
                            })
                        }
                    }).finally(() => {
                        this.disableTestUrl = false
                    })
                }
            },
            toRepoList () {
                this.$router.push({
                    name: 'repoList'
                })
            },
            handleOverrideChange (isFlag) {
                this.repoBaseInfo.override.switcher = isFlag
            },
            getRepoInfoHandler () {
                this.isLoading = true
                this.getRepoInfo({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    repoType: this.repoType
                }).then(res => {
                    this.repoBaseInfo = {
                        ...this.repoBaseInfo,
                        ...res,
                        ...res.configuration.settings,
                        repoType: res.type.toLowerCase(),
                        category: res.category
                    }
                    if (res.type === 'MAVEN' || res.type === 'NPM') {
                        switch (res.coverStrategy) {
                            case 'COVER':
                                this.repoBaseInfo.override.switcher = true
                                this.repoBaseInfo.override.isFlag = true
                                break
                            case 'UNCOVER':
                                this.repoBaseInfo.override.switcher = true
                                this.repoBaseInfo.override.isFlag = false
                                break
                            default:
                                this.repoBaseInfo.override.switcher = false
                                this.repoBaseInfo.override.isFlag = true
                        }
                    }
                    // 虚拟仓库，添加可选仓库穿梭框及上传目标仓库下拉框
                    if (res.category === 'VIRTUAL') {
                        this.repoBaseInfo.virtualStoreList = res.configuration.repositoryList
                        // 当后台返回的字段为null时需要将其设置为空字符串，否则会因为组件需要的参数类型不对应，导致选择框的placeholder不显示
                        // this.repoBaseInfo.deploymentRepo = res.configuration.deploymentRepo || ''
                    }
                    // 远程仓库，添加地址，账号密码和网络代理相关配置
                    if (res.category === 'REMOTE') {
                        this.repoBaseInfo.url = res.configuration.url
                        this.repoBaseInfo.credentials = res.configuration.credentials

                        if (res.configuration.network.proxy === null) {
                            this.repoBaseInfo.network = {
                                proxy: {
                                    host: null,
                                    port: null,
                                    username: null,
                                    password: null
                                },
                                switcher: false
                            }
                        } else {
                            this.repoBaseInfo.network = {
                                proxy: res.configuration.network.proxy,
                                switcher: true
                            }
                        }
                        const { cache } = res.configuration

                        if (cache && cache.enabled !== undefined && cache.expiration !== undefined) {
                            this.repoBaseInfo.cache = {
                                enabled: cache.enabled,
                                expiration: cache.expiration
                            }
                        } else {
                            this.repoBaseInfo.cache = {
                                enabled: true,
                                expiration: -1
                            }
                        }
                    }
                    if (res.type === 'DOCKER' && (res.category === 'LOCAL' || res.category === 'REMOTE') && res.configuration.settings.defaultNamespace === 'library') {
                        this.repoBaseInfo.enabledLibraryNamespace = true
                    }

                    const { interceptors } = res.configuration.settings
                    if (interceptors instanceof Array) {
                        const filterInterceptors = this.handleInterceptors(interceptors)
                        filterInterceptors.forEach(i => {
                            this.repoBaseInfo[i.type.toLowerCase()] = {
                                enable: true,
                                ...i.rules
                            }
                        })
                    }
                }).finally(() => {
                    this.isLoading = false
                    // 不论接口返回数据是否成功，都需要显示tab标签页
                    this.showTabPanel = true
                })
            },
            handleInterceptors (arr) {
                let filterInterceptors = []
                const patternInterceptors = arr.filter(item => {
                    return item.type === 'PATH_PATTERN'
                })
                filterInterceptors = arr.filter(item => {
                    return item.type !== 'PATH_PATTERN'
                })
                try {
                    // 确保patternInterceptors存在且有内容
                    if (patternInterceptors?.length) {
                        const { includePathPatterns = [], excludePathPatterns = [] } = patternInterceptors[0].rules
                        this.repoBaseInfo.includesPath.splice(0, this.repoBaseInfo.includesPath.length)
                        this.repoBaseInfo.ignoresPath.splice(0, this.repoBaseInfo.ignoresPath.length)
                        includePathPatterns.forEach(item => {
                            this.repoBaseInfo.includesPath.push({
                                value: item
                            })
                        })
                        excludePathPatterns.forEach(item => {
                            this.repoBaseInfo.ignoresPath.push({
                                value: item
                            })
                        })
                    }
                } catch (error) {
                    console.error(error)
                }

                return filterInterceptors
            },
            async saveBaseInfo () {
                if (this.repoBaseInfo.network.switcher && this.errorProxyPortInfo) return
                await this.$refs.repoBaseInfo.validate()
                const interceptors = []
                if (this.repoType === 'generic') {
                    ['mobile', 'web'].forEach(type => {
                        const { enable, filename, metadata } = this.repoBaseInfo[type]
                        enable && interceptors.push({
                            type: type.toUpperCase(),
                            rules: { filename, metadata }
                        })
                    })
                }
                const patternsObj = {
                    type: 'PATH_PATTERN',
                    rules: {
                        includePathPatterns: this.repoBaseInfo.includesPath.map(item => {
                            return item.value
                        }),
                        excludePathPatterns: this.repoBaseInfo.ignoresPath.map(item => {
                            return item.value
                        })
                    }
                }
                if (patternsObj.rules.includePathPatterns.length || patternsObj.rules.excludePathPatterns.length) {
                    interceptors.push(patternsObj)
                }
                const body = {
                    public: this.repoBaseInfo.public,
                    description: this.repoBaseInfo.description,
                    configuration: {
                        ...this.repoBaseInfo.configuration,
                        settings: {
                            system: this.repoBaseInfo.system,
                            interceptors: interceptors.length ? interceptors : undefined,
                            ...(
                                this.repoType === 'rpm'
                                    ? {
                                        enabledFileLists: this.repoBaseInfo.enabledFileLists,
                                        repodataDepth: this.repoBaseInfo.repodataDepth,
                                        groupXmlSet: this.repoBaseInfo.groupXmlSet
                                    }
                                    : {}
                            )
                        }
                    }
                }
                if (this.repoType === 'maven' || this.repoType === 'npm') {
                    body.coverStrategy = !this.repoBaseInfo.override.switcher ? 'DISABLE' : this.repoBaseInfo.override.isFlag ? 'COVER' : 'UNCOVER'
                }
                // 远程仓库，此时需要添加 地址，账号密码和网络代理相关的配置
                if (this.repoBaseInfo.category === 'REMOTE') {
                    body.configuration.url = this.repoBaseInfo.url
                    body.configuration.network = {
                        proxy: null
                    }
                    if (checkValueIsNullOrEmpty(this.repoBaseInfo.credentials.username)
                        && checkValueIsNullOrEmpty(this.repoBaseInfo.credentials.password)) {
                        body.configuration.credentials = {
                            username: null,
                            password: null
                        }
                    } else {
                        body.configuration.credentials = this.repoBaseInfo.credentials
                    }
                    if (this.repoBaseInfo.network.switcher) {
                        body.configuration.network = {
                            proxy: this.repoBaseInfo.network.proxy
                        }
                        if (checkValueIsNullOrEmpty(this.repoBaseInfo.network.proxy?.username)
                            && checkValueIsNullOrEmpty(this.repoBaseInfo.network.proxy?.password)) {
                            body.configuration.network.proxy.username = null
                            body.configuration.network.proxy.password = null
                        }
                    }
                    if (this.repoBaseInfo.type === 'GO') {
                        body.configuration.cache = {
                            enabled: this.repoBaseInfo.cache.enabled,
                            expiration: this.repoBaseInfo.cache.expiration
                        }
                    }
                }
                // 虚拟仓库需要添加存储库相关配置
                if (this.repoBaseInfo.category === 'VIRTUAL') {
                    body.configuration.repositoryList = this.repoBaseInfo.virtualStoreList.map(item => {
                        return {
                            name: item.name,
                            category: item.category,
                            projectId: item.projectId
                        }
                    })
                    // body.configuration.deploymentRepo = this.repoBaseInfo.deploymentRepo
                }
                if (this.repoBaseInfo.enabledLibraryNamespace) {
                    body.configuration.settings.defaultNamespace = 'library'
                }

                this.repoBaseInfo.loading = true
                this.updateRepoInfo({
                    projectId: this.projectId,
                    name: this.repoName,
                    body
                }).then(() => {
                    this.getRepoInfoHandler()
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('save') + this.$t('space') + this.$t('success')
                    })
                }).finally(() => {
                    this.repoBaseInfo.loading = false
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.base-info-tab {
    ::v-deep .bk-tab-section {
        padding: 0;
        .bk-tab-content {
            height: 100%;
            display: flex;
            flex-direction: column;
            & > :last-child {
                padding: 20px;
                flex: 1;
                min-height: 0;
                overflow: auto;
            }
        }
    }
}
.repo-config-container {
    height: 100%;
    background-color: white;
    .repo-config-tab {
        height: 100%;
        ::v-deep .bk-tab-section {
            height: calc(100% - 60px);
            overflow-y: auto;
        }
        .repo-base-info {
            max-width: 966px;
        }
    }
}
.virtual-check-container{
    width: 96%;
}
</style>
