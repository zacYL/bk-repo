<template>
    <div class="repo-generic-container" @click="() => selectRow(selectedTreeNode)">
        <header class="mb10 p20 generic-header flex-align-center">
            <Icon class="p5 generic-img" size="80" name="generic" />
            <div class="ml20 generic-title flex-column flex-1">
                <span class="mb10 repo-title text-overflow" :title="replaceRepoName(repoName)">
                    {{ replaceRepoName(repoName) }}
                </span>
                <span class="repo-description" :title="currentRepo.description">
                    {{ currentRepo.description || '--' }}
                </span>
            </div>
        </header>
        <div class="repo-generic-main flex-align-center"
            :style="{ 'margin-left': `${searchFileName ? -sideBarLeft : 0}px` }">
            <div class="repo-generic-side"
                :style="{ 'flex-basis': `${sideBarLeft - 10}px` }"
                v-bkloading="{ isLoading: treeLoading }">
                <div class="important-search">
                    <bk-input
                        v-model.trim="importantSearch"
                        placeholder="请输入关键字，按Enter键搜索"
                        clearable
                        right-icon="bk-icon icon-search"
                        @enter="searchFile"
                        @clear="searchFile">
                    </bk-input>
                </div>
                <repo-tree
                    class="repo-generic-tree"
                    ref="repoTree"
                    :important-search="importantSearch"
                    :open-list="sideTreeOpenList"
                    :selected-node="selectedTreeNode"
                    @icon-click="iconClickHandler"
                    @item-click="itemClickHandler">
                </repo-tree>
            </div>
            <move-split-bar v-model="sideBarLeft" :min-value="210" :width="10"></move-split-bar>
            <div class="repo-generic-table" v-bkloading="{ isLoading }">
                <div class="m10 flex-between-center">
                    <bk-input
                        class="w250"
                        v-if="searchFileName"
                        v-model.trim="importantSearch"
                        placeholder="请输入关键字，按Enter键搜索"
                        clearable
                        right-icon="bk-icon icon-search"
                        @enter="searchFile"
                        @clear="searchFile">
                    </bk-input>
                    <breadcrumb v-else :list="breadcrumb"></breadcrumb>
                    <div class="repo-generic-actions">
                        <bk-button class="mr10"
                            v-for="btn in operationBtns"
                            :key="btn.label"
                            @click.stop="btn.clickEvent()">
                            {{ btn.label }}
                        </bk-button>
                    </div>
                </div>
                <bk-table
                    :data="artifactoryList"
                    height="calc(100% - 104px)"
                    :outer-border="false"
                    :row-border="false"
                    size="small"
                    @row-click="selectRow"
                    @row-dblclick="openFolder">
                    <template #empty>
                        <empty-data :search="Boolean(searchFileName)">
                            <template v-if="!Boolean(searchFileName)">
                                <span class="ml10">暂无文件，</span>
                                <bk-button text @click="handlerUpload">即刻上传</bk-button>
                            </template>
                        </empty-data>
                    </template>
                    <bk-table-column :label="$t('fileName')" prop="name" :render-header="renderHeader">
                        <template #default="{ row }">
                            <div class="flex-align-center">
                                <Icon size="24" :name="row.folder ? 'folder' : getIconName(row.name)" />
                                <div class="ml10 flex-1 text-overflow" :title="row.name">{{row.name}}</div>
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column v-if="searchFileName" :label="$t('path')" prop="fullPath"></bk-table-column>
                    <bk-table-column :label="$t('lastModifiedDate')" prop="lastModifiedDate" width="200" :render-header="renderHeader">
                        <template #default="{ row }">{{ formatDate(row.lastModifiedDate) }}</template>
                    </bk-table-column>
                    <bk-table-column :label="$t('lastModifiedBy')" width="120">
                        <template #default="{ row }">
                            {{ userList[row.lastModifiedBy] ? userList[row.lastModifiedBy].name : row.lastModifiedBy }}
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('size')" width="100">
                        <template #default="{ row }">
                            <bk-button text
                                v-show="row.folder && !row.hasOwnProperty('folderSize')"
                                :disabled="row.sizeLoading"
                                @click="calculateFolderSize(row)">{{ $t('calculate') }}</bk-button>
                            <span v-show="!row.folder || row.hasOwnProperty('folderSize')">
                                {{ convertFileSize(row.size || row.folderSize || 0) }}
                            </span>
                        </template>
                    </bk-table-column>
                </bk-table>
                <bk-pagination
                    class="p10"
                    size="small"
                    align="right"
                    @change="current => handlerPaginationChange({ current })"
                    @limit-change="limit => handlerPaginationChange({ limit })"
                    :current.sync="pagination.current"
                    :limit="pagination.limit"
                    :count="pagination.count"
                    :limit-list="pagination.limitList">
                </bk-pagination>
            </div>
        </div>

        <generic-detail :detail-slider="detailSlider" @refresh="showDetail"></generic-detail>
        <generic-form-dialog ref="genericFormDialog" @submit="submitGenericForm"></generic-form-dialog>
        <generic-tree-dialog ref="genericTreeDialog" @update="updateGenericTreeNode" @submit="submitGenericTree"></generic-tree-dialog>
        <generic-upload-dialog v-bind="uploadDialog" @update="getArtifactories" @cancel="uploadDialog.show = false"></generic-upload-dialog>
    </div>
