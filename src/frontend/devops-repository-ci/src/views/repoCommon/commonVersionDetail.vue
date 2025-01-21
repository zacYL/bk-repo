<template>
    <bk-tab class="common-version-container" type="unborder-card" :active.sync="tabName" @tab-change="tabChange" v-bkloading="{ isLoading }">
        <template #setting>
            <bk-button v-if="!metadataMap.forbidStatus && repoType !== 'docker' && repoType !== 'conan' && noInLockList"
                outline class="mr10" @click="$emit('download')">{{$t('download')}}</bk-button>
            <operation-list
                v-if="storeType !== 'virtual'"
                class="mr20"
                :list="operationBtns">
                <bk-button icon="ellipsis"></bk-button>
            </operation-list>
        </template>
        <bk-tab-panel v-if="detail.basic" name="basic" :label="$t('baseInfo')">
            <div class="version-base-info base-info display-block" :data-title="$t('baseInfo')">
                <div class="package-name grid-item">
                    <label>{{$t('artifactName')}}</label>
                    <span class="flex-1 flex-align-center text-overflow">
                        <span class="text-overflow" :title="packageName">{{ packageName }}</span>
                        <span v-if="detail.basic.groupId" class="ml5 repo-tag"> {{ detail.basic.groupId }} </span>
                    </span>
                    <template v-if="storeType === 'virtual'">
                        <label class="grid-store-source">{{$t('repositorySource')}}</label>
                        <span class="flex-1 flex-align-center text-overflow">
                            <span class="text-overflow" :title="sourceRepoName || repoName">{{ sourceRepoName || repoName }}</span>
                        </span>
                    </template>
                </div>
                <div v-if="detail.basic.os" class="package-name grid-item">
                    <label>OS/ARCH</label>
                    <span class="flex-1 text-overflow" :title="detail.basic.os.join()">{{ detail.basic.os.join() }}</span>
                </div>
                <template v-if="detail.basic.version">
                    <div class="grid-item"
                        v-for="{ name, label, value } in detailInfoMap"
                        :key="name">
                        <label>{{ label }}</label>
                        <span class="flex-1 flex-align-center text-overflow">
                            <span class="text-overflow" v-bk-overflow-tips>{{ value }}</span>
                            <template v-if="name === 'version'">
                                <span class="ml5 repo-tag"
                                    v-for="tag in detail.basic.stageTag"
                                    :key="tag">
                                    {{ tag }}
                                </span>
                                <scan-tag v-if="showRepoScan" class="ml5" :status="metadataMap.scanStatus"></scan-tag>
                                <lock-tag v-if="metadataMap.lockStatus" :lock-user="metadataMap.lockUser" :lock-description="(detail.metadata.find(m => m.key === 'lockType') || {}).description"></lock-tag>
                                <forbid-tag class="ml10"
                                    v-if="metadataMap.forbidStatus"
                                    :forbid-user="metadataMap.forbidUser"
                                    :forbid-type="metadataMap.forbidType"
                                    :forbid-description="forbidDescription">
                                </forbid-tag>
                                <!-- 黑名单icon -->
                                <Icon
                                    v-if="!noInLockList"
                                    v-bk-tooltips="{
                                        content: $t('alreadyJoinRepoBlackList')
                                    }"
                                    class="ml10"
                                    size="16" :style="{
                                        color: '#FFB549'
                                    }" name="blackWhiteList" />
                            </template>
                        </span>
                    </div>
                </template>
                <div class="package-description grid-item">
                    <label>{{$t('description')}}</label>
                    <span class="flex-1 text-overflow" :title="detail.basic.description">{{ detail.basic.description || '' }}</span>
                </div>
            </div>
            <div class="version-base-info base-info-guide display-block" :data-title="$t('useTips')">
                <template v-if="articleInstall?.[0]">
                    <div class="sub-section" v-for="block in articleInstall[0].main" :key="block.subTitle">
                        <div class="mb10">{{ block.subTitle }}</div>
                        <code-area class="mb20" v-if="block.codeList && block.codeList.length" :code-list="block.codeList"></code-area>
                    </div>
                </template>
            </div>
            <div class="version-base-info base-info-checksums display-block" data-title="Checksums">
                <div v-if="detail.basic.sha256" class="grid-item">
                    <label>SHA256</label>
                    <span class="flex-1 text-overflow" :title="detail.basic.sha256">{{ detail.basic.sha256 }}</span>
                </div>
                <div v-if="detail.basic.md5" class="grid-item">
                    <label>MD5</label>
                    <span class="flex-1 text-overflow" :title="detail.basic.md5">{{ detail.basic.md5 }}</span>
                </div>
            </div>
        </bk-tab-panel>
        <bk-tab-panel v-if="detail.metadata" name="metadata" :label="$t('metaData')">
            <div class="display-block" :data-title="$t('metaData')">
                <!-- 虚拟仓库及软件源模式下不支持更新元数据 -->
                <metadataDialog v-if="storeType !== 'virtual' && !whetherSoftware && !hasLockMetadata && showUpdateOperation" ref="metadataDialogRef" @add-metadata="addMetadataHandler"></metadataDialog>
                <bk-table
                    v-if="showMetadataTable"
                    :data="metadataDataList"
                    :outer-border="false"
                    :row-border="false"
                    size="small">
                    <template #empty>
                        <empty-data ex-style="margin-top:130px;"></empty-data>
                    </template>
                    <!-- <bk-table-column :label="$t('key')" prop="key" show-overflow-tooltip></bk-table-column>
                    <bk-table-column :label="$t('value')" prop="value" show-overflow-tooltip></bk-table-column> -->
                    <bk-table-column :label="$t('metadata')">
                        <template #default="{ row }">
                            <metadata-tag :metadata="row" />
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('description')" prop="description" show-overflow-tooltip></bk-table-column>
                    <bk-table-column v-if="storeType !== 'virtual' && !whetherSoftware && !hasLockMetadata" width="70">
                        <template #default="{ row }">
                            <bk-popconfirm v-if="!row.system && showUpdateOperation" trigger="click" width="230" @confirm="deleteMetadataHandler(row)">
                                <div slot="content">
                                    <div class="flex-align-center pb10">
                                        <i class="bk-icon icon-info-circle-shape pr5 content-icon"></i>
                                        <div class="content-text">{{$t('deleteMetadataConfirm')}}</div>
                                    </div>
                                </div>
                                <Icon class="hover-btn" size="24" name="icon-delete" />
                            </bk-popconfirm>
                        </template>
                    </bk-table-column>
                </bk-table>
            </div>
        </bk-tab-panel>
        <bk-tab-panel v-if="detail.mod !== undefined" name="definition" :label="$t('definition')">
            <pre v-if="detail.mod" class="code-block">{{ detail.mod }}</pre>
            <div v-else style="width: 100%;
                height: 100%;
                color: #909399;
                font-size: 14px;"
                class="flex-center">
                {{ $t('noData') }}
            </div>
        </bk-tab-panel>
        <bk-tab-panel v-if="detail.readme" name="readme" :label="$t('readMe')">
            <div ref="readmeContent" class="version-detail-readme" v-html="readmeContent"></div>
        </bk-tab-panel>
        <bk-tab-panel v-if="detail.manifest && !isEmpty(detail.manifest)" name="manifest" label="Manifest">
            <div class="display-block" data-title="Manifest">
                <bk-table
                    :data="Object.entries(detail.manifest)"
                    :outer-border="false"
                    :row-border="false"
                    size="small">
                    <template #empty>
                        <empty-data ex-style="margin-top:130px;"></empty-data>
                    </template>
                    <bk-table-column :label="$t('key')" prop="0" show-overflow-tooltip></bk-table-column>
                    <bk-table-column :label="$t('value')" prop="1" show-overflow-tooltip></bk-table-column>
                </bk-table>
            </div>
        </bk-tab-panel>
        <bk-tab-panel v-if="detail.layers && !isEmpty(detail.layers)" name="layers" label="Layers">
            <div class="version-layers display-block" data-title="Layers">
                <div class="block-header grid-item">
                    <label>ID</label>
                    <span class="pl40">{{ $t('size') }}</span>
                </div>
                <div class="grid-item" v-for="layer in detail.layers" :key="layer.digest">
                    <label class="text-overflow" :title="layer.digest">{{ layer.digest }}</label>
                    <span class="pl40">{{ convertFileSize(layer.size) }}</span>
                </div>
            </div>
        </bk-tab-panel>
        <bk-tab-panel v-if="detail.history && !isEmpty(detail.history)" name="history" label="IMAGE HISTORY">
            <div class="version-history">
                <div class="version-history-left">
                    <div class="version-history-code hover-btn"
                        v-for="(code, index) in detail.history"
                        :key="index"
                        :class="{ select: selectedHistory.created_by === code.created_by }"
                        @click="selectedHistory = code">
                        {{code.created_by}}
                    </div>
                </div>
                <div class="version-history-right">
                    <header class="version-history-header">Command</header>
                    <code-area class="mt20"
                        :show-line-number="false"
                        :code-list="[selectedHistory.created_by]">
                    </code-area>
                </div>
            </div>
        </bk-tab-panel>
        <bk-tab-panel v-if="detail.dependencyInfo" name="dependencyInfo" :label="$t('dependencies')">
            <article class="version-dependencies">
                <section class="version-dependencies-main display-block"
                    v-for="type in ['dependencies', 'devDependencies', 'dependents']"
                    :key="type"
                    :data-title="type">
                    <template v-if="detail.dependencyInfo[type].length">
                        <template
                            v-if="type !== 'dependents'"
                            v-for="{ name, version } in detail.dependencyInfo[type]">
                            <div class="version-dependencies-key text-overflow" :key="name" :title="name">{{ name }}</div>
                            <div class="version-dependencies-value text-overflow" :key="name + version" :title="version">{{ version }}</div>
                        </template>
                        <template
                            v-else
                            v-for="(item,index) in detail.dependencyInfo['dependents']">
                            <div class="version-dependencies-key text-overflow" :key="index" :title="item">{{ item }}</div>
                        </template>
                    </template>
                    <empty-data v-else class="version-dependencies-empty"></empty-data>
                </section>
            </article>
        </bk-tab-panel>
        <bk-tab-panel v-if="detailType === 'maven'" name="rely" :label="$t('dependencies')">
            <mavenDependencies></mavenDependencies>
        </bk-tab-panel>
    </bk-tab>
