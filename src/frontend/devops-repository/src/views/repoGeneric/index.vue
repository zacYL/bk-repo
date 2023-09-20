<template>
    <div class="repo-generic-container">
        <header class="mb10 pl20 pr20 generic-header flex-align-center">
            <Icon class="generic-img" size="30" name="generic" />
            <div class="ml10 generic-title">
                <div class="repo-title text-overflow" :title="replaceRepoName(repoName)">
                    {{ replaceRepoName(repoName) }}
                </div>
            </div>
        </header>
        <div class="repo-generic-main flex-align-center"
            :style="{ 'margin-left': `${searchFileName ? -(sideBarWidth + moveBarWidth) : 0}px` }">
            <div class="repo-generic-side"
                :style="{ 'flex-basis': `${sideBarWidth}px` }"
                v-bkloading="{ isLoading: treeLoading }">
                <div class="repo-generic-side-info">
                    <span>{{$t('folderDirectory')}}</span>
                </div>
                <repo-tree
                    class="repo-generic-tree"
                    ref="repoTree"
                    :tree="genericTree"
                    :open-list="sideTreeOpenList"
                    :selected-node="selectedTreeNode"
                    @icon-click="iconClickHandler"
                    @item-click="itemClickHandler">
                    <template #operation="{ item }">
                        <operation-list
                            v-if="item.roadMap === selectedTreeNode.roadMap"
                            :list="[
                                permission.write && repoName !== 'pipeline' && { clickEvent: () => handlerUpload(item), label: '上传文件' },
                                permission.write && repoName !== 'pipeline' && { clickEvent: () => handlerUpload(item, true), label: '上传文件夹' },
                                permission.write && repoName !== 'pipeline' && { clickEvent: () => addFolder(item), label: '新建文件夹' }
                            ]">
                        </operation-list>
                    </template>
                </repo-tree>
            </div>
            <move-split-bar
                :left="sideBarWidth"
                :width="moveBarWidth"
                @change="changeSideBarWidth"
            />
            <div class="repo-generic-table" v-bkloading="{ isLoading }">
                <div class="multi-operation flex-between-center">
                    <breadcrumb v-if="!searchFileName" :list="breadcrumb" omit-middle></breadcrumb>
                    <span v-else> {{repoName + (searchFullPath || (selectedTreeNode && selectedTreeNode.fullPath) || '') }}</span>
                    <div class="repo-generic-actions bk-button-group">
                        <bk-button
                            v-if="multiSelect.length"
                            @click="handlerMultiDownload()">
                            批量下载
                        </bk-button>
                        <bk-button class="ml10"
                            v-if="multiSelect.length && !whetherSoftware"
                            @click="handlerMultiDelete()">
                            批量删除
                        </bk-button>
                        <bk-input
                            class="w250 ml10"
                            v-model.trim="inFolderSearchName"
                            :placeholder="$t('inFolderSearchPlaceholder')"
                            clearable
                            right-icon="bk-icon icon-search"
                            @enter="inFolderSearchFile"
                            @clear="inFolderSearchFile">
                        </bk-input>
                        <bk-button class="ml10"
                            @click="getArtifactories">
                            {{ $t('refresh') }}
                        </bk-button>
                    </div>
                </div>
                <bk-table
                    :data="artifactoryList"
                    height="calc(100% - 100px)"
                    :outer-border="false"
                    :row-border="false"
                    size="small"
                    @row-dblclick="openFolder"
                    @selection-change="selectMultiRow">
                    <template #empty>
                        <empty-data :is-loading="isLoading" :search="Boolean(searchFileName)"></empty-data>
                    </template>

                    <bk-table-column type="selection" width="60"></bk-table-column>

                    <bk-table-column :label="$t('fileName')" prop="name" show-overflow-tooltip>
                        <template #default="{ row }">
                            <Icon class="table-svg mr5" size="16" :name="row.folder ? 'folder' : getIconName(row.name)" />
                            <span
                                class="hover-btn disabled"
                                v-if="!row.folder && row.metadata.forbidStatus"
                                v-bk-tooltips="{ content: tooltipContent(row.metadata), placements: ['top'] }"
                            >{{row.name}}</span>
                            <!-- 文件夹支持: 鼠标悬浮时显示小手样式 -->
                            <span v-else :class="{ 'hover-btn': row.folder }">{{ row.name }}</span>
                            <scan-tag class="mr5 table-svg"
                                v-if="!row.folder && genericScanFileTypes.includes(row.name.replace(/^.+\.([^.]+)$/, '$1'))"
                                :status="row.metadata.scanStatus"
                                repo-type="generic"
                                :full-path="row.fullPath">
                            </scan-tag>
                            <Icon v-if="row.metadata.lockStatus" class="table-svg mr5" size="20" name="lock" />
                        </template>
                    </bk-table-column>

                    <!-- <bk-table-column label="$t('metadata')">
                        <template #default="{ row }">
                            <metadata-tag :metadata="row.nodeMetadata" />
                        </template>
                    </bk-table-column> -->

                    <bk-table-column v-if="searchFileName" :label="$t('path')" prop="fullPath" show-overflow-tooltip></bk-table-column>

                    <bk-table-column :label="$t('lastModifiedDate')" prop="lastModifiedDate" width="150" :render-header="renderHeader">
                        <template #default="{ row }">{{ formatDate(row.lastModifiedDate) }}</template>
                    </bk-table-column>

                    <bk-table-column :label="$t('lastModifiedBy')" width="90" show-overflow-tooltip>
                        <template #default="{ row }">
                            {{ userList[row.lastModifiedBy] ? userList[row.lastModifiedBy].name : row.lastModifiedBy }}
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('createdBy')" width="90" show-overflow-tooltip>
                        <template #default="{ row }">
                            {{ userList[row.createdBy] ? userList[row.createdBy].name : row.createdBy }}
                        </template>
                    </bk-table-column>

                    <bk-table-column :label="$t('size')" width="90" show-overflow-tooltip>
                        <template #default="{ row }">
                            <bk-button text
                                v-if="row.folder && !('folderSize' in row)"
                                :disabled="row.sizeLoading"
                                @click="calculateFolderSize(row)">{{ $t('calculate') }}</bk-button>
                            <span v-else>
                                {{ convertFileSize(row.size || row.folderSize || 0) }}
                            </span>
                        </template>
                    </bk-table-column>

                    <bk-table-column :label="$t('operation')" width="70">
                        <template #default="{ row }">
                            <operation-list
                                :list="[
                                    { clickEvent: () => showDetail(row), label: $t('detail') },
                                    ...(!row.metadata.forbidStatus ? [
                                        !row.folder && handlerPreview(row) && { clickEvent: () => handlerPreview(row, true), label: $t('preview') },
                                        { clickEvent: () => handlerDownload(row), label: $t('download') },
                                        ...((repoName !== 'pipeline' && !row.metadata.lockStatus) ? [
                                            (permission.edit && !whetherSoftware) && { clickEvent: () => renameRes(row), label: $t('rename') },
                                            (permission.write && !whetherSoftware) && { clickEvent: () => moveRes(row), label: $t('move') },
                                            (permission.write && !whetherSoftware) && { clickEvent: () => copyRes(row), label: $t('copy') }
                                        ] : []),
                                        ...(!row.folder ? [
                                            { clickEvent: () => handlerShare(row), label: $t('share') },
                                            ( genericScanFileTypes.includes(row.name.replace(/^.+\.([^.]+)$/, '$1')) && !whetherSoftware)
                                                && { clickEvent: () => handlerScan(row), label: '扫描制品' }
                                        ] : []),
                                        ...(row.folder ? [
                                            { clickEvent: () => handlerShare(row), label: $t('share') }
                                        ] : [])
                                    ] : []),
                                    (!row.folder && !whetherSoftware) && { clickEvent: () => showLimitDialog('forbid',row), label: row.metadata.forbidStatus ? '解除禁止' : '禁止使用' },
                                    (!row.folder && !whetherSoftware) && { clickEvent: () => showLimitDialog('lock',row), label: row.metadata.lockStatus ? '解除锁定' : '锁定' },
                                    (permission.delete && !whetherSoftware && !row.metadata.lockStatus) && { clickEvent: () => deleteRes(row), label: $t('delete') }
                                ]">
                            </operation-list>
                        </template>
                    </bk-table-column>
                </bk-table>
                <bk-pagination
                    class="p10"
                    size="small"
                    align="right"
                    show-total-count
                    @change="current => handlerPaginationChange({ current })"
                    @limit-change="limit => handlerPaginationChange({ limit })"
                    :current.sync="pagination.current"
                    :limit="pagination.limit"
                    :count="pagination.count"
                    :limit-list="pagination.limitList">
                </bk-pagination>
            </div>
        </div>

        <generic-detail ref="genericDetail"></generic-detail>
        <generic-form-dialog ref="genericFormDialog" @refresh="refreshNodeChange"></generic-form-dialog>
        <generic-share-dialog ref="genericShareDialog"></generic-share-dialog>
        <generic-tree-dialog ref="genericTreeDialog" @update="updateOperateTreeNode" @refresh="refreshNodeChange"></generic-tree-dialog>
        <preview-basic-file-dialog ref="previewBasicFileDialog"></preview-basic-file-dialog>
        <compressed-file-table ref="compressedFileTable" @show-preview="handlerPreviewBasicsFile"></compressed-file-table>
        <operationLimitConfirmDialog ref="operationLimitConfirmDialog" @confirm="handlerLimitOperation"></operationLimitConfirmDialog>
    </div>
