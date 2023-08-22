<template>
    <bk-sideslider
        :is-show.sync="detailSlider.show"
        :title="detailSlider.data.name"
        @click.native.stop="() => {}"
        :quick-close="true"
        :width="720">
        <template #content><bk-tab class="detail-container" type="unborder-card" :active.sync="tabName">
            <bk-tab-panel name="detailInfo" :label="$t('baseInfo')">
                <div class="version-base-info base-info display-block" :data-title="$t('baseInfo')" v-bkloading="{ isLoading: detailSlider.loading }">
                    <div class="grid-item"
                        v-for="{ name, label, value } in detailInfoMap"
                        :key="name">
                        <label>{{ label }}：</label>
                        <span class="flex-1 text-overflow" :title="value">{{ value }}</span>
                    </div>
                </div>
                <div v-if="!detailSlider.folder" class="version-base-info base-info-checksums display-block" data-title="Checksums" v-bkloading="{ isLoading: detailSlider.loading }">
                    <div v-if="detailSlider.data.sha256" class="grid-item">
                        <label>SHA256：</label>
                        <span class="flex-1 text-overflow" :title="detailSlider.data.sha256">{{ detailSlider.data.sha256 }}</span>
                    </div>
                    <div v-if="detailSlider.data.md5" class="grid-item">
                        <label>MD5：</label>
                        <span class="flex-1 text-overflow" :title="detailSlider.data.md5">{{ detailSlider.data.md5 }}</span>
                    </div>
                </div>
                <div v-if="!detailSlider.folder" class="display-block" data-title="命令行下载">
                    <div class="pl30">
                        <bk-button text theme="primary" @click="createToken">{{ $t('createToken') }}</bk-button>
                        {{ $t('tokenSubTitle') }}
                        <router-link :to="{ name: 'repoToken' }">{{ $t('token') }}</router-link>
                    </div>
                    <code-area class="mt10" :code-list="codeList"></code-area>
                    <create-token-dialog ref="createToken"></create-token-dialog>
                </div>
            </bk-tab-panel>
            <bk-tab-panel v-if="!detailSlider.folder" name="metaDate" :label="$t('metaData')">
                <div class="display-block" :data-title="$t('metadata')">
                    <metadataDialog ref="metadataDialogRef" @add-metadata="addMetadataHandler"></metadataDialog>
                    <bk-table
                        :data="(detailSlider.data.nodeMetadata || []).filter(m => m.display)"
                        :outer-border="false"
                        :row-border="false"
                        size="small">
                        <template #empty>
                            <empty-data :is-loading="detailSlider.loading"></empty-data>
                        </template>
                        <!-- <bk-table-column :label="$t('key')" prop="key" show-overflow-tooltip></bk-table-column>
                        <bk-table-column :label="$t('value')" prop="value" show-overflow-tooltip></bk-table-column> -->
                        <bk-table-column :label="$t('metadata')">
                            <template #default="{ row }">
                                <metadata-tag :metadata="row" />
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('description')" prop="description" show-overflow-tooltip></bk-table-column>
                        <bk-table-column width="70">
                            <template #default="{ row }">
                                <bk-popconfirm v-if="!row.system" trigger="click" width="230" @confirm="deleteMetadataHandler(row)">
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
            <bk-tab-panel v-if="!detailSlider.folder && detailSlider.data.metadata"
                render-directive="if"
                name="topo" label="CI/CD关联信息"
                style="height:100%;">
                <topo :root-node="rootNode" :left-tree="leftTree" :right-tree="rightTree" />
            </bk-tab-panel>
        </bk-tab></template>
    </bk-sideslider>