</template>
<script>
    import metadataTag from '@repository/views/repoCommon/metadataTag'
    import CodeArea from '@repository/components/CodeArea'
    import OperationList from '@repository/components/OperationList'
    import ScanTag from '@repository/views/repoScan/scanTag'
    import forbidTag from '@repository/components/ForbidTag'
    import LockTag from '@repository/components/LockTag'
    import mavenDependencies from '@repository/views/repoCommon/mavenDependencies'
    import metadataDialog from '@repository/components/metadataDialog'
    import { mapState, mapActions } from 'vuex'
    import { convertFileSize, formatDate } from '@repository/utils'
    import repoGuideMixin from '@repository/views/repoCommon/repoGuideMixin'
    import { isEmpty } from 'lodash'
    export default {
        name: 'commonVersionDetail',
        components: {
            CodeArea,
            OperationList,
            ScanTag,
            forbidTag,
            metadataTag,
            mavenDependencies,
            metadataDialog,
            LockTag
        },
        mixins: [repoGuideMixin],
        props: {
            noInLockList: {
                type: Boolean,
                default: true
            },
            showUpdateOperation: {
                type: Boolean,
                default: true,
                describe: '是否显示编辑制品相关操作'
            },
            showDeleteOperation: {
                type: Boolean,
                default: true,
                describe: '是否显示删除制品相关操作'
            },
            showLockOperation: {
                type: Boolean,
                default: true,
                describe: '是否显示锁定制品相关操作'
            },
            showForbidOperation: {
                type: Boolean,
                default: true,
                describe: '是否显示禁用制品相关操作'
            },
            canMoveOrCopy: {
                type: Boolean,
                default: false,
                describe: '是否显示移动或复制相关操作'
            }
        },
        data () {
            return {
                tabName: 'basic',
                isLoading: false,
                detail: {
                    basic: {
                        readme: ''
                    }
                },
                readmeContent: '',
                selectedHistory: {},
                metadata: {
                    show: false,
                    loading: false,
                    key: '',
                    value: ''
                },
                rules: {
                    key: [
                        {
                            required: true,
                            message: this.$t('pleaseInput') + this.$t('space') + this.$t('key'),
                            trigger: 'blur'
                        }
                    ],
                    value: [
                        {
                            required: true,
                            message: this.$t('pleaseInput') + this.$t('space') + this.$t('value'),
                            trigger: 'blur'
                        }
                    ]
                },
                detailType: '', // maven仓库显示依赖tab项的repoType，但不能直接用repoType，直接用会导致依赖这个tab项出现在其余的tab项之前
                isEmpty,
                showMetadataTable: true // 用于控制元数据列表table是否显示
            }
        },
        computed: {
            ...mapState(['userList', 'scannerSupportPackageType']),
            detailInfoMap () {
                return [
                    // { name: 'os', label: 'OS/ARCH' },
                    { name: 'version', label: this.$t('version') },
                    { name: 'fullPath', label: this.$t('path') },
                    { name: 'size', label: this.$t('size') },
                    { name: 'downloadCount', label: this.$t('downloads') },
                    { name: 'downloads', label: this.$t('downloads') },
                    { name: 'createdBy', label: this.$t('createdBy') },
                    { name: 'createdDate', label: this.$t('createdDate') },
                    { name: 'lastModifiedBy', label: this.$t('lastModifiedBy') },
                    { name: 'lastModifiedDate', label: this.$t('lastModifiedDate') }
                ].filter(({ name }) => name in this.detail.basic)
                    .map(item => ({ ...item, value: this.detail.basic[item.name] }))
            },
            metadataMap () {
                return (this.detail.metadata || []).reduce((target, meta) => {
                    target[meta.key] = meta.value
                    return target
                }, {})
            },
            // 是否是 软件源模式
            whetherSoftware () {
                return this.$route.path.startsWith('/software')
            },
            showRepoScan () {
                return this.scannerSupportPackageType.join(',').toLowerCase().includes(this.repoType) && !(this.storeType === 'virtual')
            },
            operationBtns () {
                const basic = this.detail.basic
                const metadataMap = this.metadataMap
                return [
                    ...(!metadataMap.forbidStatus
                        ? [
                            (this.showUpdateOperation && !(this.storeType === 'remote') && !(this.storeType === 'virtual') && !metadataMap.lockStatus) && { clickEvent: () => this.$emit('tag'), label: this.$t('upgrade'), disabled: (basic.stageTag || '').includes('@release') }
                            // this.showRepoScan && { clickEvent: () => this.$emit('scan'), label: this.$t('scan') }
                        ]
                        : []),
                    this.showForbidOperation && !this.whetherSoftware && !(this.storeType === 'virtual') && { clickEvent: () => this.$emit('forbid'), label: metadataMap.forbidStatus ? this.$t('relieve') + this.$t('space') + this.$t('forbid') : this.$t('forbid') },
                    this.showLockOperation && !this.whetherSoftware && !(this.storeType === 'virtual') && { clickEvent: () => this.$emit('lock'), label: metadataMap.lockStatus ? this.$t('relieve') + this.$t('space') + this.$t('lock') : this.$t('lock') },

                    (this.storeType === 'local') && this.canMoveOrCopy && !metadataMap.forbidStatus && !metadataMap.lockStatus && { clickEvent: () => this.$emit('move'), label: this.$t('move') },
                    (this.storeType === 'local') && this.canMoveOrCopy && !metadataMap.forbidStatus && { clickEvent: () => this.$emit('copy'), label: this.$t('copy') },

                    (this.showDeleteOperation && !this.whetherSoftware && !(this.storeType === 'virtual') && !metadataMap.lockStatus) && { clickEvent: () => this.$emit('delete'), label: this.$t('delete') }
                ].filter(Boolean)
            },
            metadataDataList () {
                // JavaScript中，true 被视为 1，false 被视为 0
                return this.detail.metadata
                    .filter(item => item.display) // 先过滤出 display 为 true 的对象
                    .sort((a, b) => b.system - a.system) // 然后根据 system 排序， system 为 true 的对象排在前面
            },
            // 用户是否设置了锁定，当前制品版本处于锁定状态下时不允许添加及删除任何元数据
            hasLockMetadata () {
                return this.detail.metadata?.find((m) => m.key === 'lockStatus')?.value
            },
            // 获取元数据中的禁用原因
            forbidDescription () {
                return this.detail.metadata?.find((m) => m.key === 'forbidStatus')?.description
            }
        },
        watch: {
            version: {
                handler (version) {
                    version && this.getDetail()
                },
                immediate: true
            },
            'detail.readme' (val) {
                if (val) {
                    const promise = window.marked ? Promise.resolve() : window.loadLibScript('/ui/libs/marked.min.js')
                    promise.then(() => {
                        this.readmeContent = window.marked.parse(val)
                    })
                }
            }
        },
        destroyed () {
            this.removeReadmeAClick()
        },
        beforeRouteLeave () {
            this.removeReadmeAClick()
        },
        methods: {
            convertFileSize,
            ...mapActions([
                'getVersionDetail',
                'addPackageMetadata',
                'deletePackageMetadata'
            ]),
            readmeClickEvent (event) {
                const target = event.target
                // 检查是否为a标签，并阻止点击事件
                if (target.tagName.toLowerCase() === 'a') {
                    event.preventDefault()
                }
            },
            initReadmeAClick () {
                const readmeContent = this.$refs.readmeContent
                if (readmeContent) {
                    readmeContent.addEventListener('click', this.readmeClickEvent)
                }
            },
            removeReadmeAClick () {
                const readmeContent = this.$refs.readmeContent
                if (readmeContent) {
                    readmeContent.removeEventListener('click', this.readmeClickEvent)
                }
            },
            getDetail () {
                this.isLoading = true
                // 在每次重新获取接口详情时都需要先将此tab页的类型置为空，否则不会重新加载依赖tab页组件，也就不会重新获取数据
                this.detailType = ''
                this.removeReadmeAClick()
                this.getVersionDetail({
                    projectId: this.projectId,
                    repoType: this.repoType,
                    repoName: this.storeType === 'virtual' ? this.sourceRepoName : this.repoName,
                    packageKey: this.packageKey,
                    version: this.version
                }).then(res => {
                    if (!res) return
                    const basic = res.basic
                    this.detail = {
                        ...res,
                        basic: {
                            ...basic,
                            size: basic.size && convertFileSize(basic.size),
                            createdBy: this.userList[basic.createdBy] ? this.userList[basic.createdBy].name : basic.createdBy,
                            createdDate: basic.createdDate && formatDate(basic.createdDate),
                            lastModifiedBy: this.userList[basic.lastModifiedBy] ? this.userList[basic.lastModifiedBy].name : basic.lastModifiedBy,
                            lastModifiedDate: basic.lastModifiedDate && formatDate(basic.lastModifiedDate)
                        }
                    }
                    if (this.repoType === 'docker') {
                        this.selectedHistory = (res?.history && res?.history[0]) || {}
                    }
                    // 此时不能在HTML中直接使用repoType，如果直接使用会导致该tab页提前被渲染，出现在其他tab页之前，不符合产品要求
                    this.detailType = this.repoType
                    // rpm仓库因为版本详情页的使用指引，需要获取当前版本详情的fullPath，用于替换使用指引的变量值
                    if (this.repoType === 'rpm') {
                        this.$router.replace({
                            query: {
                                ...this.$route.query,
                                packageFullPath: this.detail?.basic?.fullPath
                            }
                        })
                    }
                }).finally(() => {
                    this.initReadmeAClick()
                    this.isLoading = false
                })
            },
            // 添加元数据
            addMetadataHandler (item) {
                const { key, value, description } = item
                this.addPackageMetadata({
                    projectId: this.projectId,
                    repoName: this.storeType === 'virtual' ? this.sourceRepoName : this.repoName,
                    body: {
                        packageKey: this.packageKey,
                        version: this.version,
                        versionMetadata: [{ key, value, description, system: false }]
                    }
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('add') + this.$t('space') + this.$t('success')
                    })
                    // 此时添加成功，需要通过ref调用组件的关闭弹窗方法
                    this.$refs.metadataDialogRef?.hiddenAddMetadata()
                    this.getDetail()
                })
            },
            // 删除元数据
            deleteMetadataHandler (row) {
                this.deletePackageMetadata({
                    projectId: this.projectId,
                    repoName: this.storeType === 'virtual' ? this.sourceRepoName : this.repoName,
                    body: {
                        packageKey: this.packageKey,
                        version: this.version,
                        keyList: [row.key]
                    }
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('delete') + this.$t('space') + this.$t('metadata') + this.$t('space') + this.$t('success')
                    })
                    this.getDetail()
                })
            },
            tabChange () {
                // 用于解决metadata在某些情况下列表错位，无法完整显示元数据列表内容
                if (this.tabName === 'metadata') {
                    this.showMetadataTable = false
                    this.$nextTick(() => {
                        this.showMetadataTable = true
                    })
                }
            }
        }
    }
