<template>
    <bk-tab class="common-version-container" type="unborder-card" :active.sync="tabName" v-bkloading="{ isLoading }">
        <template #setting>
            <bk-button theme="primary" outline class="mr10" @click="$emit('tag')">晋级</bk-button>
            <bk-button outline class="mr10" @click="$emit('download')">下载</bk-button>
            <bk-button theme="danger" outline class="mr20" @click="$emit('delete')">删除</bk-button>
        </template>
        <bk-tab-panel v-if="detail.basic" name="versionBaseInfo" :label="$t('baseInfo')">
            <div class="version-base-info">
                <div class="base-info" :data-title="$t('baseInfo')">
                    <div class="package-name grid-item">
                        <label>制品名称：</label>
                        <span>
                            <span>{{ packageName }}</span>
                            <span v-if="detail.basic.groupId" class="mr5 repo-tag"> {{ detail.basic.groupId }} </span>
                        </span>
                    </div>
                    <div class="grid-item"
                        v-for="{ name, label, value } in detailInfoMap"
                        :key="name">
                        <label>{{ label }}：</label>
                        <span>
                            <span>{{ value }}</span>
                            <template v-if="name === 'version'">
                                <span class="mr5 repo-tag"
                                    v-for="tag in detail.basic.stageTag"
                                    :key="tag">
                                    {{ tag }}
                                </span>
                            </template>
                        </span>
                    </div>
                    <div class="package-description grid-item">
                        <label>描述：</label>
                        <span>{{ detail.basic.description || '--' }}</span>
                    </div>
                </div>
                <div class="base-info-guide" :data-title="$t('useTips')">
                    <div class="sub-section" v-for="block in articleInstall[0].main" :key="block.subTitle">
                        <div class="mb10">{{ block.subTitle }}</div>
                        <code-area class="mb20" bg-color="#e6edf6" color="#63656E" v-if="block.codeList && block.codeList.length" :code-list="block.codeList"></code-area>
                    </div>
                </div>
                <div class="base-info-checksums" data-title="Checksums">
                    <div v-if="detail.basic.sha256" class="grid-item">
                        <label>SHA256</label>
                        <span class="pl40">{{ detail.basic.sha256 }}</span>
                    </div>
                    <div v-if="detail.basic.md5" class="grid-item">
                        <label>MD5</label>
                        <span class="pl40">{{ detail.basic.md5 }}</span>
                    </div>
                </div>
            </div>
        </bk-tab-panel>
        <bk-tab-panel v-if="detail.metadata" name="versionMetaData" :label="$t('metaData')">
            <div class="flex-column version-metadata">
                <div class="pl20 pb10 flex-align-center metadata-thead">
                    <span class="metadata-key">{{ $t('key') }}</span>
                    <span class="metadata-value">{{ $t('value') }}</span>
                </div>
                <div slot="prepend" class="pl15 add-metadata-main">
                    <bk-form form-type="inline" :label-width="80" :model="metadata" :rules="rules" ref="metadatForm">
                        <bk-form-item class="mr10" :required="true" property="key">
                            <bk-input style="width: 230px" size="small" v-model="metadata.key" :placeholder="$t('key')"></bk-input>
                        </bk-form-item>
                        <bk-form-item class="mr10" :required="true" property="value">
                            <bk-input style="width: 350px" size="small" v-model="metadata.value" :placeholder="$t('value')"></bk-input>
                        </bk-form-item>
                        <bk-form-item>
                            <bk-button size="small" theme="default" @click="addMetadataHandler">{{ $t('add') }}</bk-button>
                        </bk-form-item>
                    </bk-form>
                </div>
                <div class="pl20 pb10 pt10 flex-align-center metadata-tr" v-for="([key, value]) in Object.entries(detail.metadata)" :key="key">
                    <span class="metadata-key">{{ key }}</span>
                    <span class="metadata-value">{{ value }}</span>
                </div>
                <empty-data v-if="!Object.keys(detail.metadata).length" ex-style="margin-top:100px;"></empty-data>
            </div>
        </bk-tab-panel>
        <bk-tab-panel v-if="detail.layers" name="versionLayers" label="Layers">
            <div class="mt20 flex-column">
                <div class="pl10 pb10 flex-align-center version-layers">
                    <span class="display-key">ID</span>
                    <span class="display-value">{{ $t('size') }}</span>
                </div>
                <div class="pl10 pb10 pt10 flex-align-center version-layers" v-for="layer in detail.layers" :key="layer.digest">
                    <span class="display-key">{{ layer.digest }}</span>
                    <span class="display-value">{{ convertFileSize(layer.size) }}</span>
                </div>
            </div>
        </bk-tab-panel>
        <bk-tab-panel v-if="detail.history" name="versionImageHistory" label="IMAGE HISTORY">
            <div class="version-history">
                <div class="version-history-left">
                    <div class="version-history-code hover-btn"
                        v-for="code in detail.history"
                        :key="code.created_by"
                        :class="{ select: selectedHistory.created_by === code.created_by }"
                        @click="selectedHistory = code">
                        {{code.created_by}}
                    </div>
                </div>
                <div class="version-history-right">
                    <header class="version-history-header">Command</header>
                    <code-area class="mt20"
                        bg-color="#e6edf6"
                        color="#63656E"
                        :line-number="false"
                        :code-list="[selectedHistory.created_by]">
                    </code-area>
                </div>
            </div>
        </bk-tab-panel>
        <bk-tab-panel v-if="detail.dependencyInfo" name="versionDependencies" :label="$t('dependencies')">
            <article class="version-dependencies">
                <section class="version-dependencies-main"
                    v-for="type in ['dependencies', 'devDependencies', 'dependents']"
                    :key="type"
                    :data-title="type">
                    <template v-if="detail.dependencyInfo[type].length">
                        <template
                            v-for="{ name, version } in detail.dependencyInfo[type]">
                            <div class="version-dependencies-key" :key="name">{{ name }}</div>
                            <div v-if="type !== 'dependents'" class="version-dependencies-value" :key="name + version">{{ version }}</div>
                        </template>
                        <div class="version-dependencies-more" v-if="type === 'dependents' && dependentsPage">
                            <bk-button text title="primary" @click="loadMore">{{ $t('loadMore') }}</bk-button>
                        </div>
                    </template>
                    <empty-data v-else class="version-dependencies-empty"></empty-data>
                </section>
            </article>
        </bk-tab-panel>
    </bk-tab>