</template>
<script>
    import metadataTag from '@repository/views/repoCommon/metadataTag'
    import CodeArea from '@repository/components/CodeArea'
    import createTokenDialog from '@repository/views/repoToken/createTokenDialog'
    import topo from '@repository/components/topo'
    import metadataDialog from '@repository/components/metadataDialog'
    import topoDataMixin from './artiTopoMixin'
    import { mapState, mapActions } from 'vuex'
    import { convertFileSize, formatDate } from '@repository/utils'
    export default {
        name: 'genericDetail',
        components: { CodeArea, createTokenDialog, metadataTag, topo, metadataDialog },
        mixins: [topoDataMixin],
        data () {
            return {
                tabName: 'detailInfo',
                detailSlider: {
                    show: false,
                    loading: false,
                    projectId: '',
                    repoName: '',
                    folder: false,
                    path: '',
                    data: {}
                }
            }
        },
        computed: {
            ...mapState(['userInfo', 'userList']),
            detailInfoMap () {
                return [
                    { name: 'fullPath', label: this.$t('path') },
                    { name: 'size', label: this.$t('size') },
                    { name: 'createdBy', label: this.$t('createdBy') },
                    { name: 'createdDate', label: this.$t('createdDate') },
                    { name: 'lastModifiedBy', label: this.$t('lastModifiedBy') },
                    { name: 'lastModifiedDate', label: this.$t('lastModifiedDate') },
                    { name: 'recentlyUseDate', label: this.$t('recentlyUseDate') }
                ].filter(({ name }) => name in this.detailSlider.data && (name !== 'size' || !this.detailSlider.data.folder) && (name !== 'recentlyUseDate' || !this.detailSlider.data.folder))
                    .map(item => ({ ...item, value: this.detailSlider.data[item.name] }))
            },
            codeList () {
                const { projectId, repoName, path } = this.detailSlider
                return [
                    `wget --user=${this.userInfo.username} --password=<PERSONAL_ACCESS_TOKEN> "${location.origin}/generic/${projectId}/${repoName}${path}"`
                ]
            }
        },
        methods: {
            ...mapActions(['getNodeDetail', 'addMetadata', 'deleteMetadata']),
            setData (data) {
                this.detailSlider = {
                    ...this.detailSlider,
                    ...data
                }
                this.getDetail()
            },
            getDetail () {
                this.detailSlider.loading = true
                this.getNodeDetail({
                    projectId: this.detailSlider.projectId,
                    repoName: this.detailSlider.repoName,
                    fullPath: this.detailSlider.path
                }).then(data => {
                    this.detailSlider.data = {
                        ...data,
                        name: data.name || this.repoName,
                        size: convertFileSize(data.size),
                        createdBy: this.userList[data.createdBy] ? this.userList[data.createdBy].name : data.createdBy,
                        createdDate: formatDate(data.createdDate),
                        lastModifiedBy: this.userList[data.lastModifiedBy] ? this.userList[data.lastModifiedBy].name : data.lastModifiedBy,
                        lastModifiedDate: formatDate(data.lastModifiedDate),
                        recentlyUseDate: formatDate(data.recentlyUseDate)
                    }
                }).finally(() => {
                    this.detailSlider.loading = false
                })
            },
            createToken () {
                this.$refs.createToken.showDialogHandler()
            },

            addMetadataHandler (item) {
                const { key, value, description } = item
                this.addMetadata({
                    projectId: this.detailSlider.projectId,
                    repoName: this.detailSlider.repoName,
                    fullPath: this.detailSlider.data.fullPath,
                    body: {
                        nodeMetadata: [{ key, value, description }]
                    }
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('add') + this.$t('space') + this.$t('success')
                    })
                    // 此时添加成功，需要通过ref调用组件的关闭弹窗方法
                    this.$refs.metadataDialogRef.hiddenAddMetadata()
                    this.getDetail()
                })
            },
            deleteMetadataHandler (row) {
                this.deleteMetadata({
                    projectId: this.detailSlider.projectId,
                    repoName: this.detailSlider.repoName,
                    fullPath: this.detailSlider.data.fullPath,
                    body: {
                        keyList: [row.key]
                    }
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('delete') + this.$t('space') + this.$t('metadata') + this.$t('space') + this.$t('success')
                    })
                    this.getDetail()
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.detail-container {
    height: 100%;
    ::v-deep .bk-tab-section {
        height: calc(100% - 50px);
        overflow-y: auto;
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
                flex-basis: 80px;
                text-align: right;
            }
        }
    }
}
.content-icon {
    color: var(--dangerColor);
}
</style>
