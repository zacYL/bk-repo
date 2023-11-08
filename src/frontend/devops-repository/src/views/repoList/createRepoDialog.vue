<template>
    <canway-dialog
        v-model="show"
        :width="currentLanguage === 'zh-cn' ? 810 : 1006"
        height-num="770"
        :title="title"
        @cancel="cancel">
        <bk-form class="mr10 repo-base-info" :label-width="150" :model="repoBaseInfo" :rules="rules" ref="repoBaseInfo">
            <bk-form-item :label="$t('repoType')" :required="true" property="type" error-display-type="normal">
                <bk-radio-group v-model="repoBaseInfo.type" class="repo-type-radio-group" @change="changeRepoType">
                    <bk-radio-button v-for="repo in filterRepoEnum" :key="repo" :value="repo">
                        <div class="flex-column flex-center repo-type-radio" :class="{ 'checked': repo === repoBaseInfo.type }">
                            <Icon size="32" :name="repo" />
                            <span>{{repo}}</span>
                            <span v-show="repo === repoBaseInfo.type" class="top-right-selected">
                                <i class="devops-icon icon-check-1"></i>
                            </span>
                        </div>
                    </bk-radio-button>
                </bk-radio-group>
            </bk-form-item>
            <bk-form-item :label="$t('repoName')" :required="true" property="name" error-display-type="normal">
                <bk-input class="w480" v-model.trim="repoBaseInfo.name" maxlength="32" show-word-limit
                    :placeholder="$t(repoBaseInfo.type === 'docker' ? 'repoDockerNamePlaceholder' : 'repoNamePlaceholder')">
                </bk-input>
                <div v-if="repoBaseInfo.type === 'docker'" class="form-tip">{{ $t('dockerRepoTip')}}</div>
            </bk-form-item>
            <template v-if="storeType === 'remote'">
                <bk-form-item :label="$t('remoteProxyAddress')" :required="true" property="url" error-display-type="normal">
                    <bk-input class="w480" v-model.trim="repoBaseInfo.url"></bk-input>
                    <bk-button theme="primary" :disabled="disableTestUrl" @click="onClickTestRemoteUrl">{{ $t('testRemoteUrl') }}</bk-button>
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
                <template v-if="repoBaseInfo.network.switcher">
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
                            @focus="errorProxyPortInfo = false"
                        ></bk-input>
                        <p class="form-error-tip" v-if="errorProxyPortInfo">{{$t('repositoryProxyPortInfo')}}</p>
                    </bk-form-item>
                    <bk-form-item :label="$t('networkAccount')" property="network.proxy.username">
                        <bk-input class="w480" v-model.trim="repoBaseInfo.network.proxy.username"></bk-input>
                    </bk-form-item>
                    <bk-form-item :label="$t('networkPassword')" property="network.proxy.password">
                        <bk-input class="w480" type="password" v-model.trim="repoBaseInfo.network.proxy.password"></bk-input>
                    </bk-form-item>
                </template>
            </template>

            <template v-if="storeType === 'virtual'">
                <bk-form-item :label="$t('select') + $t('space') + $t('storageStore')" property="virtualStoreList" :required="true" error-display-type="normal">
                    <bk-button class="mb10" hover-theme="primary" @click="toCheckedStore">{{ $t('pleaseSelect') }}</bk-button>
                    <div class="virtual-check-container">
                        <store-sort
                            v-if="repoBaseInfo.virtualStoreList.length"
                            :key="repoBaseInfo.virtualStoreList"
                            ref="storeSortRef"
                            :sort-list="repoBaseInfo.virtualStoreList">
                        </store-sort>
                    </div>
                </bk-form-item>
                <!-- <bk-form-item :label="$t('uploadTargetStore')" property="uploadTargetStore">
                    <bk-select
                        v-model="repoBaseInfo.deploymentRepo"
                        style="width:300px;"
                        :show-empty="false"
                        :placeholder="$t('pleaseSelect') + $t('space') + $t('uploadTargetStore')">
                        <bk-option v-for="item in deploymentRepoCheckList" :key="item.name" :id="item.name" :name="item.name">
                        </bk-option>
                        <div v-if="!deploymentRepoCheckList.length" class="form-tip mt10 ml10 mr10 mb10">
                            {{$t('noAddedLocalStore')}}
                        </div>
                    </bk-select>
                    <div class="form-tip">{{$t('addPackagePrompt')}}</div>
                </bk-form-item> -->
            </template>
            <bk-form-item :label="$t('accessPermission')">
                <card-radio-group
                    v-model="available"
                    :list="availableList">
                </card-radio-group>
            </bk-form-item>
            <template v-if="repoBaseInfo.type === 'generic'">
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
                            <bk-input class="w250" v-model.trim="repoBaseInfo[type].filename" :placeholder="$t('fileNameRule')"></bk-input>
                        </bk-form-item>
                        <bk-form-item :label="$t('metadata')" :label-width="80"
                            :property="`${type}.metadata`" required error-display-type="normal">
                            <bk-input class="w250" v-model.trim="repoBaseInfo[type].metadata" :placeholder="$t('metadataRule')"></bk-input>
                        </bk-form-item>
                    </template>
                </bk-form-item>
            </template>
            <template v-if="!(storeType === 'remote') && !(storeType === 'virtual') && repoBaseInfo.type === 'rpm'">
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

            <bk-form-item :label="$t('versionStrategy')" v-if="!(storeType === 'remote') && !(storeType === 'virtual') && (repoBaseInfo.type === 'maven' || repoBaseInfo.type === 'npm')">
                <div class="flex-align-center">
                    <bk-switcher
                        v-model="repoBaseInfo.override.switcher"
                        size="small"
                        theme="primary"
                        @change="handleOverrideChange"
                    ></bk-switcher>
                    <span class="ml10" style="width:95%">{{$t('coverStrategyInfo')}}</span>
                </div>
                <bk-radio-group v-model="repoBaseInfo.override.isFlag" v-if="repoBaseInfo.override.switcher">
                    <bk-radio class="mr20" :value="false">{{$t('notAllowCover')}}</bk-radio>
                    <bk-radio :value="true">{{$t('allowCover')}}</bk-radio>
                </bk-radio-group>
            </bk-form-item>

            <bk-form-item :label="$t('description')">
                <bk-input type="textarea"
                    class="w480"
                    maxlength="200"
                    :rows="6"
                    v-model.trim="repoBaseInfo.description"
                    :placeholder="$t('repoDescriptionPlaceholder')">
                </bk-input>
            </bk-form-item>
        </bk-form>
        <template #footer>
            <bk-button @click="cancel">{{$t('cancel')}}</bk-button>
            <bk-button class="ml10" :loading="loading" theme="primary" @click="confirm">{{$t('confirm')}}</bk-button>
        </template>
        <check-target-store
            ref="checkTargetStoreRef"
            :repo-type="repoBaseInfo.type"
            :check-list="repoBaseInfo.virtualStoreList"
            @checkedTarget="onCheckedTargetStore">
        </check-target-store>
    </canway-dialog>
