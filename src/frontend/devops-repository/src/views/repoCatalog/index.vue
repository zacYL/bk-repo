<template>
    <div class="repo-rely-container">
        <header class="mb10 pl20 pr20 rely-header flex-align-center">
            <bk-input
                class="search-common"
                v-model.trim="fileNameSearch"
                placeholder="请输入文件名称"
                clearable
                right-icon="bk-icon icon-search"
            >
            </bk-input>

            <bk-select v-model="checkRepoType" placeholder="请选择仓库类型" class="search-common">
                <bk-option
                    v-for="option in depotList"
                    :key="option.id"
                    :id="option.id"
                    :name="option.name.toLowerCase()">
                </bk-option>
            </bk-select>
        </header>
        <div class="repo-rely-main flex-align-center">
            <div class="repo-rely-side"
                :style="{ 'flex-basis': `${sideBarWidth}px` }"

            >
                <div class="repo-rely-side-info">
                    <span>
                        目录列表
                    </span>
                </div>
                <div class="repo-rely-side-tree">
                    <relyTree @clickNode="onClickNode" :check-type="checkRepoType" :key="checkRepoType"></relyTree>
                </div>

            </div>
            <move-split-bar
                :left="sideBarWidth"
                :width="moveBarWidth"
                @change="changeSideBarWidth"
            />
            <div class="repo-rely-content">
                <div class="repo-rely-content-header" v-if="baseDetailInfo">
                    <Icon size="14" :name="currentRepoType.toLowerCase()" />
                    <div class="ml10" :title="replaceRepoName(baseDetailInfo.repoName || baseDetailInfo.name)">
                        {{ replaceRepoName(baseDetailInfo.repoName || baseDetailInfo.name) }}
                    </div>
                </div>
                <div class="repo-rely-content-show">
                    <bk-tab :active.sync="active" type="unborder-card" @tab-change="onChangeTab">
                        <bk-tab-panel
                            name="basic" label="基础信息" v-bkloading="{ isLoading: basicTabLoading }">
                            <basic-info v-if="baseDetailInfo && baseDetailInfo.name" :node-type="currentNodeType" :detail-info="baseDetailInfo"></basic-info>
                        </bk-tab-panel>
                        <!-- 此时需要给tab-panel添加key作为标识，不然渲染的文件内容会出现上次的内容 -->
                        <bk-tab-panel
                            v-if="currentNodeType === 'file' && applyFileTypes[fileObj.type]"
                            :key="baseDetailInfo.fullPath "
                            name="content" label="文件内容" v-bkloading="{ isLoading: fileContentLoading }" :style="{ 'display': fileObj.size > maxFileSize ? 'flex' : '' }" style=" align-items: center; justify-content: center;">

                            <!-- <pre v-if="fileContent && fileObj.size <= maxFileSize"><code class="html">{{fileContent}}</code></pre> -->

                            <json-viewer v-if="jsonData && fileObj.size <= maxFileSize && applyFileTypes[fileObj.type] === 'json'" :value="jsonData"
                                copyable
                                boxed
                                sort
                                expanded
                                show-double-quotes>
                                <template v-slot:copy="{ copied }">{{copied ? '已复制' : '复制'}}</template>
                            </json-viewer>
                            <div v-if="fileObj.size <= maxFileSize && applyFileTypes[fileObj.type] !== 'json'" id="markdown-container"></div>
                            <div v-if=" fileObj.size > maxFileSize">
                                文件过大，请下载文件到本地查看
                            </div>

                        </bk-tab-panel>
                        <bk-button v-if="baseDetailInfo && currentNodeType === 'file'" theme="default" title="下载" size="small" class="mr10 repo-rely-content-download" @click="onDownloadFile">下载</bk-button>

                    </bk-tab>

                </div>
            </div>

        </div>
    </div>