</script>
<style lang="scss" scoped>
.common-version-container {
    height: 100%;
    .code-block {
        font-family: monospace, 'Courier New', Courier, Consolas, Monaco, 'DejaVu Sans Mono', 'Roboto Mono';
        background-color: #F9FBFF;
        padding: 10px;
        color: #081E40;
        white-space: pre-wrap;
        overflow-x: auto;
    }
    ::v-deep .bk-tab-section {
        height: calc(100% - 50px);
        overflow-y: auto;
    }
    .version-base-info {
        .grid-item {
            display: flex;
            align-items: center;
            height: 40px;
            overflow: hidden;
            > * {
                padding: 0 10px;
            }
            > label {
                line-height: 40px;
                flex-basis: 126px;
                flex-shrink: 0;
                background-color: var(--bgColor);
            }
        }
        &.base-info {
            display: grid;
            grid-template: auto / repeat(2, 1fr);
            border: solid var(--borderColor);
            border-width: 1px 0 0 1px;
            .grid-item {
                border: solid var(--borderColor);
                border-width: 0 1px 1px 0;
                > label {
                    color: var(--fontSubsidiaryColor);
                    border-right: 1px solid var(--borderColor);
                }
            }
            .package-name,
            .package-description {
                grid-column: 1 / 3;
            }
        }
        &.base-info-guide {
            padding: 20px 20px 0;
            border: 1px dashed var(--borderWeightColor);
            border-radius: 4px;
        }
        &.base-info-checksums {
            padding: 20px 10px;
            display: grid;
            background-color: var(--bgLighterColor);
        }
    }
    .version-layers {
        padding: 20px;
        display: grid;
        gap: 20px;
        background-color: var(--bgHoverColor);
        .grid-item {
            display: flex;
            overflow: hidden;
            label {
                padding-left: 10px;
                flex-basis: 600px;
            }
        }
        .block-header {
            border-bottom: 1px solid var(--borderWeightColor);
        }
    }
    .grid-store-source{
        border-left: 1px solid var(--borderColor);;
        margin:0 1px 0 0;
    }
    .version-history {
        height: 100%;
        display: flex;
        &-left {
            height: 100%;
            width: 30%;
            padding-right: 40px;
            margin-right: 40px;
            border-right: 2px solid var(--borderWeightColor);
            overflow-y: auto;
            counter-reset: row-num;
            .version-history-code {
                height: 42px;
                line-height: 42px;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                &:hover {
                    background-color: var(--bgLighterColor);
                }
                &.select {
                    background-color: var(--bgHoverColor);
                }
                &:before {
                    display: inline-block;
                    width: 30px;
                    margin-right: 5px;
                    text-align: center;
                    background-color: var(--bgColor);
                    counter-increment: row-num;
                    content: counter(row-num);
                }
            }
        }
        &-right {
            height: 100%;
            overflow-y: auto;
            width: 70%;
            flex: 2;
        }
    }
    .version-dependencies {
        height: 100%;
        overflow-y: auto;
        &-main {
            display: grid;
            grid-template: auto / repeat(4, 1fr);
            grid-gap: 1px;
            background-color: var(--borderWeightColor);
            border: 1px solid var(--borderWeightColor);
        }
        &-more {
            grid-column: 1 / 5;
            line-height: 40px;
            padding-left: 30px;
            background-color: white;
        }
        &-key, &-value {
            line-height: 40px;
            padding-left: 30px;
            padding-right: 10px;
        }
        &-key {
            background-color: var(--bgHoverColor);
        }
        &-value {
            background-color: white;
        }
        &-empty {
            padding: 20px;
            grid-column: 1 / 5;
            background-color: white;
        }
    }
}
.content-icon {
    color: var(--dangerColor);
}
</style>
<style lang="scss">
.version-detail-readme {
    font: initial;
    color: initial;
    line-height: initial;
    ul {
        padding-inline-start: 40px;
    }

    li {
        list-style: inherit;
    }
    
    a {
        cursor: default;
        color: var(--primaryColor);
        text-decoration: underline;
        &:hover {
            color: var(--primaryHoverColor)
        }
    }

    pre {
        > * {
            white-space: initial;
        }
    }
    h1, h2, h3, h4, h5, h6 {
        margin-top: 0;
        margin-bottom: 16px;
        font-weight: 600;
        line-height: 1.25;
    }
    blockquote, dl, ol, p, pre, table, ul {
        margin-top: 0;
        margin-bottom: 16px;
    }
    h1 {
        font-size: 2rem;
    }
    h2 {
        font-size: 1.5rem;
    }
    h3 {
        font-size: 1.25rem;
    }

    table {
        overflow: auto;
        word-break: normal;
        word-break: keep-all;
        border-collapse: collapse;
        border-spacing: 0;
        tr {
            background-color: var(--bgColor);
            border-top: 1px solid var(--borderColor);
            th, td {
                padding: 6px 13px;
                text-align: left;
                border: 1px solid var(--borderColor);
            }
        }
    }
}
</style>
