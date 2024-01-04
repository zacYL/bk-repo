<template>
    <div class="repo-rely-container">
        <header class="mb10 pl20 pr20 rely-header flex-align-center">
            <bk-input
                class="search-common"
                v-model.trim="fileNameSearch"
                :placeholder="$t('fileNamePlaceholder')"
                clearable
                right-icon="bk-icon icon-search"
                @enter="onSearchFile"
                @clear="() => {
                    searchFlag = false
                }">
            </bk-input>
            <bk-select v-model="checkRepoType" :placeholder="$t('repositoryTypePlaceholder')" class="search-common" @change="onChangeRepoType">
                <bk-option
                    v-for="option in depotList"
                    :key="option.value"
                    :id="option.value"
                    :name="option.label">
                    <div class="flex-align-center">
                        <Icon size="20" :name="option.value" />
                        <span class="ml10 flex-1 text-overflow">{{option.label}}</span>
                    </div>
                </bk-option>
            </bk-select>
            <bk-button v-if="searchFlag" @click="searchFlag = !searchFlag">{{$t('backToDirectoryTree')}}</bk-button>
        </header>
        <div class="repo-rely-main flex-align-center" v-if="!searchFlag">
            <div class="repo-rely-side"
                :style="{ 'flex-basis': `${sideBarWidth}px` }">
                <div class="repo-rely-side-info">
                    <span>{{$t('directoryList')}}</span>
                </div>
                <div class="repo-rely-side-tree">
                    <relyTree @clickNode="onClickNode" :check-type="checkRepoType" :search-node="searchNode" :key="checkRepoType" @searchFinish="onSearchFinish"></relyTree>
                </div>
            </div>
            <move-split-bar
                :left="sideBarWidth"
                :width="moveBarWidth"
                @change="changeSideBarWidth"
            />
            <div class="repo-rely-content" v-bkloading="{ isLoading: basicTabLoading }">
                <div class="repo-rely-content-header" v-if="baseDetailInfo">
                    <Icon size="14" :name="currentRepoType.toLowerCase()" />
                    <div class="ml10" :title="replaceRepoName(baseDetailInfo.repoName || baseDetailInfo.name)">
                        {{ replaceRepoName(baseDetailInfo.repoName || baseDetailInfo.name) }}
                    </div>
                </div>
                <div class="repo-rely-content-show">
                    <bk-tab :active.sync="active" type="unborder-card">
                        <bk-tab-panel
                            name="basic" :label="$t('baseInfo')" v-bkloading="{ isLoading: basicTabLoading }">
                            <basic-info v-if="baseDetailInfo && baseDetailInfo.name" :node-type="currentNodeType" :detail-info="baseDetailInfo"></basic-info>
                        </bk-tab-panel>
                    </bk-tab>
                    <div class="repo-rely-content-option">
                        <bk-button
                            v-if="baseDetailInfo && currentNodeType === 'file' && !baseDetailInfo.forbidStatus"
                            theme="default"
                            :title="$t('download')"
                            size="small"
                            class="mr10 "
                            @click="onDownloadFile">
                            {{$t('download')}}
                        </bk-button>
                        <bk-button
                            v-if="currentNodeType === 'file' && (applyFileTypes[fileObj.type] || fileObj.type.includes('sha') ) && baseDetailInfo.repoName"
                            theme="default"
                            :title="$t('preview')"
                            size="small"
                            @click="onPreviewFile">
                            {{$t('preview')}}
                        </bk-button>
                    </div>
                </div>
            </div>
        </div>
        <div class="repo-table-container" v-if="searchFlag" v-bkloading="{ isLoading: tableLoading }">
            <bk-table
                class="mt10"
                height="calc(100% - 53px)"
                :data="repoSearchTable"
                :outer-border="false"
                :row-border="false"
                size="small">
                <template #empty>
                    <empty-data :is-loading="isLoading" :search="Boolean(fileNameSearch)"></empty-data>
                </template>
                <bk-table-column :label="$t('folderOrFileName')" prop="name" show-overflow-tooltip>
                    <template #default="{ row }">
                        <div class="hover-btn" @click="onClickTableItem(row)">{{row.name}}</div>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('path')" prop="fullPath" width="350" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="$t('repository')" width="200">
                    <template #default="{ row }">{{ row.repoName }}</template>
                </bk-table-column>
                <bk-table-column :label="$t('lastModifiedDate')" prop="lastModifiedDate">
                    <template #default="{ row }">{{ formatDate(row.lastModifiedDate) }}</template>
                </bk-table-column>
            </bk-table>
            <bk-pagination
                class="p10"
                size="small"
                align="center"
                show-total-count
                @change="current => handlerPaginationChange({ current })"
                @limit-change="limit => handlerPaginationChange({ limit })"
                :current.sync="pagination.current"
                :limit="pagination.limit"
                :count="pagination.count"
                :limit-list="pagination.limitList">
            </bk-pagination>
        </div>
        <canway-dialog
            v-if="baseDetailInfo && baseDetailInfo.fullPath"
            :value="previewDialogShow"
            width="800"
            height-num="600"
            render-directive="if"
            :title="baseDetailInfo.name + $t('space') + ' ' + $t('fileContent')"
            @cancel="cancel">
            <template #default>
                <!-- 因为dialog关闭之后没有销毁，所以当同一个文件多次预览时会有上次的文件内容，为了解决此问题，使用了 Math.random() -->
                <div class="catalog-file-content" :key="baseDetailInfo && baseDetailInfo.fullPath + Math.random()" v-bkloading="{ isLoading: fileContentLoading }">
                    <div id="markdown-container"></div>
                </div>
            </template>
            <template #footer>
                <bk-button @click="cancel">{{$t('close')}}</bk-button>
            </template>
        </canway-dialog>
    </div>
