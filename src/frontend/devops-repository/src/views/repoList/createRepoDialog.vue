<template>
    <canway-dialog
        v-model="show"
        :width="currentLanguage === 'zh-cn' ? 810 : 1006"
        height-num="770"
        :title="title"
        @cancel="cancel">
        <bk-alert
            v-if="tipsObj"
            :style="{
                marginLeft: currentLanguage === 'zh-cn' ? '82px' : '60px',
                width: `calc(100% - ${currentLanguage === 'zh-cn' ? '82px' : '60px'})`
            }"
            class="mb20"
            type="warning"
            :title="tipsObj.tips">
        </bk-alert>
        <bk-form class="mr10 repo-base-info" :label-width="150" :model="repoBaseInfo" :rules="rules" ref="repoBaseInfo">
            <bk-form-item :label="$t('repoType')" :required="true" property="type" error-display-type="normal">
                <bk-radio-group v-model="repoBaseInfo.type" class="repo-type-radio-group" @change="changeRepoType">
                    <bk-radio-button v-for="repo in filterRepoEnum" :key="repo.label" :value="repo.value">
                        <div class="flex-column flex-center repo-type-radio" :class="{ 'checked': repo.value === repoBaseInfo.type }">
                            <Icon size="32" :name="repo.value" />
                            <span>{{repo.label}}</span>
                            <span v-show="repo.value === repoBaseInfo.type" class="top-right-selected">
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
                    <!-- todo 测试链接暂未支持 -->
                    <bk-button v-if="repoBaseInfo.type !== 'generic'" theme="primary" :disabled="disableTestUrl" @click="onClickTestRemoteUrl">{{ $t('testRemoteUrl') }}</bk-button>
                </bk-form-item>
                <template v-if="repoBaseInfo.type === 'cocoapods'">
                    <!-- 远程仓库类型 -->
                    <bk-form-item :label="$t('remoteRepoType')" property="remoteType" error-display-type="normal">
                        <bk-select
                            v-model="repoBaseInfo.remoteType"
                            class="w250">
                            <bk-option v-for="type in remoteRepoTypes" :key="type.value" :id="type.value" :name="type.label">
                                <div class="flex-align-center">
                                    <span class="ml10 flex-1 text-overflow">{{type.label}}</span>
                                </div>
                            </bk-option>
                        </bk-select>
                    </bk-form-item>
                    <!-- specs 下载地址 -->
                    <bk-form-item :label="$t('specsDownloadUrl')"
                        :required="repoBaseInfo.remoteType === 'OTHER'"
                        property="downloadUrl" error-display-type="normal"
                        :desc="{
                            content: `<div>
                                <p>${$t('specsDownloadUrlTips1')}</p>
                                <p>${$t('specsDownloadUrlTips2')}</p>
                                <p>${$t('specsDownloadUrlTips3')}</p>
                            </div>`
                        }"
                        desc-type="icon">
                        <bk-input class="w480" v-model.trim="repoBaseInfo.downloadUrl"></bk-input>
                    </bk-form-item>
                </template>
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
                </div>
                <bk-form-item v-show="['go'].includes(repoBaseInfo.type) && storeType === 'remote'" :label="$t('cache')" property="switcher">
                    <template>
                        <bk-switcher v-model="repoBaseInfo.cache.enabled" theme="primary"></bk-switcher>
                        <span>{{repoBaseInfo.cache.enabled ? $t('open') : $t('close')}}</span>
                    </template>
                </bk-form-item>
                <div v-if="repoBaseInfo.cache.enabled && ['go'].includes(repoBaseInfo.type)" class="pt20 pb20">
                    <bk-form-item :label="$t('expiration')" property="cache.expiration" :required="true" :rules="rules.cache" error-display-type="normal">
                        <bk-input :placeholder="$t('cacheExpirationPlaceholder')" class="w480" type="number" v-model.trim="repoBaseInfo.cache.expiration"></bk-input>
                    </bk-form-item>
                </div>
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
            </template>
            <template v-if="!['generic', 'composer'].includes(repoBaseInfo.type)">
                <bk-form-item
                    :label="$t('includePath')">
                    <bk-button @click="addPath('includesPath')" class="mr5">{{ $t("addPath") }}</bk-button>
                    <bk-icon-plus type="plus-hint" v-if="showIncludesPathDesc" v-bk-tooltips="includesPathDesc" />
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
                    :list="availableList">
                </card-radio-group>
            </bk-form-item>
            <template v-if="repoBaseInfo.type === 'generic' && storeType === 'local'">
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
            <template v-if="repoBaseInfo.type === 'docker' && (storeType === 'local' || storeType === 'remote')">
                <bk-form-item :label="$t('enabledLibraryNamespace')">
                    <bk-checkbox v-model="repoBaseInfo.enabledLibraryNamespace"></bk-checkbox>
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
    import { repoEnum, repoSupportEnum, remoteRepoSupportEnum } from '@repository/store/publicEnum'
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
            enabledLibraryNamespace: false,
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
            cache: {
                enabled: true,
                expiration: 120
            },
            // 虚拟仓库的选中的存储库列表
            virtualStoreList: [],
            // deploymentRepo: '' // 虚拟仓库中选择存储的本地仓库
            includesPath: [],
            ignoresPath: [],
            downloadUrl: '', // specs下载地址
            remoteType: 'GIT_HUB'
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
                errorProxyPortInfo: false,
                showIncludesPathDesc: true
            }
        },
        computed: {
            ...mapState(['domain']),
            remoteRepoTypes () {
                return [
                    {
                        label: 'GitHub',
                        value: 'GIT_HUB'
                    },
                    {
                        label: 'bkrepo',
                        value: 'CPACK'
                    },
                    {
                        label: this.$t('other'),
                        value: 'OTHER'
                    }
                ]
            },
            projectId () {
                return this.$route.params.projectId
            },
            tipsObj () {
                if (this.protocol === 'http' && ['go'].includes(this.repoBaseInfo.type) && ['project', 'system'].includes(this.available)) {
                    return {
                        tips: this.$t('goWarnTips')
                    }
                }
                return ''
            },
            protocol () {
                return window.location.protocol.replace(':', '')
            },
            includesPathDesc () {
                return {
                    allowHtml: true,
                    content: '1',
                    html: `<p>${this.$t('includesPathDesc1')}</p>
                        <p>${this.$t('includesPathDesc2')}</p>
                        <p>${this.$t('includesPathDesc3')}</p>
                        <p>${this.$t('includesPathDesc4')}</p>
                        ${this.repoBaseInfo.type === 'maven'
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
                const metadataRule = this.storeType !== 'remote'
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
                        message: this.$t('noSelectStorageStore') + this.$t('space') + this.$t('create') + this.$t('space') + this.$t('virtualStore'),
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
                    ignoresPath: ignoresPathRule,
                    includesPath: includesPathRule,
                    'mobile.filename': filenameRule,
                    'mobile.metadata': metadataRule,
                    'web.filename': filenameRule,
                    'web.metadata': metadataRule,
                    cache: (this.storeType === 'remote' && ['go'].includes(this.repoBaseInfo.type)) ? cacheRule : {},
                    // 远程仓库才应该有地址的校验
                    url: this.storeType === 'remote' ? urlRule : {},
                    // 远程仓库且开启网络代理才应该设置代理的IP和端口的校验
                    'network.proxy.host': (this.storeType === 'remote' && this.repoBaseInfo.network.switcher) ? proxyHostRule : {},
                    'network.proxy.port': (this.storeType === 'remote' && this.repoBaseInfo.network.switcher) ? proxyPortRule : {},
                    // 虚拟仓库的选择存储库的校验
                    virtualStoreList: this.storeType === 'virtual' ? checkStorageRule : {},
                    // 为远程仓库且仓库类型为cocoapods且远程仓库类型为其他才设置下载地址的校验
                    ...(
                        this.repoBaseInfo.type === 'cocoapods'
                        && this.storeType === 'remote'
                        && this.repoBaseInfo.remoteType === 'OTHER'
                    )
                        ? {
                            downloadUrl: [
                                {
                                    required: true,
                                    message: this.$t('pleaseInput') + this.$t('space') + this.$t('specsDownloadUrl'),
                                    trigger: 'blur'
                                }
                            ]
                        }
                        : {}
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
                    // 因为可能支持创建的远程及虚拟仓库，在本地仓库支持创建的仓库中不存在，所以需要两者匹配才能在创建远程及虚拟仓库时显示
                    this.filterRepoEnum = val === 'local'
                        ? repoEnum
                        : (val === 'remote' ? remoteRepoSupportEnum : repoSupportEnum).map((item) => repoEnum.find((st) => item.value === st.value))
                    // 因为远程仓库和虚拟仓库没有generic类型且远程仓库支持的制品类型有限，所以需要将其重新赋默认值
                    this.repoBaseInfo.type = this.filterRepoEnum[0]?.value || ''
                }
            },
            'repoBaseInfo.type': {
                // 当选择的仓库类型改变时需要将选择的存储库重置为空
                handler () {
                    this.repoBaseInfo.virtualStoreList = []
                    this.showIncludesPathDesc = false
                    this.$nextTick(() => {
                        this.showIncludesPathDesc = true
                        this.initGoRemoteDefaultUrl()
                    })
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
            this.$nextTick(() => {
                this.initGoRemoteDefaultUrl()
            })
        },
        methods: {
            ...mapActions(['createRepo', 'checkRepoName', 'testRemoteUrl', 'getDomain']),
            // 初始化go远程仓库默认路径
            initGoRemoteDefaultUrl () {
                if (this.storeType === 'remote') {
                    this.repoBaseInfo.url = this.repoBaseInfo.type === 'go' ? 'https://goproxy.cn' : ''
                }
            },
            addPath (key) {
                this.repoBaseInfo[key].push({ value: '' })
            },
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
                            ),
                            ...(
                                (this.repoBaseInfo.type === 'cocoapods'
                                    && this.storeType === 'remote')
                                    ? {
                                        downloadUrl: this.repoBaseInfo.downloadUrl,
                                        type: this.repoBaseInfo.remoteType
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
                    if (this.repoBaseInfo.type === 'go') {
                        body.configuration.cache = {
                            enabled: this.repoBaseInfo.cache.enabled,
                            expiration: this.repoBaseInfo.cache.expiration
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
                if (this.repoBaseInfo.enabledLibraryNamespace) {
                    body.configuration.settings.defaultNamespace = 'library'
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
            // 宽高需要加 2px ,因为上下各有 1px的 border
            width: 82px;
            height: 62px;
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
