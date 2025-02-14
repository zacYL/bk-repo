<template>
    <canway-dialog
        :value="show"
        width="1000"
        height-num="660"
        :title="title"
        render-directive="if"
        @cancel="cancel">
        <div class="search-detail-container">
            <aside class="search-detail-container-left">
                <header class="version-header" :title="versionTotalInfo">{{versionTotalInfo}}</header>
                <div class="version-list">
                    <div class="mb10 version-item flex-center"
                        :class="{ 'selected': $version === version }"
                        v-for="$version in versionList"
                        :key="$version"
                        @click="changeVersion($version)">
                        <span class="text-overflow" style="max-width:150px;" :title="$version">{{ $version }}</span>
                    </div>
                </div>
            </aside>
            <!-- 右侧展示基本信息及checkSum及元数据等 -->
            <main class="search-detail-container-right" v-bkloading="{ isLoading }">
                <header class="pl10 version-header flex-align-center">{{$t('versionDetail')}}</header>
                <div class="search-detail-container-right-info">
                    <!-- 基本信息 -->
                    <div class="version-base-info base-info display-block" :data-title="$t('baseInfo')">
                        <div class="package-name grid-item">
                            <label>{{$t('artifactName')}}</label>
                            <span class="flex-1 flex-align-center text-overflow">
                                <span class="text-overflow" :title="packageName">{{ packageName }}</span>
                                <span v-if="detail.basic.groupId" class="ml5 repo-tag"> {{ detail.basic.groupId }} </span>
                            </span>
                        </div>
                        <template v-if="detail.basic.version">
                            <div class="grid-item"
                                v-for="{ name, label, value } in detailInfoMap"
                                :key="name">
                                <label>{{ label }}</label>
                                <span class="flex-1 flex-align-center text-overflow">
                                    <span class="text-overflow" :title="value">{{ value }}</span>
                                    <template v-if="name === 'version'">
                                        <span class="ml5 repo-tag"
                                            v-for="tag in detail.basic.stageTag"
                                            :key="tag">
                                            {{ tag }}
                                        </span>
                                        <scan-tag v-if="showRepoScan" readonly class="ml10" :status="metadataMap.scanStatus"></scan-tag>
                                        <forbid-tag class="ml10"
                                            v-if="metadataMap.forbidStatus"
                                            :forbid-user="metadataMap.forbidUser"
                                            :forbid-type="metadataMap.forbidType"
                                            :forbid-description="forbidDescription">
                                        </forbid-tag>
                                    </template>
                                </span>
                            </div>
                        </template>
                        <div class="package-description grid-item">
                            <label>{{$t('description')}}</label>
                            <span class="flex-1 text-overflow" :title="detail.basic.description">{{ detail.basic.description || '' }}</span>
                        </div>
                    </div>
                    <!-- checkSum -->
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
                    <!-- 元数据 -->
                    <div v-if="detail.metadata" class="display-block" :data-title="$t('metaData')">
                        <bk-table
                            :data="metadataDataList"
                            :outer-border="false"
                            :row-border="false"
                            size="small">
                            <template #empty>
                                <empty-data ex-style="margin-top:130px;"></empty-data>
                            </template>
                            <bk-table-column :label="$t('metadata')">
                                <template #default="{ row }">
                                    <metadata-tag :metadata="row" />
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('description')" prop="description" show-overflow-tooltip></bk-table-column>
                        </bk-table>
                    </div>
                </div>
            </main>
        </div>
        <template #footer>
            <bk-button theme="primary" @click="jumpToVersionDetail">{{$t('jumpToVersionDetail')}}</bk-button>
        </template>
    </canway-dialog>