</template>
<script>
    import CodeArea from '@/components/CodeArea'
    import { mapState, mapActions } from 'vuex'
    import { convertFileSize, formatDate } from '@/utils'
    import repoGuideMixin from './repoGuideMixin'
    export default {
        name: 'commonVersionDetail',
        components: { CodeArea },
        mixins: [repoGuideMixin],
        data () {
            return {
                tabName: 'versionBaseInfo',
                isLoading: false,
                detail: {
                    basic: {}
                },
                // 当前已请求页数，0代表没有更多
                dependentsPage: 1,
                pagination: {
                    count: 0,
                    current: 1,
                    limit: 20,
                    'limit-list': [10, 20, 40]
                },
                selectedHistory: {},
                metadata: {
                    key: '',
                    value: ''
                },
                rules: {
                    key: [
                        {
                            required: true,
                            message: this.$t('pleaseInput') + this.$t('key'),
                            trigger: 'change'
                        }
                    ],
                    value: [
                        {
                            required: true,
                            message: this.$t('pleaseInput') + this.$t('value'),
                            trigger: 'change'
                        }
                    ]
                }
            }
        },
        computed: {
            ...mapState(['userList']),
            detailInfoMap () {
                return [
                    { name: 'version', label: this.$t('version') },
                    { name: 'os', label: 'OS/ARCH' },
                    { name: 'fullPath', label: this.$t('path') },
                    { name: 'size', label: this.$t('size') },
                    { name: 'downloadCount', label: this.$t('downloads') },
                    { name: 'downloads', label: this.$t('downloads') },
                    { name: 'createdBy', label: this.$t('createdBy') },
                    { name: 'createdDate', label: this.$t('createdDate') },
                    { name: 'lastModifiedBy', label: this.$t('lastModifiedBy') },
                    { name: 'lastModifiedDate', label: this.$t('lastModifiedDate') }
                ].filter(({ name }) => this.detail.basic.hasOwnProperty(name))
                    .map(item => ({ ...item, value: this.detail.basic[item.name] }))
            }
        },
        watch: {
            version: {
                handler: function (version) {
                    version && this.getDetail()
                },
                immediate: true
            }
        },
        methods: {
            convertFileSize,
            ...mapActions([
                'getVersionDetail',
                'getNpmDependents',
                'addPackageMetadata'
            ]),
            getDetail () {
                this.isLoading = true
                this.getVersionDetail({
                    projectId: this.projectId,
                    repoType: this.repoType,
                    repoName: this.repoName,
                    packageKey: this.packageKey,
                    version: this.version
                }).then(res => {
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
                    if (this.repoType === 'npm') {
                        const dependents = res.dependencyInfo.dependents
                        this.detail.dependencyInfo.dependents = dependents.records
                        if (dependents.totalRecords < 20) {
                            this.dependentsPage = 0
                        }
                    }
                    if (this.repoType === 'docker') {
                        this.selectedHistory = res.history[0] || {}
                    }
                }).finally(() => {
                    this.isLoading = false
                })
            },
            loadMore () {
                if (this.isLoading) return
                this.isLoading = true
                this.getNpmDependents({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    packageKey: this.packageKey,
                    current: this.dependentsPage + 1
                }).then(({ records }) => {
                    this.detail.dependencyInfo.dependents.push(...records)
                    this.dependentsPage++
                    if (records.length < 20) this.dependentsPage = 0
                }).finally(() => {
                    this.isLoading = false
                })
            },
            async addMetadataHandler () {
                await this.$refs.metadatForm.validate()
                this.addPackageMetadata({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    body: {
                        packageKey: this.packageKey,
                        version: this.version,
                        metadata: {
                            [this.metadata.key]: this.metadata.value
                        }
                    }
                }).finally(() => {
                    this.metadata = {
                        key: '',
                        value: ''
                    }
                    this.getDetail()
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.common-version-container {
    height: 100%;
    ::v-deep .bk-tab-section {
        height: calc(100% - 40px);
        .bk-tab-content {
            height: 100%;
        }
    }
    .version-base-info {
        height: 100%;
        overflow-y: auto;
        .base-info,
        .base-info-guide,
        .base-info-checksums {
            position: relative;
            margin-top: 55px;
            margin-bottom: 20px;
            &:first-child {
                margin-top: 35px;
            }
            &:before {
                position: absolute;
                top: -30px;
                left: 20px;
                content: '';
                width: 3px;
                height: 12px;
                background-color: var(--primaryColor);
            }
            &:after {
                position: absolute;
                top: -35px;
                left: 30px;
                content: attr(data-title);
                font-size: 16px;
                font-weight: bold;
            }
        }
        .base-info {
            padding: 20px;
            display: grid;
            grid-template: auto / repeat(3, 1fr);
            grid-gap: 20px;
            background-color: var(--bgHoverColor);
            .package-name,
            .package-description {
                grid-column: 1 / 4;
            }
            .repo-tag {
                color: white;
                background-color: #91ADD1;
            }
        }
        .base-info-guide {
            padding: 20px 50px 0;
            border: 1px dashed var(--borderWeightColor);
            border-radius: 4px;
        }
        .base-info-checksums {
            padding: 20px;
            display: grid;
            grid-gap: 20px;
            background-color: var(--bgHoverColor);
        }
        .grid-item {
            display: flex;
            label {
                width: 100px;
                text-align: right;
            }
        }
    }
    .version-metadata {
        height: 100%;
        overflow: auto;
        .metadata-thead, .metadata-tr {
            border-bottom: 1px solid var(--borderWeightColor);
            line-height: 2;
        }
        .metadata-key {
            width: 250px;
        }
        .metadata-value {
            flex: 1;
            word-break: break-all;
        }
        .add-metadata-main {
            display: flex;
            align-items: center;
            height: 40px;
            border-bottom: 1px solid var(--borderWeightColor);
        }
    }
    .version-layers {
        border-bottom: 1px solid var(--borderWeightColor);
        line-height: 2;
        .display-key {
            text-align: left;
            flex: 6;
        }
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
                    background-color: var(--bgHoverColor);
                }
                &.select {
                    background-color: #ebedf0;
                }
                &:before {
                    display: inline-block;
                    width: 30px;
                    margin-right: 5px;
                    text-align: center;
                    background-color: #f9f9f9;
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
            position: relative;
            margin-top: 55px;
            margin-bottom: 20px;
            display: grid;
            grid-template: auto / repeat(4, 1fr);
            grid-gap: 1px;
            background-color: var(--borderWeightColor);
            border: 1px solid var(--borderWeightColor);
            &:first-child {
                margin-top: 35px;
            }
            &:before {
                position: absolute;
                top: -30px;
                left: 20px;
                content: '';
                width: 3px;
                height: 12px;
                background-color: var(--primaryColor);
            }
            &:after {
                position: absolute;
                top: -35px;
                left: 30px;
                content: attr(data-title);
                font-size: 16px;
                font-weight: bold;
            }
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
        }
        &-key {
            background-color: var(--bgHoverColor);
        }
        &-value {
            background-color: white;
        }
        &-empty {
            padding: 20px 0 20px 50px;
            justify-content: flex-start;
            grid-column: 1 / 5;
            background-color: white;
        }
    }
    .display-key {
        flex: 1;
        text-align: right;
        margin-right: 40px;
    }
    .display-value {
        flex: 3;
        word-break: break-all;
    }
}
</style>