</template>
<script>
    import OperationList from '@repository/components/OperationList'
    import Breadcrumb from '@repository/components/Breadcrumb'
    import MoveSplitBar from '@repository/components/MoveSplitBar'
    import RepoTree from '@repository/components/RepoTree'
    import operationLimitConfirmDialog from '@repository/components/operationLimitConfirmDialog'
    import ScanTag from '@repository/views/repoScan/scanTag'
    // import metadataTag from '@repository/views/repoCommon/metadataTag'
    import genericDetail from '@repository/views/repoGeneric/genericDetail'
    import genericFormDialog from '@repository/views/repoGeneric/genericFormDialog'
    import genericShareDialog from '@repository/views/repoGeneric/genericShareDialog'
    import genericTreeDialog from '@repository/views/repoGeneric/genericTreeDialog'
    import previewBasicFileDialog from '@repository/views/repoGeneric/previewBasicFileDialog'
    import compressedFileTable from '@repository/views/repoGeneric/compressedFileTable'
    import { convertFileSize, formatDate, debounce } from '@repository/utils'
    import { getIconName, genericScanFileTypes } from '@repository/store/publicEnum'
    import { mapState, mapMutations, mapActions } from 'vuex'
    export default {
        name: 'repoGeneric',
        components: {
            OperationList,
            Breadcrumb,
            MoveSplitBar,
            RepoTree,
            ScanTag,
            // metadataTag,
            genericDetail,
            genericFormDialog,
            genericShareDialog,
            genericTreeDialog,
            previewBasicFileDialog,
            compressedFileTable,
            operationLimitConfirmDialog
        },
        data () {
            return {
                MODE_CONFIG,
                genericScanFileTypes,
                sideBarWidth: 300,
                moveBarWidth: 10,
                isLoading: false,
                treeLoading: false,
                // 左侧树处于打开状态的目录
                sideTreeOpenList: [],
                sortType: 'lastModifiedDate',
                sortDirection: 'DESC',
                // 中间展示的table数据
                artifactoryList: [],
                multiSelect: [],
                // 左侧树选中的节点
                selectedTreeNode: {},
                // 分页信息
                pagination: {
                    count: 0,
                    current: 1,
                    limit: 20,
                    limitList: [10, 20, 40]
                },
                debounceClickTreeNode: null,
                inFolderSearchName: this.$route.query.fileName,
                searchFullPath: ''
            }
        },
        computed: {
            ...mapState(['repoListAll', 'userList', 'permission', 'genericTree']),
            projectId () {
                return this.$route.params.projectId
            },
            repoName () {
                return this.$route.query.repoName
            },
            currentRepo () {
                return this.repoListAll.find(repo => repo.name === this.repoName) || {}
            },
            breadcrumb () {
                if (!this.selectedTreeNode.roadMap) return
                const breadcrumb = []
                let node = this.genericTree
                const road = this.selectedTreeNode.roadMap.split(',')
                road.splice(0, 1)
                road.forEach(index => {
                    breadcrumb.push({
                        name: node[index].displayName,
                        value: node[index],
                        cilckHandler: item => {
                            this.itemClickHandler(item.value)
                        }
                    })
                    node = node[index].children
                })
                return breadcrumb
            },
            searchFileName () {
                return this.$route.query.fileName
            },
            // 是否是 软件源模式
            whetherSoftware () {
                return this.$route.path.startsWith('/software')
            }
        },
        watch: {
            projectId () {
                this.getRepoListAll({ projectId: this.projectId })
            },
            repoName () {
                this.initTree()
            },
            '$route.query.path' () {
                this.pathChange()
            }
        },
        beforeRouteEnter (to, from, next) {
            // 前端隐藏report仓库/log仓库
            if (MODE_CONFIG === 'ci' && (to.query.repoName === 'log')) {
                next({
                    name: 'repoList',
                    params: {
                        projectId: to.params.projectId
                    }
                })
            } else next()
        },
        created () {
            this.getRepoListAll({ projectId: this.projectId })
            this.initTree()
            this.pathChange()
            window.repositoryVue.$on('upload-refresh', debounce((path) => {
                if (path.replace(/\/[^/]+$/, '').includes(this.selectedTreeNode.fullPath)) {
                    this.itemClickHandler(this.selectedTreeNode)
                }
            }))
            this.debounceClickTreeNode = debounce(this.clickTreeNodeHandler, 100)
        },
        beforeDestroy () {
            window.repositoryVue.$off('upload-refresh')
        },
        methods: {
            convertFileSize,
            getIconName,
            formatDate,
            ...mapMutations(['INIT_TREE', 'INIT_OPERATE_TREE', 'UPDATE_TREE', 'UPDATE_OPERATE_TREE']),
            ...mapActions([
                'getRepoListAll',
                'getFolderList',
                'getArtifactoryList',
                'deleteArtifactory',
                'deleteMultiArtifactory',
                'downloadMultiArtifactory',
                'getFolderSize',
                'getFileNumOfFolder',
                'getMultiFileNumOfFolder',
                'forbidMetadata',
                'lockMetadata',
                'getGenericList'
            ]),
            tooltipContent ({ forbidType, forbidUser }) {
                switch (forbidType) {
                    case 'SCANNING':
                        return '制品正在扫描中'
                    case 'QUALITY_UNPASS':
                        return '制品扫描质量规则未通过'
                    case 'MANUAL':
                        return `${this.userList[forbidUser]?.name || forbidUser} 手动禁止`
                    default:
                        return ''
                }
            },
            changeSideBarWidth (sideBarWidth) {
                if (sideBarWidth > 260) {
                    this.sideBarWidth = sideBarWidth
                }
            },
            renderHeader (h, { column }) {
                return h('div', {
                    class: {
                        'flex-align-center hover-btn': true,
                        'selected-header': this.sortType === column.property
                    },
                    on: {
                        click: () => {
                            this.sortType = column.property
                            // 当点击切换排序时需要将升序修改为降序，降序修改为升序
                            this.sortDirection = this.sortDirection === 'DESC' ? 'ASC' : 'DESC'
                            this.handlerPaginationChange()
                        }
                    }
                }, [
                    h('span', column.label),
                    h('i', {
                        class: `ml5 devops-icon ${this.sortDirection === 'DESC' ? 'icon-down-shape' : 'icon-up-shape'}`
                    })
                ])
            },
            initTree () {
                this.INIT_TREE([{
                    name: this.replaceRepoName(this.repoName),
                    displayName: this.replaceRepoName(this.repoName),
                    fullPath: '',
                    folder: true,
                    children: [],
                    roadMap: `${this.repoName},0`
                }])
            },
            // 初始化操作树(复制、移动)
            initGenericOperateTree () {
                const Tree = []
                return this.getGenericList({ projectId: this.projectId }).then((res) => {
                    res.forEach(item => {
                        Tree.push({
                            name: this.replaceRepoName(item.name),
                            displayName: this.replaceRepoName(item.name),
                            fullPath: '',
                            folder: true,
                            children: [],
                            roadMap: `${item.name},0`
                        })
                    })
                    this.INIT_OPERATE_TREE(Tree)
                }).catch((error) => {
                    console.log(error)
                })
            },
            pathChange () {
                const paths = (this.$route.query.path || '').split('/').filter(Boolean)
                paths.pop() // 定位到文件/文件夹的上级目录
                paths.reduce(async (chain, path) => {
                    const node = await chain
                    if (!node) return
                    await this.updateGenericTreeNode(node)
                    const child = node.children.find(child => child.name === path)
                    if (!child) return
                    this.sideTreeOpenList.push(child.roadMap)
                    return child
                }, Promise.resolve(this.genericTree[0])).then(node => {
                    this.itemClickHandler(node || this.genericTree[0])
                })
            },
            // 获取中间列表数据
            getArtifactories () {
                if (!this.repoName || !this.projectId) {
                    return
                }
                this.isLoading = true
                this.getArtifactoryList({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    fullPath: this.searchFullPath || this.selectedTreeNode?.fullPath,
                    ...(this.inFolderSearchName
                        ? {
                            name: this.searchFileName
                        }
                        : {}
                    ),
                    current: this.pagination.current,
                    limit: this.pagination.limit,
                    sortType: this.sortType,
                    isPipeline: this.repoName === 'pipeline',
                    sortDirection: this.sortDirection,
                    searchFlag: this.searchFileName
                }).then(({ records, totalRecords }) => {
                    this.pagination.count = totalRecords
                    this.artifactoryList = records.map(v => {
                        return {
                            metadata: {},
                            ...v,
                            // 流水线文件夹名称替换
                            name: v.metadata?.displayName || v.name
                        }
                    })
                }).finally(() => {
                    this.isLoading = false
                })
            },
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getArtifactories()
            },
            // 树组件选中文件夹
            itemClickHandler (node) {
                this.debounceClickTreeNode(node)
            },
            clickTreeNodeHandler (node) {
                this.selectedTreeNode = node
                this.handlerPaginationChange()
                // 更新已展开文件夹数据
                const reg = new RegExp(`^${node.roadMap}`)
                const openList = this.sideTreeOpenList
                openList.splice(0, openList.length, ...openList.filter(v => !reg.test(v)))
                // 打开选中节点的左侧树的所有祖先节点
                node.roadMap.split(',').forEach((v, i, arr) => {
                    const roadMap = arr.slice(0, i + 1).join(',')
                    !openList.includes(roadMap) && openList.push(roadMap)
                })
                // 更新子文件夹
                if (node.loading) return
                this.updateGenericTreeNode(node)

                // 更新url参数
                const { path = '' } = this.$route.query
                if (path.replace(/\/[^/]+$/, '') !== node.fullPath) {
                    this.$router.replace({
                        query: {
                            ...this.$route.query,
                            path: `${node.fullPath}/default`
                        }
                    })
                }
            },
            iconClickHandler (node) {
                // 更新已展开文件夹数据
                const reg = new RegExp(`^${node.roadMap}`)
                const openList = this.sideTreeOpenList
                if (openList.includes(node.roadMap)) {
                    openList.splice(0, openList.length, ...openList.filter(v => !reg.test(v)))
                } else {
                    openList.push(node.roadMap)
                    // 更新子文件夹
                    if (node.loading) return
                    // 当前选中文件夹为当前操作文件夹的后代文件夹，则锁定文件夹保证选中文件夹路径完整
                    if (node.roadMap !== this.selectedTreeNode.roadMap && reg.test(this.selectedTreeNode.roadMap)) return
                    this.updateGenericTreeNode(node)
                }
            },
            updateOperateTreeNode (item) {
                this.$set(item, 'loading', true)
                const name = item.roadMap.split(',').slice(0, 1)[0]
                return this.getFolderList({
                    projectId: this.projectId,
                    repoName: name,
                    fullPath: item.fullPath,
                    roadMap: item.roadMap,
                    isPipeline: this.repoName === 'pipeline'
                }).then((res) => {
                    const records = res.records
                    const roadMap = item.roadMap
                    this.UPDATE_OPERATE_TREE({
                        roadMap,
                        list: records.map((v, index) => ({
                            ...v,
                            roadMap: `${roadMap},${index}`
                        }))
                    })
                }).finally(() => {
                    this.$set(item, 'loading', false)
                })
            },
            updateGenericTreeNode (item) {
                this.$set(item, 'loading', true)
                const name = item?.roadMap && item?.roadMap.split(',').slice(0, 1)[0]
                return this.getFolderList({
                    projectId: this.projectId,
                    repoName: name,
                    fullPath: item.fullPath,
                    roadMap: item.roadMap,
                    isPipeline: this.repoName === 'pipeline'
                }).then((res) => {
                    const records = res.records
                    const roadMap = item.roadMap
                    this.UPDATE_TREE({
                        roadMap,
                        list: records.map((v, index) => ({
                            ...v,
                            roadMap: `${roadMap},${index}`
                        }))
                    })
                }).finally(() => {
                    this.$set(item, 'loading', false)
                })
            },
            // 双击table打开文件夹
            openFolder (row) {
                if (!row.folder) return
                if (this.searchFileName) {
                    // 搜索中打开文件夹
                    this.searchFullPath = row.fullPath
                    this.handlerPaginationChange()
                } else {
                    const node = this.selectedTreeNode.children.find(v => v.fullPath === row.fullPath)
                    this.itemClickHandler(node)
                }
            },
            showDetail ({ folder, fullPath }) {
                this.$refs.genericDetail.setData({
                    show: true,
                    loading: false,
                    projectId: this.projectId,
                    repoName: this.repoName,
                    folder,
                    path: fullPath,
                    data: {}
                })
            },
            renameRes ({ name, fullPath }) {
                this.$refs.genericFormDialog.setData({
                    show: true,
                    loading: false,
                    type: 'rename',
                    name,
                    path: fullPath,
                    title: `${this.$t('rename')} (${name})`
                })
            },
            addFolder ({ fullPath }) {
                this.$refs.genericFormDialog.setData({
                    show: true,
                    loading: false,
                    type: 'add',
                    path: fullPath + '/',
                    title: `${this.$t('create') + this.$t('folder')}`
                })
            },
            handlerScan ({ name, fullPath }) {
                this.$refs.genericFormDialog.setData({
                    show: true,
                    loading: false,
                    title: '扫描制品',
                    type: 'scan',
                    id: '',
                    name,
                    path: fullPath
                })
            },
            refreshNodeChange (destTreeData) {
                // 在当前仓库中复制或移动文件夹后需要更新选中目录的上层目录
                if (destTreeData?.repoName && destTreeData?.folder && destTreeData?.repoName === this.repoName) {
                    this.updateGenericTreeNode(destTreeData)
                }
                this.updateGenericTreeNode(this.selectedTreeNode)
                this.getArtifactories()
            },
            handlerShare ({ name, fullPath }) {
                this.$refs.genericShareDialog.setData({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    show: true,
                    loading: false,
                    title: `${this.$t('share')} (${name})`,
                    path: fullPath,
                    user: [],
                    ip: [],
                    permits: '',
                    time: 7
                })
            },
            async deleteRes ({ name, folder, fullPath }) {
                if (!fullPath) return
                let totalRecords
                if (folder) {
                    totalRecords = await this.getFileNumOfFolder({
                        projectId: this.projectId,
                        repoName: this.repoName,
                        fullPath
                    })
                }
                this.$confirm({
                    theme: 'danger',
                    message: `${this.$t('confirm') + this.$t('delete')}${folder ? this.$t('folder') : this.$t('file')} ${name} ？`,
                    subMessage: `${folder && totalRecords ? `当前文件夹下存在${totalRecords}个文件` : ''}`,
                    confirmFn: () => {
                        return this.deleteArtifactory({
                            projectId: this.projectId,
                            repoName: this.repoName,
                            fullPath
                        }).then(() => {
                            this.refreshNodeChange()
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('delete') + this.$t('success')
                            })
                        })
                    }
                })
            },
            moveRes ({ name, fullPath, folder }) {
                this.initGenericOperateTree().then(() => {
                    this.$refs.genericTreeDialog.setTreeData({
                        show: true,
                        type: 'move',
                        title: `${this.$t('move')} (${name})`,
                        path: fullPath,
                        folder: folder
                    })
                })
            },
            copyRes ({ name, fullPath, folder }) {
                this.initGenericOperateTree().then(() => {
                    this.$refs.genericTreeDialog.setTreeData({
                        show: true,
                        type: 'copy',
                        title: `${this.$t('copy')} (${name})`,
                        path: fullPath,
                        folder: folder
                    })
                })
            },
            handlerUpload ({ fullPath }, folder = false) {
                this.$globalUploadFiles({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    folder,
                    fullPath
                })
            },
            handlerDownload ({ fullPath }) {
                const url = `/generic/${this.projectId}/${this.repoName}/${encodeURIComponent(fullPath)}?download=true`
                this.$ajax.head(url).then(() => {
                    window.open(
                        '/web' + url,
                        '_self'
                    )
                }).catch(e => {
                    const message = e.status === 423 ? this.$t('fileDownloadError') : this.$t('fileError')
                    this.$bkMessage({
                        theme: 'error',
                        message
                    })
                })
            },
            handlerMultiDownload () {
                const commonPath = this.selectedTreeNode.fullPath
                const ids = this.multiSelect.map(r => r.id)
                const url = `${this.projectId}/${this.repoName}/${encodeURIComponent(commonPath)}?id=<${ids.join(':')}>`
                this.$ajax.get(`/generic/multi/false/${url}`).then(() => {
                    window.open(
                        '/web' + `/generic/multi/true/${url}`,
                        '_self'
                    )
                })
            },
            showLimitDialog (limitType, row) {
                this.$refs.operationLimitConfirmDialog.setData({
                    show: true,
                    loading: false,
                    limitType: limitType,
                    theme: 'danger',
                    limitStatus: row.metadata[`${limitType}Status`],
                    limitReason: '',
                    name: row.fullPath,
                    message: this.$t(row.metadata[`${limitType}Status`] ? 'confirmRemoveLimitOperationInfo' : 'confirmLimitOperationInfo', { limit: this.$t(limitType), type: this.$t('file') })
                })
            },
            handlerLimitOperation (data) {
                const { name, limitType } = data
                this[`${limitType}Metadata`]({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    fullPath: name,
                    body: {
                        nodeMetadata: [{ key: `${limitType}Status`, value: !data.limitStatus, description: data.limitReason }]
                    }
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: (data.limitStatus ? this.$t('remove') + this.$t('space') + this.$t(limitType) : this.$t(limitType)) + this.$t('success')
                    })
                    this.$refs.operationLimitConfirmDialog.dialogData.show = false
                    this.getArtifactories()
                }).finally(() => {
                    this.$refs.operationLimitConfirmDialog.dialogData.loading = false
                })
            },
            calculateFolderSize (row) {
                this.$set(row, 'sizeLoading', true)
                this.getFolderSize({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    fullPath: row.fullPath
                }).then(({ size }) => {
                    this.$set(row, 'folderSize', size)
                }).finally(() => {
                    this.$set(row, 'sizeLoading', false)
                })
            },
            selectMultiRow (selects) {
                this.multiSelect = selects
            },
            async handlerMultiDelete () {
                const paths = this.multiSelect.map(r => r.fullPath)
                const totalRecords = await this.getMultiFileNumOfFolder({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    paths
                })
                this.$confirm({
                    theme: 'danger',
                    message: `确认批量删除已选中的 ${this.multiSelect.length} 项？`,
                    subMessage: `选中文件夹和文件共计包含 ${totalRecords} 个文件`,
                    confirmFn: () => {
                        return this.deleteMultiArtifactory({
                            projectId: this.projectId,
                            repoName: this.repoName,
                            paths
                        }).then(() => {
                            this.refreshNodeChange()
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('delete') + this.$t('success')
                            })
                        })
                    }
                })
            },
            handlerPreview (row, excute = false) {
                const ext = row.name.replace(/^.+\.([^.]+)$/, '$1')
                const basicEnable = [
                    'txt', 'sh', 'bat', 'json', 'yaml', 'md',
                    'xml', 'log', 'ini', 'properties', 'toml'
                ].includes(ext)
                const compressEnable = [
                    'rar', 'zip', 'gz', 'tgz', 'tar', 'jar'
                ].includes(ext)
                if (basicEnable) {
                    excute && this.handlerPreviewBasicsFile(row)
                    return true
                }
                if (compressEnable) {
                    excute && this.handlerPreviewCompressedFile(row)
                    return true
                }
                return false
            },
            async handlerPreviewBasicsFile (row) {
                this.$refs.previewBasicFileDialog.setData({
                    show: true,
                    title: row.name,
                    projectId: row.projectId,
                    repoName: row.repoName,
                    fullPath: row.fullPath,
                    filePath: row.filePath
                })
            },
            async handlerPreviewCompressedFile (row) {
                if (row.size > 1024 * 1024 * 1024) { // 1GB
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('previewCompressedLimitTips')
                    })
                    return
                }
                this.$refs.compressedFileTable.setData({
                    show: true,
                    title: row.name,
                    projectId: row.projectId,
                    repoName: row.repoName,
                    fullPath: row.fullPath
                })
            },
            // 文件夹内部的搜索，根据文件名或文件夹名搜索
            inFolderSearchFile () {
                this.$router.replace({
                    query: {
                        ...this.$route.query,
                        fileName: this.inFolderSearchName
                    }
                })
                if (!this.inFolderSearchName) {
                    this.searchFullPath = ''
                }
                this.handlerPaginationChange()
            }
        }
    }