</template>
<script>
    import metadataTag from '@repository/views/repoCommon/metadataTag'
    import ScanTag from '@repository/views/repoScan/scanTag'
    import forbidTag from '@repository/components/ForbidTag'
    import { mapState, mapActions } from 'vuex'
    import { convertFileSize, formatDate } from '@repository/utils'
    export default {
        name: 'searchDependDetail',
        components: { metadataTag, ScanTag, forbidTag },
        props: {
            title: {
                type: String,
                default () {
                    return this.$t('versionDetailSnapshot')
                }
            },
            // 当前选择版本的节点信息
            info: {
                type: Object,
                default: () => {}
            },
            // 左侧版本列表
            versionList: {
                type: Array,
                default: () => []
            }
        },
        data () {
            return {
                show: false,
                isLoading: false,
                version: '',
                detail: {
                    basic: {}
                }
            }
        },
        computed: {
            ...mapState(['userList', 'scannerSupportPackageType']),
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
                ].filter(({ name }) => name in this.detail.basic)
                    .map(item => ({ ...item, value: this.detail.basic[item.name] }))
            },
            metadataMap () {
                return (this.detail.metadata || []).reduce((target, meta) => {
                    target[meta.key] = meta.value
                    return target
                }, {})
            },
            showRepoScan () {
                return this.scannerSupportPackageType.join(',').includes(this.info.type)
            },
            metadataDataList () {
                // JavaScript中，true 被视为 1，false 被视为 0
                return this.detail.metadata?.filter(item => item.display) // 先过滤出 display 为 true 的对象
                    .sort((a, b) => b.system - a.system) // 然后根据 system 排序， system 为 true 的对象排在前面
            },
            // 获取元数据中的禁用原因
            forbidDescription () {
                return this.detail.metadata?.find((m) => m.key === 'forbidStatus')?.description
            },
            packageName () {
                return this.info.key.replace(/^.*:\/\/(?:.*:)*([^:]+)$/, '$1')
            },
            versionTotalInfo () {
                return this.$t('artifactVersionTotalInfo', { length: this.versionList?.length || 0 })
            }
        },
        watch: {
            version: {
                handler (version) {
                    version && this.getDetail()
                },
                immediate: true
            },
            versionList: {
                handler (arr) {
                    this.version = arr.length > 0 ? arr[0] : ''
                },
                deep: true,
                immediate: true
            }
        },
        created () {
            // 获取系统支持的所有包名后缀列表
            this.refreshSupportPackageTypeList()
        },
        methods: {
            convertFileSize,
            ...mapActions(['getVersionDetail', 'refreshSupportPackageTypeList']),
            getDetail () {
                this.isLoading = true
                this.getVersionDetail({
                    projectId: this.info.projectId,
                    repoType: this.info.type,
                    repoName: this.info.repoName,
                    packageKey: this.info.key,
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
                }).finally(() => {
                    this.isLoading = false
                })
            },
            changeVersion (version) {
                this.version = version
            },
            cancel () {
                this.show = false
            },
            jumpToVersionDetail () {
                this.$emit('detail', this.version)
            }
        }
    }
</script>
<style lang="scss" scoped>
.search-detail-container {
    height: 500px;
    display: flex;
    justify-content: space-between;
    &-left {
        width: 180px;
        height: 100%;
        margin-right: 10px;
        background-color: #fff;
    }
    &-right {
        width:calc(100% - 150px);
        height: 100%;
        background-color: #fff;
        &-info {
            border-left: 1px solid var(--borderWeightColor);
            padding: 10px;
            height: calc(100% - 30px);
            overflow-y: auto;
        }
    }
    .version-header {
        height: 30px;
        font-size: 14px;
        color: var(--fontPrimaryColor);
        border-bottom: 1px solid var(--borderWeightColor);
        display: block;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
    .version-list {
        height: calc(100% - 30px);
        overflow-y: auto;
        padding: 10px ;
        background-color: #fff;
    }
    .version-item {
        height: 36px;
        border-radius: 2px;
        background-color: var(--bgLightColor);
        cursor: pointer;
        &:hover {
            background-color: var(--bgHoverLighterColor);
        }
        &.selected {
            color: white;
            background-color: var(--primaryColor);
        }
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
                flex-basis: 120px;
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
}
</style>