</template>
<script>
    import MoveSplitBar from '@repository/components/MoveSplitBar'
    import basicInfo from '@repository/views/repoCatalog/basicInfo'
    import relyTree from '@repository/views/repoCatalog/relyTree'
    import { convertFileSize, formatDate, convertFileByteSize } from '@repository/utils'
    import { mapState, mapActions } from 'vuex'
    import { repoEnum, fileTypeList } from '@repository/store/publicEnum'
    export default {
        name: 'repoGeneric',
        components: {
            MoveSplitBar,
            basicInfo,
            relyTree
        },
        data () {
            return {
                repoEnum,
                sideBarWidth: 400, // 当前页面左侧的宽度
                moveBarWidth: 10, // 每次移动的距离
                treeLoading: false,
                fileNameSearch: '', // 搜索的文件名称
                searchFlag: false, // 是否是搜索事件
                searchNode: '', // 搜索后点击的节点
                depotList: repoEnum.filter(item => item.value !== 'generic'),
                checkRepoType: '',
                active: 'basic', // 当前选中的标签页
                currentNodeType: 'depot', // 当前选中的节点类型
                repoName: '', // 当前选择的节点名称
                baseDetailInfo: null, // 当前选择节点的详情
                currentRepoType: '', // 当前选择的节点的仓库类型
                fileContent: null, // 当前点击的文件内容
                // 当前点击的文件(大小和类型)
                fileObj: {
                    size: 0,
                    type: ''
                },
                // 可接受显示文件内容的类型
                applyFileTypes: fileTypeList,
                maxFileSize: 1024 * 1024 * 1, // 1MB ，文件大小限制，超过此限制则不显示文件具体文件内容，提示需要下载
                jsonData: '',
                basicTabLoading: false, // 基础信息tab页加载中状态
                fileContentLoading: false, // 文件内容tab页加载中状态
                repoSearchTable: [], // 搜索展示列表
                // 分页信息
                pagination: {
                    count: 0,
                    current: 1,
                    limit: 20,
                    limitList: [10, 20, 40]
                },
                tableLoading: false,
                previewDialogShow: false
            }
        },
        computed: {
            ...mapState(['repoListAll', 'userList', 'permission', 'genericTree']),
            projectId () {
                return this.$route.params.projectId
            }

        },
        methods: {
            convertFileSize,
            formatDate,
            ...mapActions([
                'getNodeDetail',
                'getRepoInfo',
                'getRepoInfoDetail',
                'getTableListByName'
            ]),
            onSearchFile () {
                // 此时展示搜索table页面
                this.searchFlag = true
                this.pagination.current = 1
                this.pagination.limit = 20
                this.getTableList()
            },
            onChangeRepoType () {
                if (this.searchFlag) {
                    this.pagination.current = 1
                    this.pagination.limit = 20
                    this.getTableList()
                }
            },
            // 获取当前搜索的结果
            getTableList () {
                this.tableLoading = true
                const params = {
                    projectId: this.projectId,
                    name: this.fileNameSearch.trim() || '',
                    current: this.pagination.current || 1,
                    limit: this.pagination.limit || 20
                }
                if (this.checkRepoType.length > 0) {
                    params.repoType = [this.checkRepoType]
                }
                this.getTableListByName(
                    params
                ).then(res => {
                    this.repoSearchTable = res.records || []
                    this.pagination.count = res.totalRecords || 0
                    this.pagination.current = res.page
                    this.pagination.limit = res.pageSize
                }).finally(() => {
                    this.tableLoading = false
                })
            },
            // 处理分页数据
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getTableList()
            },
            onClickTableItem (item) {
                this.searchNode = item
                this.searchFlag = false
                this.basicTabLoading = true
            },
            // 搜索完成之后的操作
            onSearchFinish (flag) {
                // 在点击到之前选择的节点后将右部分的loading状态取消
                this.basicTabLoading = flag
                // 将搜索选中的节点置为空
                this.searchNode = ''
                // 将文件名称搜索框置为空
                this.fileNameSearch = ''
            },
            onClickNode (node) {
                this.basicTabLoading = true
                this.currentRepoType = ''
                this.active = 'basic'
                this.fileContent = null
                this.jsonData = null
                this.fileObj = {
                    type: '',
                    size: 0
                }
                if (node.repoName === node.name && this.depotList.map(item => item.value.toUpperCase()).includes(node.type)) {
                    this.currentNodeType = 'depot'
                    this.currentRepoType = node.type
                    this.getRepoDetail(node.projectId, node.repoName, node.type)
                } else {
                    if (node.id !== node.name && node.folder) {
                        this.currentNodeType = 'folder'
                    } else {
                        this.currentNodeType = 'file'
                        if (node.name.lastIndexOf('.') !== -1) {
                            this.fileObj.type = node.name.substr(node.name.lastIndexOf('.') + 1)?.toLowerCase()
                        } else {
                            // 此时表明当前文件名不带点. ,此时文件类型设置为未知，否则会导致 shaxxx 没有后缀的文件认为是有sha后缀的，导致可以预览
                            this.fileObj.type = 'unknown'
                        }
                    }
                    this.getCurrentNodeDetail(node.projectId, node.repoName, node.fullPath)
                }
            },
            //    获取仓库详情
            getRepoDetail (projectId, repoName, repoType) {
                this.getRepoInfo({
                    projectId: projectId,
                    repoName: repoName,
                    repoType: repoType
                }).then(res => {
                    this.baseDetailInfo = {
                        ...res,
                        ...res.configuration.settings,
                        name: res.name || repoName,
                        createdBy: res.createdBy,
                        createdDate: formatDate(res.createdDate),
                        lastModifiedBy: res.lastModifiedBy,
                        lastModifiedDate: formatDate(res.lastModifiedDate),
                        override: {
                            switcher: false,
                            isFlag: true
                        }
                    }
                    if (res.type === 'MAVEN' || res.type === 'NPM') {
                        switch (res.coverStrategy) {
                            case 'COVER':
                                this.baseDetailInfo.override.switcher = true
                                this.baseDetailInfo.override.isFlag = true
                                break
                            case 'UNCOVER':
                                this.baseDetailInfo.override.switcher = true
                                this.baseDetailInfo.override.isFlag = false
                                break
                            default:
                                this.baseDetailInfo.override.switcher = false
                                this.baseDetailInfo.override.isFlag = true
                        }
                    }
                    // 虚拟仓库，添加可选仓库穿梭框及上传目标仓库下拉框
                    if (res.category === 'VIRTUAL') {
                        this.baseDetailInfo.virtualStoreList = res.configuration.repositoryList
                        // 当后台返回的字段为null时需要将其设置为空字符串，否则会因为组件需要的参数类型不对应，导致选择框的placeholder不显示
                        // this.baseDetailInfo.deploymentRepo = res.configuration.deploymentRepo || ''
                    }
                    // 远程仓库，添加地址，账号密码和网络代理相关配置
                    if (res.category === 'REMOTE') {
                        this.baseDetailInfo.url = res.configuration.url
                        this.baseDetailInfo.credentials = res.configuration.credentials
                        if (res.configuration.network.proxy === null) {
                            this.baseDetailInfo.network = {
                                proxy: {
                                    host: null,
                                    port: null,
                                    username: null,
                                    password: null
                                },
                                switcher: false
                            }
                        } else {
                            this.baseDetailInfo.network = {
                                proxy: res.configuration.network.proxy,
                                switcher: true
                            }
                        }
                    }
                }).finally(() => {
                    if (!this.searchNode && !this.searchNode.id) {
                        this.basicTabLoading = false
                    }
                })
            },
            // 获取当前选择的节点的详情信息
            getCurrentNodeDetail (projectId, repoName, fullPath) {
                this.getNodeDetail({
                    projectId: projectId,
                    repoName: repoName,
                    fullPath: fullPath
                }).then(res => {
                    this.baseDetailInfo = {
                        ...res,
                        name: res.name || '',
                        size: res.size && convertFileSize(res.size),
                        createdBy: this.userList[res.createdBy] ? this.userList[res.createdBy]?.name : res.createdBy,
                        createdDate: res.createdDate && formatDate(res.createdDate),
                        lastModifiedBy: this.userList[res.lastModifiedBy] ? this.userList[res.lastModifiedBy]?.name : res.lastModifiedBy,
                        lastModifiedDate: res.lastModifiedDate && formatDate(res.lastModifiedDate),
                        forbidStatus: res.metadata.forbidStatus
                    }
                    this.getRepoInfoDetail({
                        projectId: projectId,
                        repoName: repoName
                    }).then(res => {
                        this.currentRepoType = res.type
                    })
                }).finally(() => {
                    if (!this.searchNode && !this.searchNode.id) {
                        this.basicTabLoading = false
                    }
                })
            },
            // 预览文件内容
            onPreviewFile () {
                if (convertFileByteSize(this.baseDetailInfo.size) <= this.maxFileSize) {
                    this.previewDialogShow = true
                    this.fileContent = ''
                    this.onDownloadFile('fileContent')
                } else {
                    this.$bkMessage({
                        theme: 'warning',
                        limit: 3,
                        message: this.$t('previewErrorMsg')
                    })
                }
            },
            cancel () {
                this.previewDialogShow = false
            },

            // 下载文件或 获取文件内容
            onDownloadFile (type) {
                this.fileContentLoading = true
                const url = `/repository/api/list/${this.baseDetailInfo.projectId}/${this.baseDetailInfo.repoName}/${encodeURIComponent(this.baseDetailInfo.fullPath)}?download=true`
                this.$ajax.head(url).then(() => {
                    if (type === 'fileContent') {
                        this.$ajax({ url: url, method: 'GET', responseType: 'blob' }).then(res => {
                            this.fileObj.size = res.size
                            if (this.fileObj.size < this.maxFileSize) {
                                const reader = new FileReader()
                                reader.onload = (e) => {
                                    // 使用cherry-markdown解析
                                    const markdownInstance = new window.Cherry({
                                        id: 'markdown-container',
                                        editor: {
                                            theme: 'light',
                                            defaultModel: 'previewOnly'
                                        },
                                        engine: {
                                            syntax: {
                                                codeBlock: {
                                                    theme: 'default',
                                                    wrap: true, // 超出长度是否换行，false则显示滚动条
                                                    lineNumber: true // 默认显示行号
                                                }
                                            }
                                        }
                                    })
                                    this.fileContent = reader.result || e.target.result || this.$t('loadFileContentErrorTip')
                                    // markdown 或者是 sha*的文件统一直接用 markdown 展示
                                    // json文件存在部分没有格式化的，此时需要格式化一下
                                    if (this.applyFileTypes[this.fileObj.type] === 'json') {
                                        this.fileContent = JSON.stringify(JSON.parse(this.fileContent), null, '\t')
                                    }
                                    // markdown 文档直接展示，其他的使用markdown的代码块，即下方的转义 ```
                                    // 注意：文档内容结束之后需要再次使用 \n 换行之后才能转移结束的 ```，否则会导致解析不成功
                                    const content = this.applyFileTypes[this.fileObj.type] === 'md'
                                        ? `${this.fileContent}`
                                        : `\`\`\`${this.applyFileTypes[this.fileObj.type] || 'js'}\n${this.fileContent}\n\`\`\``
                                    markdownInstance.setValue(content)
                                }
                                reader.readAsText(res)
                            }
                        }).finally(() => {
                            this.fileContentLoading = false
                        })
                    } else {
                        // 下载文件
                        window.open('/web' + url, '_self')
                        this.fileContentLoading = false
                    }
                }).catch(e => {
                    const message = e.status === 423 ? this.$t('fileDownloadError') : this.$t('fileError')
                    this.$bkMessage({
                        theme: 'error',
                        message
                    })
                })
            },
            // 改变左侧树的宽度
            changeSideBarWidth (sideBarWidth) {
                if (sideBarWidth > 260) {
                    this.sideBarWidth = sideBarWidth
                }
            }
        }
    }