</template>
<script>
    import CardRadioGroup from '@repository/components/CardRadioGroup'
    import StoreSort from '@repository/components/StoreSort'
    import CheckTargetStore from '@repository/components/CheckTargetStore'
    import { repoEnum, repoSupportEnum } from '@repository/store/publicEnum'
    import { mapActions, mapState } from 'vuex'
    import { isEmpty } from 'lodash'
    import { checkValueIsNullOrEmpty } from '@repository/utils'
    const getRepoBaseInfo = () => {
        return {
            type: 'generic',
            category: 'LOCAL',
            name: '',
            public: false,
            system: false,
            enabledFileLists: false,
            repodataDepth: 0,
            interceptors: [],
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
                switcher: false,
                proxy: {
                    host: null,
                    port: null,
                    username: null,
                    password: null
                }
            },
            // 虚拟仓库的选中的存储库列表
            virtualStoreList: []
            // deploymentRepo: '' // 虚拟仓库中选择存储的本地仓库
        }
    }

    export default {
        name: 'createRepo',
        components: { CardRadioGroup, CheckTargetStore, StoreSort },
        props: {
            // 当前仓库类型ID
            storeType: {
                type: String,
                default: 'local'
            }
        },
        data () {
            return {
                show: false,
                loading: false,
                repoBaseInfo: getRepoBaseInfo(),
                // 因为创建仓库时拆分为本地/远程/虚拟，远程仓库和虚拟仓库没有generic选项，所以需要重新组合
                filterRepoEnum: repoEnum,
                disableTestUrl: false, // 远程仓库中测试链接按钮是否被禁用
                errorProxyPortInfo: false
            }
        },
        computed: {
            ...mapState(['domain']),
            projectId () {
                return this.$route.params.projectId
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
                        message: this.$t('noSelectStorageStore') + this.$t('space') + this.$t('create') + this.$t('space') + this.$t('virtualStore'),
                        trigger: 'blur'
                    }
                ]
                return {
                    type: [
                        {
                            required: true,
                            message: this.$t('pleaseSelect') + this.$t('space') + this.$t('repoType'),
                            trigger: 'blur'
                        }
                    ],
                    name: [
                        {
                            required: true,
                            message: this.$t('pleaseInput') + this.$t('space') + this.$t('repoName'),
                            trigger: 'blur'
                        },
                        {
                            regex: this.repoBaseInfo.type === 'docker' ? /^[a-z][a-z0-9\-_]{1,31}$/ : /^[a-zA-Z][a-zA-Z0-9\-_]{1,31}$/,
                            message: this.$t(this.repoBaseInfo.type === 'docker' ? 'repoDockerNamePlaceholder' : 'repoNamePlaceholder'),
                            trigger: 'blur'
                        },
                        {
                            validator: str => {
                                // 集成CI模式下不允许创建名称为 log 的 generic 仓库
                                return (MODE_CONFIG === 'ci' && this.repoBaseInfo.type === 'generic') ? !/^log$/.test(str) : true
                            },
                            message: this.$t('createLogStoreErrorTips'),
                            trigger: 'blur'
                        },
                        {
                            validator: this.asynCheckRepoName,
                            message: this.$t('repoName') + this.$t('space') + this.$t('exist'),
                            trigger: 'blur'
                        }
                    ],
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
                    'mobile.filename': filenameRule,
                    'mobile.metadata': metadataRule,
                    'web.filename': filenameRule,
                    'web.metadata': metadataRule,
                    // 远程仓库才应该有地址的校验
                    url: this.storeType === 'remote' ? urlRule : {},
                    // 远程仓库且开启网络代理才应该设置代理的IP和端口的校验
                    'network.proxy.host': (this.storeType === 'remote' && this.repoBaseInfo.network.switcher) ? proxyHostRule : {},
                    'network.proxy.port': (this.storeType === 'remote' && this.repoBaseInfo.network.switcher) ? proxyPortRule : {},
                    // 虚拟仓库的选择存储库的校验
                    virtualStoreList: this.storeType === 'virtual' ? checkStorageRule : {}
                }
            },
            available: {
                get () {
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
            // 弹窗标题
            title () {
                return this.$t('create') + this.$t('space') + this.$t('space') + this.$t('space') + this.$t(this.storeType + 'Store')
            }
            // 虚拟仓库中选择上传的目标仓库的下拉列表数据
            // deploymentRepoCheckList () {
            //     return this.repoBaseInfo.virtualStoreList.filter(item => item.category === 'LOCAL')
            // }
        },
        watch: {
            storeType: {
                handler (val) {
                    //  远程及虚拟仓库，目前只支持maven、npm、pypi、nuget四种仓库
                    this.filterRepoEnum = val === 'local' ? repoEnum : repoEnum.filter(item => repoSupportEnum.includes(item))
                    // 因为远程仓库和虚拟仓库没有generic类型且远程仓库支持的制品类型有限，所以需要将其重新赋默认值
                    this.repoBaseInfo.type = this.filterRepoEnum[0] || ''
                }
            },
            // deploymentRepoCheckList: {
            //     handler (val) {
            //         // 当选中的存储库中没有本地仓库或者当前选中的上传目标仓库不在被选中的存储库中时需要将当前选中的上传目标仓库重置为空
            //         if (!val.length || !(val.map((item) => item.name).includes(this.repoBaseInfo.deploymentRepo))) {
            //             this.repoBaseInfo.deploymentRepo = ''
            //         }
            //     }
            // },
            'repoBaseInfo.type': {
                // 当选择的仓库类型改变时需要将选择的存储库重置为空
                handler () {
                    this.repoBaseInfo.virtualStoreList = []
                }
            },
            // 当网络代理关闭时需要将判断端口输入框的输入是否符合规范的错误提示是否出现重置为 false，否则会导致再次打开出现错误提示
            'repoBaseInfo.network.switcher' (val) {
                !val && (this.errorProxyPortInfo = false)
            }
        },
        created () {
            // 此时需要获取docker的域名，因为创建docker的远程仓库时需要用到，用户不能手动输入当前仓库地址
            this.getDomain('docker')
        },
        methods: {
            ...mapActions(['createRepo', 'checkRepoName', 'testRemoteUrl', 'getDomain']),
            // 打开选择存储库弹窗
            toCheckedStore () {
                this.$refs.checkTargetStoreRef && (this.$refs.checkTargetStoreRef.show = true)
            },
            showDialogHandler () {
                this.show = true
                this.repoBaseInfo = getRepoBaseInfo()
                this.$refs.repoBaseInfo && this.$refs.repoBaseInfo.clearError()
            },
            cancel () {
                this.show = false
                this.$emit('close')
            },
            handleOverrideChange (isFlag) {
                this.repoBaseInfo.override.switcher = isFlag
            },
            asynCheckRepoName () {
                return this.checkRepoName({
                    projectId: this.projectId,
                    name: this.repoBaseInfo.name
                }).then(res => !res)
            },
            changeRepoType () {
                if (this.repoBaseInfo.type === 'docker') this.repoBaseInfo.name = ''
                this.$refs.repoBaseInfo.clearError()
            },
            checkRemoteUrl (val) {
                const reg = /^https?:\/\/(([a-zA-Z0-9_-])+(\.)?)*(:\d+)?(\/((\.)?(\?)?=?&?[a-zA-Z0-9_-](\?)?)*)*$/
                return reg?.test(val)
            },
            // 校验当前输入的远程代理源地址是否是当前仓库
            checkRemoteRepeatUrl (val) {
                let urlSplicing = ''
                if (this.repoBaseInfo.type === 'docker') {
                    urlSplicing = `${location.protocol}//${this.domain.docker}/${this.projectId}/${this.repoBaseInfo.name}`
                } else {
                    const originHref = window.location.origin
                    urlSplicing = originHref + '/' + this.repoBaseInfo.type + '/' + this.projectId + '/' + this.repoBaseInfo.name
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
            onBlurProxyPort () {
                this.errorProxyPortInfo = isNaN(Number(this.repoBaseInfo.network.proxy.port))
            },
            // 选中的存储库弹窗确认事件
            onCheckedTargetStore (list) {
                this.repoBaseInfo.virtualStoreList = list
            },
            async confirm () {
                if (this.repoBaseInfo.network.switcher && this.errorProxyPortInfo) return
                await this.$refs.repoBaseInfo.validate()
                const interceptors = []
                if (this.repoBaseInfo.type === 'generic') {
                    ['mobile', 'web'].forEach(type => {
                        const { enable, filename, metadata } = this.repoBaseInfo[type]
                        enable && interceptors.push({
                            type: type.toUpperCase(),
                            rules: { filename, metadata }
                        })
                    })
                }
                this.loading = true
                const body = {
                    projectId: this.projectId,
                    type: this.repoBaseInfo.type.toUpperCase(),
                    name: this.repoBaseInfo.name,
                    public: this.repoBaseInfo.public,
                    description: this.repoBaseInfo.description,
                    category: this.storeType.toUpperCase() || 'COMPOSITE',
                    configuration: {
                        type: this.storeType || 'composite',
                        settings: {
                            system: this.repoBaseInfo.system,
                            interceptors: interceptors.length ? interceptors : undefined,
                            ...(
                                this.repoBaseInfo.type === 'rpm'
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
                if (body.type === 'MAVEN' || body.type === 'NPM') {
                    body.coverStrategy = !this.repoBaseInfo.override.switcher ? 'DISABLE' : this.repoBaseInfo.override.isFlag ? 'COVER' : 'UNCOVER'
                }
                // 远程仓库，此时需要添加 地址，账号密码和网络代理相关的配置
                if (this.storeType === 'remote') {
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
                if (this.storeType === 'virtual') {
                    body.configuration.repositoryList = this.repoBaseInfo.virtualStoreList.map(item => {
                        return {
                            name: item.name,
                            category: item.category,
                            projectId: item.projectId
                        }
                    })
                    // body.configuration.deploymentRepo = this.repoBaseInfo.deploymentRepo || ''
                }
                this.createRepo({ body }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('create') + this.$t('space') + this.$t('repository') + this.$t('space') + this.$t('success')
                    })
                    this.cancel()
                    this.$emit('refresh')
                }).finally(() => {
                    this.loading = false
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.repo-base-info {
    max-height: 442px;
    overflow-y: auto;
    .repo-type-radio-group {
        display: grid;
        grid-template: auto / repeat(6, 80px);
        gap: 20px;
        ::v-deep .bk-form-radio-button {
            .bk-radio-button-text {
                height: auto;
                line-height: initial;
                padding: 0;
                border-radius: 2px;
            }
        }
        .repo-type-radio {
            position: relative;
            padding: 5px;
            width: 80px;
            height: 60px;
             &.checked {
                background-color: var(--bgHoverLighterColor);
                color: var(--primaryColor) ;
            }
            .top-right-selected {
                position: absolute;
                top: 0;
                right: 0;
                border-width: 16px;
                border-style: solid;
                border-color: var(--primaryColor) var(--primaryColor) transparent transparent;
                i {
                    position: absolute;
                    margin-top: -12px;
                    color: #fff;
                }
            }
        }
    }
}
.virtual-check-container{
    width: 96%;
}
</style>
