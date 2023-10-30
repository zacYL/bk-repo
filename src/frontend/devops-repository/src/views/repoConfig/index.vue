<template>
    <div class="repo-config-container" v-bkloading="{ isLoading }">
        <bk-tab v-if="showTabPanel" class="repo-config-tab page-tab" type="unborder-card" :active.sync="tabName">
            <bk-tab-panel name="baseInfo" :label="$t('repoBaseInfo')">
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
                            <bk-input style="width:400px" v-model.trim="repoBaseInfo.url"></bk-input>
                            <bk-button theme="primary" :disabled="disableTestUrl" @click="onClickTestRemoteUrl">{{ $t('testRemoteUrl') }}</bk-button>
                        </bk-form-item>
                        <bk-form-item :label="$t('remoteProxyAccount')" property="credentials.username" error-display-type="normal">
                            <bk-input style="width:400px" v-model.trim="repoBaseInfo.credentials.username"></bk-input>
                        </bk-form-item>
                        <bk-form-item :label="$t('remoteProxyPassword')" property="credentials.password" error-display-type="normal">
                            <bk-input style="width:400px" type="password" v-model.trim="repoBaseInfo.credentials.password"></bk-input>
                        </bk-form-item>
                        <bk-form-item :label="$t('networkProxy')" property="switcher">
                            <template>
                                <bk-switcher v-model="repoBaseInfo.network.switcher" theme="primary"></bk-switcher>
                                <span>{{repoBaseInfo.network.switcher ? $t('open') : $t('close')}}</span>
                            </template>
                        </bk-form-item>
                        <template v-if="repoBaseInfo.network.switcher">
                            <bk-form-item label="IP" property="network.proxy.host" :required="true" error-display-type="normal">
                                <bk-input style="width:400px" v-model.trim="repoBaseInfo.network.proxy.host"></bk-input>
                            </bk-form-item>
                            <bk-form-item :label="$t('port')" property="network.proxy.port" :required="true" error-display-type="normal">
                                <bk-input
                                    style="width:400px"
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
                                <bk-input style="width:400px" v-model.trim="repoBaseInfo.network.proxy.username"></bk-input>
                            </bk-form-item>
                            <bk-form-item :label="$t('networkPassword')" property="network.proxy.password">
                                <bk-input style="width:400px" type="password" v-model.trim="repoBaseInfo.network.proxy.password"></bk-input>
                            </bk-form-item>
                        </template>
                    </template>
                        
                    <template v-if="repoBaseInfo.category === 'VIRTUAL'">
                        <bk-form-item :label=" $t('select') + $t('storageStore')" property="virtualStoreList" :required="true" error-display-type="normal">
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
                        <!-- <bk-form-item :label="$t('uploadTargetStore')" property="deploymentRepo">
                            <bk-select
                                v-model="repoBaseInfo.deploymentRepo"
                                style="width:300px;"
                                :show-empty="false"
                                :placeholder="$t('pleaseSelect') + $t('uploadTargetStore')">
                                <bk-option v-for="item in deploymentRepoCheckList" :key="item.name" :id="item.name" :name="item.name">
                                </bk-option>
                                <div v-if="!deploymentRepoCheckList.length" class="form-tip mt10 ml10 mr10 mb10">
                                    {{$t('noAddedLocalStore')}}
                                </div>
                            </bk-select>
                            <div class="form-tip">{{$t('addPackagePrompt')}}</div>
                        </bk-form-item> -->
                    </template>
                    <bk-form-item label="访问权限">
                        <card-radio-group
                            v-model="available"
                            :list="availableList"
                            :disabled="repoBaseInfo.name === 'pipeline' || repoBaseInfo.name === 'report'"
                        >
                        </card-radio-group>
                    </bk-form-item>

                    <bk-form-item label="版本策略" v-if="!(repoBaseInfo.category === 'REMOTE') && !(repoBaseInfo.category === 'VIRTUAL') && (repoType === 'maven' || repoType === 'npm')">
                        <div class="flex-align-center">
                            <bk-switcher
                                v-model="repoBaseInfo.override.switcher"
                                size="small"
                                theme="primary"
                                @change="handleOverrideChange"
                            ></bk-switcher>
                            <span class="ml10">开启后上传同名称版本制品将会根据版本策略决定是否覆盖</span>
                        </div>
                        <bk-radio-group v-model="repoBaseInfo.override.isFlag" v-if="repoBaseInfo.override.switcher">
                            <bk-radio class="mr20" :value="false">不允许覆盖</bk-radio>
                            <bk-radio :value="true">允许覆盖</bk-radio>
                        </bk-radio-group>
                    </bk-form-item>

                    <template v-if="repoType === 'generic'">
                        <!-- <bk-form-item v-for="type in ['mobile', 'web']" :key="type" -->
                        <bk-form-item v-for="type in ['web']" :key="type"
                            :label="$t(`${type}Download`)" :property="`${type}.enable`">
                            <bk-radio-group v-model="repoBaseInfo[type].enable">
                                <bk-radio class="mr20" :value="true">{{ $t('open') }}</bk-radio>
                                <bk-radio :value="false">{{ $t('close') }}</bk-radio>
                            </bk-radio-group>
                            <template v-if="repoBaseInfo[type].enable">
                                <bk-form-item :label="$t('fileName')" :label-width="60" class="mt10"
                                    :property="`${type}.filename`" required error-display-type="normal">
                                    <bk-input class="w250" v-model.trim="repoBaseInfo[type].filename" :placeholder="$t('fileNameRule')"></bk-input>
                                </bk-form-item>
                                <bk-form-item :label="$t('metadata')" :label-width="60"
                                    :property="`${type}.metadata`" required error-display-type="normal">
                                    <bk-input class="w250" v-model.trim="repoBaseInfo[type].metadata" :placeholder="$t('metadataRule')"></bk-input>
                                </bk-form-item>
                            </template>
                        </bk-form-item>
                    </template>
                    <template v-if="!(repoBaseInfo.category === 'REMOTE') && !(repoBaseInfo.category === 'VIRTUAL') && repoType === 'rpm'">
                        <bk-form-item :label="$t('enabledFileLists')">
                            <bk-checkbox v-model="repoBaseInfo.enabledFileLists"></bk-checkbox>
                        </bk-form-item>
                        <bk-form-item :label="$t('repodataDepth')" property="repodataDepth" error-display-type="normal">
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
                            :placeholder="$t('repoDescriptionPlacehodler')">
                        </bk-input>
                    </bk-form-item>
                    <bk-form-item>
                        <bk-button :loading="repoBaseInfo.loading" theme="primary" @click="saveBaseInfo">{{$t('save')}}</bk-button>
                    </bk-form-item>
                </bk-form>
            </bk-tab-panel>
            <bk-tab-panel v-if="showProxyConfigTab" name="proxyConfig" :label="$t('proxyConfig')">
                <proxy-config :base-data="repoBaseInfo" @refresh="getRepoInfoHandler"></proxy-config>
            </bk-tab-panel>
            <bk-tab-panel v-if="showCleanConfigTab" render-directive="if" name="cleanConfig" label="清理设置">
                <clean-config :base-data="repoBaseInfo" @refresh="getRepoInfoHandler"></clean-config>
            </bk-tab-panel>
            <bk-tab-panel render-directive="if" name="permissionConfig" :label="$t('permissionConfig')">
                <permission-config :category="repoBaseInfo.category"></permission-config>
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
    import permissionConfig from './permissionConfig'
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
                    // 虚拟仓库的选中的存储库列表
                    virtualStoreList: [],
                    // deploymentRepo: '', // 虚拟仓库中选择存储的本地仓库
                    // 是否展示tab标签页，因为代理设置和清理设置需要根据详情页接口返回的数据判断是否显示，解决异步导致的tab顺序错误的问题
                    showTabPanel: false
                },
                disableTestUrl: false,
                errorProxyPortInfo: false
            }
        },
        computed: {
            ...mapState(['domain']),
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
                    { label: '项目内公开', value: 'project', tip: '项目内成员可以使用' },
                    { label: '系统内公开', value: 'system', tip: '系统内成员可以使用' },
                    { label: '可匿名下载', value: 'public', tip: '不鉴权，任意终端都可下载' }
                ]
            },
            rules () {
                const filenameRule = [
                    {
                        required: true,
                        message: this.$t('pleaseFileName'),
                        trigger: 'blur'
                    }
                ]
                const metadataRule = [
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
                // 远程仓库的 地址校验规则
                const urlRule = [
                    {
                        required: true,
                        message: this.$t('pleaseInput') + this.$t('address'),
                        trigger: 'blur'
                    },
                    {
                        validator: this.checkRemoteUrl,
                        message: this.$t('pleaseInput') + this.$t('legit') + this.$t('address'),
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
                        message: this.$t('pleaseInput') + this.$t('networkProxy') + 'IP',
                        trigger: 'blur'
                    }
                ]
                const proxyPortRule = [
                    {
                        required: true,
                        message: this.$t('pleaseInput') + this.$t('networkProxy') + this.$t('port'),
                        trigger: 'blur'
                    }
                ]
                // 虚拟仓库下选择存储库的校验
                const checkStorageRule = [
                    {
                        required: true,
                        message: this.$t('noSelectStorageStore') + this.$t('save'),
                        trigger: 'blur'
                    }
                ]
                return {
                    repodataDepth: [
                        {
                            regex: /^(0|[1-9][0-9]*)$/,
                            message: this.$t('pleaseInput') + this.$t('legit') + this.$t('repodataDepth'),
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
                            message: this.$t('pleaseInput') + this.$t('legit') + this.$t('groupXmlSet') + `(.xml${this.$t('type')})`,
                            trigger: 'change'
                        }
                    ],
                    'mobile.filename': filenameRule,
                    'mobile.metadata': metadataRule,
                    'web.filename': filenameRule,
                    'web.metadata': metadataRule,
                    // 远程仓库才应该有地址的校验
                    url: this.repoBaseInfo.category === 'REMOTE' ? urlRule : {},
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
            // deploymentRepoCheckList: {
            //     handler (val) {
            //         // 当选中的存储库中没有本地仓库或者当前选中的上传目标仓库不在被选中的存储库中时需要将当前选中的上传目标仓库重置为空
            //         if (!val.length || !(val.map((item) => item.name).includes(this.repoBaseInfo.deploymentRepo))) {
            //             this.repoBaseInfo.deploymentRepo = ''
            //         }
            //     }
            // }
            // 当网络代理关闭时需要将判断端口输入框的输入是否符合规范的错误提示是否出现重置为 false，否则会导致再次打开出现错误提示
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
            handleOverrideChange (isFlag) {
                this.repoBaseInfo.override.switcher = isFlag
            },
            toRepoList () {
                this.$router.push({
                    name: 'repoList'
                })
            },
            checkRemoteUrl (val) {
                const reg = /^https?:\/\/(([a-zA-Z0-9_-])+(\.)?)*(:\d+)?(\/((\.)?(\?)?=?&?[a-zA-Z0-9_-](\?)?)*)*$/
                return reg.test(val)
            },
            // 校验当前输入的远程代理源地址是否是当前仓库
            checkRemoteRepeatUrl (val) {
                const originHref = window.location.origin
                const urlSplicing = originHref + '/' + this.repoBaseInfo.type?.toLowerCase() + '/' + this.projectId + '/' + this.repoBaseInfo.name
                const urlSplicingSpecial = urlSplicing + '/'
                return !(urlSplicing === val || urlSplicingSpecial === val)
            },
            // 创建远程仓库弹窗中测试远程链接
            onClickTestRemoteUrl () {
                if (this.repoBaseInfo.network.switcher && (!this.repoBaseInfo.network.proxy.host || !this.repoBaseInfo.network.proxy.port)) {
                    this.$bkMessage({
                        theme: 'warning',
                        limit: 3,
                        message: this.$t('pleaseInput') + this.$t('legit') + this.$t('networkProxy')
                    })
                    return
                }
                if (this.repoBaseInfo.network.switcher && this.errorProxyPortInfo) return
                if (!this.repoBaseInfo?.url || isEmpty(this.repoBaseInfo.url) || !this.checkRemoteUrl(this.repoBaseInfo?.url)) {
                    this.$bkMessage({
                        theme: 'warning',
                        limit: 3,
                        message: this.$t('pleaseInput') + this.$t('legit') + this.$t('address')
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
                                message: this.$t('connectFailed') + `: ${res.message}`
                            })
                        }
                    }).finally(() => {
                        this.disableTestUrl = false
                    })
                }
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
                    }

                    const { interceptors } = res.configuration.settings
                    if (interceptors instanceof Array) {
                        interceptors.forEach(i => {
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
                this.repoBaseInfo.loading = true
                this.updateRepoInfo({
                    projectId: this.projectId,
                    name: this.repoName,
                    body
                }).then(() => {
                    this.getRepoInfoHandler()
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('save') + this.$t('success')
                    })
                }).finally(() => {
                    this.repoBaseInfo.loading = false
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
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
            max-width: 800px;
        }
    }
}
.virtual-check-container{
    width: 96%;
}
</style>