</script>
<style lang="scss" scoped>
.repo-rely-container {
    height: 100%;
    overflow: hidden;
    .rely-header{
        height: 50px;
        background-color: white;
    }
    .repo-rely-main {
        height: calc(100% - 60px);
        .repo-rely-side {
            height: 100%;
            overflow: hidden;
            background-color: white;

            .repo-rely-side-info{
                padding: 15px 20px;
                border-bottom:1px solid rgba(203, 213, 224, 0.5);;
            }
            .repo-rely-side-tree{
                height: calc(100% - 50px - 1px - 8*2px);
                overflow-y: auto;
                margin: 8px;
                min-width: 300px;

            }
        }

        .repo-rely-content {
            flex: 1;
            height: 100%;
            background-color: #fff;
            width: calc(100% - 500px);
            &-header{
                display: flex;
                align-items: center;
                padding: 0 0 0 20px;
                height: 50px;
                border-bottom: 1px solid rgba(203, 213, 224, 0.5);
            }

        }
    }
}
.repo-rely-content-show{
height:calc(100% - 50px);
position: relative;
}

.search-common{
    width: 200px;
    margin: 0 10px 0 0;
}
.repo-rely-content-option{
    position: absolute;
    right: 20px;
    top: 12px;
}

.repo-table-container{
    background: #fff;
    height: calc(100% - 60px);

}
.catalog-file-content{
    height:520px;
    overflow:auto;
}
// ::v-deep .bk-table-row.selected-row {
//     background-color: var(--bgHoverColor);
// }
::v-deep .bk-tab-section{
    height: 100%;
}
::v-deep .bk-tab-unborder-card{
    height: 100%;
}
::v-deep .bk-tab-content{
    height: calc(100% - 50px);
    overflow-y: auto;
}
</style>
