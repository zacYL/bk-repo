<template>
    <div class="repo-config-container">
        <bk-form ref="repoBaseInfo" class="repo-base-info" :label-width="150" v-if="nodeType === 'depot'">
            <bk-form-item :label="$t('repoName')">
                <div class="flex-align-center">
                    <icon size="20" :name="detailInfo.repoType || repoType" />
                    <span class="ml10">{{replaceRepoName(detailInfo.repoName || detailInfo.name)}}</span>
                </div>
            </bk-form-item>
            <bk-form-item :label="$t('storeTypes')">
                <div class="flex-align-center">
                    <icon size="20" :name="(detailInfo.category && detailInfo.category.toLowerCase() || 'local') + '-store'" />
                    <span class="ml10">{{$t((detailInfo.category && detailInfo.category.toLowerCase() || 'local') + 'Store' ) }}</span>
                </div>
            </bk-form-item>
            <bk-form-item :label="$t('repoAddress')">
                <span>{{repoAddress}}</span>
            </bk-form-item>
            <template v-if="detailInfo.category === 'REMOTE'">
                <bk-form-item :label="$t('remoteProxyAddress')">
                    <bk-input :disabled="true" class="w480" v-model.trim="detailInfo.url"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('remoteProxyAccount')">
                    <bk-input :disabled="true" class="w480" v-model.trim="detailInfo.credentials.username"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('remoteProxyPassword')">
                    <bk-input :disabled="true" class="w480" type="password" v-model.trim="detailInfo.credentials.password"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('networkProxy')">
                    <template>
                        <bk-switcher :disabled="true" v-model="detailInfo.network.switcher" theme="primary"></bk-switcher>
                        <span>{{detailInfo.network.switcher ? $t('open') : $t('close')}}</span>
                    </template>
                </bk-form-item>
                <template v-if="detailInfo.network.switcher">
                    <bk-form-item label="IP">
                        <bk-input :disabled="true" class="w480" v-model.trim="detailInfo.network.proxy.host"></bk-input>
                    </bk-form-item>
                    <bk-form-item :label="$t('port')">
                        <bk-input :disabled="true" class="w480" type="number" :max="65535" :min="1" v-model.trim="detailInfo.network.proxy.port"></bk-input>
                    </bk-form-item>
                    <bk-form-item :label="$t('networkAccount')">
                        <bk-input :disabled="true" class="w480" v-model.trim="detailInfo.network.proxy.username"></bk-input>
                    </bk-form-item>
                    <bk-form-item :label="$t('networkPassword')">
                        <bk-input :disabled="true" class="w480" type="password" v-model.trim="detailInfo.network.proxy.password"></bk-input>
                    </bk-form-item>
                </template>
            </template>
            <template v-if="detailInfo.category === 'VIRTUAL'">
                <bk-form-item :label=" $t('select') + $t('space') + $t('storageStore')">
                    <div class="virtual-check-container">
                        <store-sort
                            v-if="detailInfo.virtualStoreList.length"
                            :key="detailInfo.virtualStoreList"
                            ref="storeSortRef"
                            :disabled="true"
                            :sort-list="detailInfo.virtualStoreList"
                        ></store-sort>
                    </div>
                </bk-form-item>
                <!-- <bk-form-item :label="$t('uploadTargetStore')">
                    <bk-select
                        v-model="detailInfo.deploymentRepo"
                        style="width:300px;"
                        :show-empty="false"
                        :disabled="true"
                        :placeholder="$t('noAddedLocalStore')">
                        <bk-option v-for="item in deploymentRepoCheckList" :key="item.name" :id="item.name" :name="item.name">
                        </bk-option>
                    </bk-select>
                    <div class="form-tip">{{$t('addPackagePrompt')}}</div>
                </bk-form-item> -->
            </template>
            <bk-form-item :label="$t('accessPermission')">
                <card-radio-group
                    v-model="available"
                    :list="availableList"
                    disabled>
                </card-radio-group>
            </bk-form-item>

            <bk-form-item :label="$t('versionStrategy')" v-if="!(detailInfo.category === 'REMOTE') && !(detailInfo.category === 'VIRTUAL') && (repoType === 'maven' || repoType === 'npm')">
                <div class="flex-align-center">
                    <bk-switcher
                        v-model="detailInfo.override.switcher"
                        size="small"
                        theme="primary"
                        @change="handleOverrideChange"
                        disabled
                    ></bk-switcher>
                    <span class="ml10">{{$t('coverStrategyInfo')}}</span>
                </div>
                <bk-radio-group v-model="detailInfo.override.isFlag" v-if="detailInfo.override.switcher">
                    <bk-radio class="mr20" :value="false" disabled>{{$t('notAllowCover')}}</bk-radio>
                    <bk-radio :value="true" disabled>{{$t('allowCover')}}</bk-radio>
                </bk-radio-group>
            </bk-form-item>
            <template v-if="repoType === 'docker' && (detailInfo.category === 'LOCAL' || detailInfo.category === 'REMOTE')">
                <bk-form-item :label="$t('enabledLibraryNamespace')">
                    <bk-checkbox v-model="libraryNamespace" disabled></bk-checkbox>
                </bk-form-item>
            </template>
            <template v-if="!(detailInfo.category === 'REMOTE') && !(detailInfo.category === 'VIRTUAL') && repoType === 'rpm'">
                <bk-form-item :label="$t('enabledFileLists')">
                    <bk-checkbox v-model="detailInfo.enabledFileLists" disabled></bk-checkbox>
                </bk-form-item>
                <bk-form-item :label="$t('repoDataDepth')" property="repodataDepth" error-display-type="normal">
                    <bk-input class="w480" v-model.trim="detailInfo.repodataDepth" disabled></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('groupXmlSet')" property="groupXmlSet" error-display-type="normal">
                    <bk-tag-input
                        class="w480"
                        :value="detailInfo.groupXmlSet"
                        @change="(val) => {
                            detailInfo.groupXmlSet = val.map(v => {
                                return v.replace(/^([^.]*)(\.xml)?$/, '$1.xml')
                            })
                        }"
                        :list="[]"
                        trigger="focus"
                        :clearable="false"
                        allow-create
                        has-delete-icon
                        disabled>
                    </bk-tag-input>
                </bk-form-item>
            </template>
            <bk-form-item :label="$t('description')">
                <bk-input type="textarea"
                    class="w480"
                    maxlength="200"
                    :rows="6"
                    v-model.trim="detailInfo.description"
                    :placeholder="$t('repoDescriptionPlaceholder')"
                    disabled>
                </bk-input>
            </bk-form-item>

        </bk-form>

        <div class="version-base-info base-info display-block" :data-title="$t('baseInfo')" v-if="nodeType === 'file' || nodeType === 'folder'">
            <div class="grid-item"
                v-for="{ name, label, value } in detailInfoMap"
                :key="name">
                <label>{{ label }} : </label>
                <span style="cursor: pointer;" class="flex-1 text-overflow" :title="value">{{ value }}</span>
            </div>
        </div>
        <div v-if="nodeType === 'file' && !detailInfo.folder" class="version-base-info base-info-checksums display-block" data-title="Checksums">
            <div v-if="detailInfo.sha256" class="grid-item">
                <label>SHA256: </label>
                <span class="flex-1 text-overflow" :title="detailInfo.sha256">{{ detailInfo.sha256 }}</span>
            </div>
            <div v-if="detailInfo.md5" class="grid-item">
                <label>MD5: </label>
                <span class="flex-1 text-overflow" :title="detailInfo.md5">{{ detailInfo.md5 }}</span>
            </div>
        </div>

    </div>