</template>
<script>
    import MoveSplitBar from '@repository/components/MoveSplitBar'
    import basicInfo from '@repository/views/repoCatalog/basicInfo'
    import relyTree from '@repository/views/repoCatalog/relyTree'
    import { convertFileSize, formatDate } from '@repository/utils'
    import { mapState, mapActions } from 'vuex'
    import { depotTypeList, fileTypeList } from '@repository/views/repoCatalog/depotType.js'
    export default {
        name: 'repoGeneric',
        components: {
            MoveSplitBar,
            basicInfo,
            relyTree
        },
        data () {
            return {
                sideBarWidth: 500, // 当前页面左侧的宽度
                moveBarWidth: 10, // 每次移动的距离
                isLoading: false,
                treeLoading: false,
                fileNameSearch: '', // 搜索的文件名称

                depotList: depotTypeList,
                checkRepoType: '',

                active: 'basic', // 当前选中的标签页
                currentNodeType: 'depot', // 当前选中的节点类型
                repoName: '', // 当前选择的节点名称
                nodeLevel: 0, // 节点的层级，因为根节点唯一键是仓库名，子节点就可以用id
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
                fileContentLoading: false // 文件内容tab页加载中状态
            }
        },
        computed: {
            ...mapState(['repoListAll', 'userList', 'permission', 'genericTree']),
            projectId () {
                return this.$route.params.projectId
            }

        },
        watch: {

        },
        created () {
        },

        beforeDestroy () {
        },
        methods: {
            convertFileSize,
            formatDate,
            ...mapActions([
                'getNodeDetail',
                'getRepoInfo',
                'getRepoInfoDetail'
            ]),
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
                if (node.repoName === node.name && this.depotList.map(item => item.name).includes(node.type)) {
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
                            // 此时表明当前文件名不带点. ,此时将整个文件名作为文件类型
                            this.fileObj.type = node.name?.toLowerCase()
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
                }).finally(() => {
                    this.basicTabLoading = false
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
                        name: res.name || this.repoName,
                        size: convertFileSize(res.size),
                        createdBy: res.createdBy,
                        createdDate: formatDate(res.createdDate),
                        lastModifiedBy: res.lastModifiedBy,
                        lastModifiedDate: formatDate(res.lastModifiedDate)
                    }
                    this.getRepoInfoDetail({
                        projectId: projectId,
                        repoName: repoName
                    }).then(res => {
                        this.currentRepoType = res.type
                    })
                }).finally(() => {
                    this.basicTabLoading = false
                })
            },
            // 当选择的是文件内容tab页时，需要调用接口获取二进制流对象
            onChangeTab (name) {
                if (name === 'content') {
                    this.onDownloadFile('fileContent')
                }
            },

            // 下载文件或 获取文件内容
            onDownloadFile (type) {
                this.fileContentLoading = true
                const markdownInstance = new window.Cherry({
                    id: 'markdown-container',
                    // markdown 文件不能按照代码的样式去显示,即Markdown文件直接展示，不需要其他操作
                    editor: {
                        codemirror: {
                            // depend on codemirror theme name: https://codemirror.net/demo/theme.html
                            theme: 'dracula'
                        },
                        defaultModel: 'previewOnly'
                    }
                })

                const url = `/repository/api/list/${this.baseDetailInfo.projectId}/${this.baseDetailInfo.repoName}/${encodeURIComponent(this.baseDetailInfo.fullPath)}?download=true`
                this.$ajax.head(url).then(() => {
                    if (type === 'fileContent') {
                        this.$ajax({ url: url, method: 'GET', responseType: 'blob' }).then(res => {
                            this.fileObj.size = res.size
                            if (this.fileObj.size < this.maxFileSize) {
                                const reader = new FileReader()
                                reader.onload = (e) => {
                                    if (this.fileObj.type === 'json') {
                                        // json文件使用  vue-json-viewer 解析
                                        this.jsonData = reader.result || e.target.result || ''
                                    } else {
                                        // 非json文件使用cherry-markdown解析
                                        this.fileContent = reader.result || e.target.result || ''
                                        const content = this.applyFileTypes[this.fileObj.type] === 'md' ? `${this.fileContent}` : `\`\`\`${this.applyFileTypes[this.fileObj.type] || 'js'}\n${this.fileContent} \`\`\``
                                        markdownInstance.setValue(content)
                                    }
                                }
                                reader.readAsText(res)
                            }
                        }).finally(() => {
                            this.fileContentLoading = false
                        })
                    } else {
                        // 下载文件
                        window.open(
                            '/web' + url,
                            '_self'
                        )
                    }
                }).catch(e => {
                    const message = e.status === 423 ? this.$t('fileDownloadError') : this.$t('fileError')
                    this.$bkMessage({
                        theme: 'error',
                        message
                    })
                })
            },

            //    改变左侧树的宽度
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
.repo-rely-content-download{
    position: absolute;
    right: 50px;
    top: 12px;
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