</template>
<script>
    import Breadcrumb from '@/components/Breadcrumb'
    import MoveSplitBar from '@/components/MoveSplitBar'
    import RepoTree from '@/components/RepoTree'
    import genericDetail from './genericDetail'
    import genericUploadDialog from './genericUploadDialog'
    import genericFormDialog from './genericFormDialog'
    import genericTreeDialog from './genericTreeDialog'
    import { convertFileSize, formatDate } from '@/utils'
    import { getIconName } from '@/store/publicEnum'
    import { mapState, mapMutations, mapActions } from 'vuex'
    export default {
        name: 'repoGeneric',
        components: {
            Breadcrumb,
            MoveSplitBar,
            RepoTree,
            genericDetail,
            genericUploadDialog,
            genericFormDialog,
            genericTreeDialog
        },
        data () {
            return {
                sideBarLeft: 310,
                isLoading: false,
                treeLoading: false,
                importantSearch: this.$route.query.fileName,
                // 左侧树处于打开状态的目录
                sideTreeOpenList: [],
                sortType: 'lastModifiedDate',
                // 中间展示的table数据
                artifactoryList: [],
                // 左侧树选中的节点
                selectedTreeNode: {},
                // 分页信息
                pagination: {
                    count: 0,
                    current: 1,
                    limit: 20,
                    'limit-list': [10, 20, 40]
                },
                // table单击事件，debounce
                rowClickCallback: null,
                // table选中的行
                selectedRow: {},
                // 查看详情
                detailSlider: {
                    show: false,
                    loading: false,
                    folder: false,
                    data: {}
                },
                // 上传制品
                uploadDialog: {
                    show: false,
                    title: '',
                    fullPath: ''
                }
            }
        },
        computed: {
            ...mapState(['repoListAll', 'userList', 'genericTree']),
            projectId () {
                return this.$route.params.projectId
            },
            repoName () {
                return this.$route.query.repoName
            },
            currentRepo () {
                return this.repoListAll.find(repo => repo.name === this.repoName) || {}
            },
            operationBtns () {
                // 是否选中了行
                const isSelectedRow = this.selectedRow.fullPath !== this.selectedTreeNode.fullPath
                // 是否是限制操作仓库，report/log已被过滤
                const isLimit = this.repoName === 'pipeline'
                // 是否选中的是文件夹
                const isFolder = this.selectedRow.folder
                return [
                    { clickEvent: this.showDetail, label: this.$t('detail') },
                    isSelectedRow && !isLimit && { clickEvent: this.renameRes, label: this.$t('rename') },
                    isSelectedRow && !isLimit && { clickEvent: this.moveRes, label: this.$t('move') },
                    isSelectedRow && !isLimit && { clickEvent: this.copyRes, label: this.$t('copy') },
                    isSelectedRow && !isLimit && { clickEvent: this.deleteRes, label: this.$t('delete') },
                    isSelectedRow && !isFolder && { clickEvent: this.handlerShare, label: this.$t('share') },
                    isSelectedRow && { clickEvent: this.handlerDownload, label: this.$t('download') },
                    !isSelectedRow && !isLimit && { clickEvent: this.addFolder, label: this.$t('create') },
                    !isSelectedRow && !isLimit && { clickEvent: this.handlerUpload, label: this.$t('upload') },
                    !isSelectedRow && { clickEvent: this.getArtifactories, label: this.$t('refresh') }
                ].filter(Boolean)
            },
            breadcrumb () {
                const breadcrumb = []
                let node = this.genericTree
                const road = this.selectedTreeNode.roadMap.split(',')
                road.forEach(index => {
                    breadcrumb.push({
                        name: node[index].name,
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
            }
        },
        beforeRouteEnter (to, from, next) {
            // 前端隐藏report仓库/log仓库
            if (to.query.name === 'report' || to.query.name === 'log') {
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
            this.initPage()
        },
        methods: {
            convertFileSize,
            getIconName,
            formatDate,
            ...mapMutations(['INIT_TREE']),
            ...mapActions([
                'getRepoListAll',
                'getNodeDetail',
                'getFolderList',
                'getArtifactoryList',
                'createFolder',
                'getArtifactoryListByQuery',
                'deleteArtifactory',
                'renameNode',
                'moveNode',
                'copyNode',
                'shareArtifactory',
                'getFolderSize',
                'getFileNumOfFolder'
            ]),
            renderHeader (h, { column }) {
                return h('div', {
                    class: 'flex-align-center hover-btn',
                    on: {
                        click: () => {
                            this.sortType = column.property
                            this.handlerPaginationChange()
                        }
                    }
                }, [
                    h('span', column.label),
                    h('i', {
                        class: {
                            'ml5 devops-icon icon-down-shape': true,
                            'selected': this.sortType === column.property
                        }
                    })
                ])
            },
            initPage () {
                this.INIT_TREE([{
                    name: this.replaceRepoName(this.repoName),
                    fullPath: '',
                    folder: true,
                    children: [],
                    roadMap: '0'
                }])
                this.itemClickHandler(this.genericTree[0])
            },
            // 获取中间列表数据
            getArtifactories () {
                this.isLoading = true
                const ajax = this.searchFileName ? this.getArtifactoryListByQuery({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    name: this.searchFileName,
                    current: this.pagination.current,
                    limit: this.pagination.limit,
                    sortType: this.sortType
                }) : this.getArtifactoryList({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    fullPath: this.selectedTreeNode.fullPath,
                    current: this.pagination.current,
                    limit: this.pagination.limit,
                    sortType: this.sortType,
                    isPipeline: this.repoName === 'pipeline'
                })
                ajax.then(({ records, totalRecords }) => {
                    this.pagination.count = totalRecords
                    this.artifactoryList = records.map(v => {
                        return {
                            ...v,
                            // 流水线文件夹名称替换
                            name: (v.metadata && v.metadata.displayName) || v.name
                        }
                    })
                }).finally(() => {
                    this.isLoading = false
                })
            },
            searchFile () {
                if (this.importantSearch || this.searchFileName) {
                    this.$router.replace({
                        query: {
                            ...this.$route.query,
                            fileName: this.importantSearch
                        }
                    })
                    this.handlerPaginationChange()
                }
            },
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getArtifactories()
            },
            // 树组件选中文件夹
            itemClickHandler (node) {
                this.selectedTreeNode = node
                // 取消table行选中样式
                this.selectedRow.element && this.selectedRow.element.classList.remove('selected-row')
                // 初始化table选中行、数据
                this.selectedRow = node
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
            updateGenericTreeNode (item) {
                this.$set(item, 'loading', true)
                this.getFolderList({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    fullPath: item.fullPath,
                    roadMap: item.roadMap,
                    isPipeline: this.repoName === 'pipeline'
                }).finally(() => {
                    this.$set(item, 'loading', false)
                })
            },
            // 双击table打开文件夹
            openFolder (row, $event) {
                if (!row.folder) return
                $event.stopPropagation()
                this.rowClickCallback && clearTimeout(this.rowClickCallback)
                const node = this.selectedTreeNode.children.find(v => v.fullPath === row.fullPath)
                this.itemClickHandler(node)
            },
            // 控制选中的行
            selectRow (row, $event) {
                $event && $event.stopPropagation()
                const element = $event ? $event.currentTarget : null
                this.rowClickCallback && clearTimeout(this.rowClickCallback)
                this.rowClickCallback = window.setTimeout(() => {
                    this.selectedRow.element && this.selectedRow.element.classList.remove('selected-row')
                    element && element.classList.add('selected-row')
                    this.selectedRow = {
                        ...row,
                        element
                    }
                }, 300)
            },
            showDetail () {
                this.detailSlider = {
                    show: true,
                    loading: true,
                    folder: this.selectedRow.folder,
                    data: {}
                }
                this.getNodeDetail({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    fullPath: this.selectedRow.fullPath
                }).then(data => {
                    this.detailSlider.data = {
                        ...data,
                        name: data.name || this.repoName,
                        size: convertFileSize(data.size),
                        createdBy: this.userList[data.createdBy] ? this.userList[data.createdBy].name : data.createdBy,
                        createdDate: formatDate(data.createdDate),
                        lastModifiedBy: this.userList[data.lastModifiedBy] ? this.userList[data.lastModifiedBy].name : data.lastModifiedBy,
                        lastModifiedDate: formatDate(data.lastModifiedDate)
                    }
                }).finally(() => {
                    this.detailSlider.loading = false
                })
            },
            renameRes () {
                this.$refs.genericFormDialog.setFormData({
                    show: true,
                    loading: false,
                    type: 'rename',
                    name: this.selectedRow.name,
                    title: `${this.$t('rename')} (${this.selectedRow.name})`
                })
            },
            addFolder () {
                this.$refs.genericFormDialog.setFormData({
                    show: true,
                    loading: false,
                    type: 'add',
                    path: this.selectedRow.fullPath + '/',
                    title: `${this.$t('create') + this.$t('folder')}`
                })
            },
            handlerShare () {
                this.$refs.genericFormDialog.setFormData({
                    show: true,
                    loading: false,
                    type: 'share',
                    title: `${this.$t('share')} (${this.selectedRow.name})`,
                    user: [],
                    ip: [],
                    permits: 1,
                    time: 1
                })
            },
            submitGenericForm (data) {
                this.$refs.genericFormDialog.setFormData({ loading: true })
                let message = ''
                let fn = null
                switch (data.type) {
                    case 'add':
                        fn = this.submitAddFolder(data).then(() => {
                            this.updateGenericTreeNode(this.selectedTreeNode)
                        })
                        message = this.$t('create') + this.$t('folder')
                        break
                    case 'rename':
                        fn = this.submitRenameNode(data).then(() => {
                            this.updateGenericTreeNode(this.selectedTreeNode)
                        })
                        message = this.$t('rename')
                        break
                    case 'share':
                        fn = this.submitShareArtifactory(data)
                        message = this.$t('share')
                        break
                }
                fn.then(() => {
                    this.selectRow(this.selectedTreeNode)
                    this.getArtifactories()
                    this.$bkMessage({
                        theme: 'success',
                        message: message + this.$t('success')
                    })
                    this.$refs.genericFormDialog.setFormData({ show: false })
                }).finally(() => {
                    this.$refs.genericFormDialog.setFormData({ loading: false })
                })
            },
            submitAddFolder (data) {
                return this.createFolder({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    fullPath: data.path
                })
            },
            submitRenameNode (data) {
                return this.renameNode({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    fullPath: this.selectedRow.fullPath,
                    newFullPath: this.selectedRow.fullPath.replace(/[^/]*$/, data.name)
                })
            },
            submitShareArtifactory (data) {
                return this.shareArtifactory({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    fullPathSet: [this.selectedRow.fullPath],
                    type: 'DOWNLOAD',
                    host: `${location.origin}/web/generic`,
                    needsNotify: true,
                    ...(data.ip.length ? { authorizedIpSet: data.ip } : {}),
                    ...(data.user.length ? { authorizedUserSet: data.user } : {}),
                    ...(Number(data.time) > 0 ? { expireSeconds: Number(data.time) * 86400 } : {}),
                    ...(Number(data.permits) > 0 ? { permits: Number(data.permits) } : {})
                })
            },
            async deleteRes () {
                if (!this.selectedRow.fullPath) return
                let totalRecords
                if (this.selectedRow.folder) {
                    totalRecords = await this.getFileNumOfFolder({
                        projectId: this.projectId,
                        repoName: this.repoName,
                        fullPath: this.selectedRow.fullPath
                    })
                }
                this.$confirm({
                    theme: 'danger',
                    message: `${this.$t('confirm') + this.$t('delete')}${this.selectedRow.folder ? this.$t('folder') : this.$t('file')} ${this.selectedRow.name} ？`,
                    subMessage: `${this.selectedRow.folder && totalRecords ? `当前文件夹下存在${totalRecords}个文件` : ''}`,
                    confirmFn: () => {
                        return this.deleteArtifactory({
                            projectId: this.projectId,
                            repoName: this.repoName,
                            fullPath: this.selectedRow.fullPath
                        }).then(() => {
                            this.selectRow(this.selectedTreeNode)
                            this.updateGenericTreeNode(this.selectedTreeNode)
                            this.getArtifactories()
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('delete') + this.$t('success')
                            })
                        })
                    }
                })
            },
            moveRes () {
                this.$refs.genericTreeDialog.setTreeData({
                    show: true,
                    type: 'move',
                    title: `${this.$t('move')} (${this.selectedRow.name})`,
                    openList: ['0'],
                    selectedNode: this.genericTree[0]
                })
            },
            copyRes () {
                this.$refs.genericTreeDialog.setTreeData({
                    show: true,
                    type: 'copy',
                    title: `${this.$t('copy')} (${this.selectedRow.name})`,
                    openList: ['0'],
                    selectedNode: this.genericTree[0]
                })
            },
            submitGenericTree (data) {
                this.$refs.genericTreeDialog.setTreeData({ loading: true })
                this[data.type + 'Node']({
                    body: {
                        srcProjectId: this.projectId,
                        srcRepoName: this.repoName,
                        srcFullPath: this.selectedRow.fullPath,
                        destProjectId: this.projectId,
                        destRepoName: this.repoName,
                        destFullPath: `${data.selectedNode.fullPath || '/'}`,
                        overwrite: false
                    }
                }).then(() => {
                    this.$refs.genericTreeDialog.setTreeData({ show: false })
                    this.selectRow(this.selectedTreeNode)
                    // 更新源和目的的节点信息
                    this.updateGenericTreeNode(this.selectedTreeNode)
                    this.updateGenericTreeNode(data.selectedNode)
                    this.getArtifactories()
                    this.$bkMessage({
                        theme: 'success',
                        message: data.type + this.$t('success')
                    })
                }).finally(() => {
                    this.$refs.genericTreeDialog.setTreeData({ loading: false })
                })
            },
            handlerUpload () {
                this.uploadDialog = {
                    show: true,
                    title: `${this.$t('upload')} (${this.selectedTreeNode.fullPath || '/'})`,
                    fullPath: this.selectedTreeNode.fullPath
                }
            },
            handlerDownload () {
                const url = `/generic/${this.projectId}/${this.repoName}/${this.selectedRow.fullPath}?download=true`
                this.$ajax.head(url).then(() => {
                    window.open(
                        '/web' + url,
                        '_self'
                    )
                }).catch(e => {
                    this.$bkMessage({
                        theme: 'error',
                        message: e.status !== 404 ? e.message : this.$t('fileNotExist')
                    })
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
            }
        }
    }
</script>
<style lang="scss" scoped>
.repo-generic-container {
    height: 100%;
    overflow: hidden;
    .generic-header{
        height: 130px;
        background-color: white;
        .generic-img {
            width: 110px;
            height: 90px;
            border-radius: 4px;
            box-shadow: 0px 3px 5px 0px rgba(217, 217, 217, 0.5);
        }
        .generic-title {
            overflow: hidden;
            .repo-title {
                max-width: 500px;
                font-size: 20px;
                font-weight: bold;
                color: var(--fontPrimaryColor);
            }
            .repo-description {
                max-width: 600px;
                display: -webkit-box;
                -webkit-line-clamp: 3;
                -webkit-box-orient: vertical;
                overflow: hidden;
            }
        }
    }
    .repo-generic-main {
        height: calc(100% - 140px);
        user-select: none;
        .repo-generic-side {
            height: 100%;
            background-color: white;
            .important-search {
                padding: 10px;
                border-bottom: 1px solid var(--borderWeightColor);
            }
            .repo-generic-tree {
                height: calc(100% - 53px);
            }
        }
        .repo-generic-table {
            flex: 1;
            height: 100%;
            background-color: white;
            ::v-deep .devops-icon {
                font-size: 16px;
                &.disabled {
                    color: var(--fontTipColor);
                    cursor: not-allowed;
                }
                &.icon-down-shape {
                    color: var(--fontSubsidiaryColor);
                    &.selected {
                        color: var(--fontPrimaryColor);
                    }
                }
            }
        }
    }
}

::v-deep .bk-table-row.selected-row {
    background-color: var(--bgColor);
}
</style>