</script>
<style lang="scss" scoped>
.repo-generic-container {
    height: 100%;
    overflow: hidden;
    .generic-header{
        height: 60px;
        background-color: white;
        .generic-img {
            border-radius: 4px;
        }
        .generic-title {
            .repo-title {
                max-width: 500px;
                font-size: 16px;
                font-weight: 500;
                color: #081E40;
            }
            // .repo-description {
            //     max-width: 70vw;
            //     padding: 5px 15px;
            //     background-color: var(--bgWeightColor);
            //     border-radius: 2px;
            // }
        }
    }
    .repo-generic-main {
        height: calc(100% - 70px);
        .repo-generic-side {
            height: 100%;
            overflow: hidden;
            background-color: white;
            &-info{
                height: 50px;
                display: flex;
                align-items: center;
                padding-left: 20px;
            }
            .repo-generic-tree {
                border-top: 1px solid var(--borderColor);
                height: calc(100% - 50px);
            }
        }
        .repo-generic-table {
            flex: 1;
            height: 100%;
            width: 0;
            background-color: white;
            .multi-operation {
                height: 50px;
                padding: 10px 20px;
            }
            ::v-deep .selected-header {
                color: var(--fontPrimaryColor);
                .icon-down-shape {
                    color: var(--primaryColor);
                }
                .icon-up-shape {
                    color: var(--primaryColor);
                }
            }
        }
    }
}

::v-deep .bk-table-row.selected-row {
    background-color: var(--bgHoverColor);
}
</style>