</template>
<script>
    import CardRadioGroup from '@repository/components/CardRadioGroup'
    import StoreSort from '@repository/components/StoreSort'
    import { mapState, mapActions } from 'vuex'
    export default {
        name: 'repoConfig',
        components: {
            CardRadioGroup,
            StoreSort
        },
        props: {
            nodeType: {
                type: String,
                default: 'depot',
                required: true,
                describe: '当前选择的节点类型(仓库depot、文件夹folder、文件file)'
            },
            detailInfo: {
                type: Object,
                default: () => {
                    return {}
                },
                required: true,
                describe: '当前选择的节点的详情信息'
            }

        },
        data () {
            return {
            }
        },
        computed: {
            ...mapState(['domain']),
            projectId () {
                return this.$route.params.projectId
            },
            repoType () {
                return this.detailInfo?.type?.toLowerCase() || ''
            },
            detailInfoMap () {
                return [
                    // { name: 'repoName', label: this.$t('repoName') },
                    { name: 'fullPath', label: this.$t('path') },
                    { name: 'size', label: this.$t('size') },
                    { name: 'createdBy', label: this.$t('createdBy') },
                    { name: 'createdDate', label: this.$t('createdDate') },
                    { name: 'lastModifiedBy', label: this.$t('lastModifiedBy') },
                    { name: 'lastModifiedDate', label: this.$t('lastModifiedDate') }
                ].filter(({ name }) => name in this.detailInfo && (name !== 'size' || !this.detailInfo.folder))
                    .map(item => ({ ...item, value: this.detailInfo[item.name] }))
            },
            repoAddress () {
                this.detailInfo.repoType = this.detailInfo?.type?.toLowerCase() || ''
                const { repoType, name } = this.detailInfo
                if (repoType === 'docker') {
                    return `${location.protocol}//${this.domain.docker}/${this.projectId}/${name}/`
                }
                return `${location.origin}/${repoType}/${this.projectId}/${name}/`
            },
            available: {
                get () {
                    if (this.detailInfo.public) return 'public'
                    if (this.detailInfo?.configuration?.settings?.system) return 'system'
                    return 'project'
                },
                set (val) {
                    this.detailInfo.public = val === 'public'
                    this.detailInfo.system = val === 'system'
                }
            },
            availableList () {
                return [
                    { label: this.$t('openProjectLabel'), value: 'project', tip: this.$t('openProjectTip') },
                    { label: this.$t('systemPublic'), value: 'system', tip: this.$t('systemPublicTip') },
                    { label: this.$t('openPublicLabel'), value: 'public', tip: this.$t('openPublicTip') }
                ]
            },
            libraryNamespace: {
                get() {
                    return this.detailInfo?.configuration?.settings?.defaultNamespace === 'library'
                },
                set(val) {
                    this.detailInfo.configuration.settings.defaultNamespace = val === 'library'
                }
            }
            // 虚拟仓库中选择上传的目标仓库的下拉列表数据
            // deploymentRepoCheckList () {
            //     return this.detailInfo.virtualStoreList.filter(item => item.category === 'LOCAL')
            // }
        },
        watch: {
            repoType: {
                handler (type) {
                    type && this.getDomain(type)
                },
                immediate: true
            }
        },
        created () {
        },
        methods: {
            ...mapActions(['getDomain']),
            handleOverrideChange (isFlag) {
                this.detailInfo.override.switcher = isFlag
            }

        }
    }
</script>
<style lang="scss" scoped>
.repo-config-container {
    // height: 100%;
     height: calc(100% - 2em - 11px);
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
   .version-base-info {
        &.base-info {
            padding: 20px;
            display: grid;
            gap: 20px;
            background-color: var(--bgHoverColor);
        }
        &.base-info-checksums {
            padding: 20px;
            display: grid;
            gap: 20px;
            background-color: var(--bgHoverColor);
        }
        .grid-item {
            display: flex;
            overflow: hidden;
            label {
                flex-basis: 120px;
                text-align: left;
            }
        }
    }
.virtual-check-container{
    width: 96%;
}
</style>
