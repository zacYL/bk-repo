<template>
    <vue-tree
        ref="treeDataRefs"
        :data="treeData"
        node-key="id"
        accordion
        highlight-current
        :render-after-expand="false"
        :default-expanded-keys="defaultExpandKeys"
        :props="defaultProps"
        :expand-on-click-node="false"
        @node-click="handleNodeClick"
        @node-expand="handleNodeExpand"
        v-bkloading="{ isLoading: treeLoading }"
        style="height:100%;"
    >
        <template slot-scope="{ node }">

            <div class=" repo-catalog-tree">
                <Icon size="16" :name="node.data.type ? node.data.type.toLowerCase() : node.data.folder ? 'folder' : 'file'" class="ml5 mr5" />
                <span>
                    {{ node.label }}
                </span>
            </div>
        </template>
    </vue-tree>

</template>
<script>
    import VueTree from '@devops/vue-tree'
    import '@devops/vue-tree/dist/vue-tree.css'
    import { mapActions } from 'vuex'
    import lodash from 'lodash'

    export default {
        name: 'relyTree',
        components: {
            VueTree
        },
        props: {
            checkType: {
                type: String,
                default: '',
                required: false,
                describe: '当前选择的仓库类型'
            },
            searchNode: {
                type: Object,
                default: () => {
                    return {}
                },
                required: false,
                describe: '搜索后点击的节点对象'
            }

        },
        data () {
            return {
                treeData: [],
                defaultProps: {
                    children: 'children',
                    label: 'name'
                },
                currentClickNode: '', // 当前点击的节点
                treeLoading: false,
                defaultExpandKeys: [] // 默认展开的节点的key数组
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {

        },
        created () {
            this.getRelyRoot()
        },
        methods: {
            ...mapActions(['getRepoRelyList', 'getSubTreeNodes']),
            // 获取当前项目下的所有的依赖树的根节点
            getRelyRoot () {
                this.treeLoading = true
                this.getRepoRelyList({ projectId: this.projectId, type: this.checkType || '' }).then(res => {
                    const nodeList = res.map(item => {
                        const node = {
                            id: item.id || item.name,
                            name: item.name,
                            folder: true,
                            fullPath: '/',
                            type: item.type,
                            projectId: item.projectId,
                            repoName: item.name,
                            // 默认为当前节点添加一个子节点，让其显示左边的展开图标按钮
                            children: [{ id: '000000', name: '' }]
                        }
                        return node
                    })
                    this.treeData = nodeList
                    if (!lodash.isEmpty(this.searchNode)) {
                        this.$nextTick(() => {
                            const currentRepoData = this.treeData.find(item => item.name === this.searchNode.repoName)
                            const currentRepoNode = this.$refs.treeDataRefs.getNode(currentRepoData.id)
                            if (currentRepoData && currentRepoNode) {
                                this.handleNodeExpand(currentRepoData, currentRepoNode).then(() => {
                                    // 此时需要考虑到获取仓库的子层级时接口报错后会导致this.searchNode重置为空的情况
                                    if (!lodash.isEmpty(this.searchNode) && this.searchNode.fullPath) {
                                        // 然后去切割 fullPath 为数组，并去除第一个/
                                        const fullPathList = this.searchNode.fullPath.replace(/^\//, '').split('/')
                                        // 调用方法递归找到当前元素的位置
                                        this.getNodePosition(currentRepoData, fullPathList, 0)
                                    }
                                })
                            }
                        })
                    }
                    // 当前存在依赖仓库，并且不是搜索后返回的，此时就需要默认选中第一个仓库
                    if (this.treeData.length > 0 && !this.searchNode.id) {
                        // 此时需要默认选择第一个仓库
                        this.$emit('clickNode', this.treeData[0])
                    }
                }).finally(() => {
                    if (!this.searchNode && !this.searchNode.id) {
                        this.treeLoading = false
                    }
                })
            },
            // 递归找到当前选择的节点所在的层级并点击选中该节点
            getNodePosition (data, fullPathList, i) {
                this.treeLoading = true
                if (i < fullPathList.length) {
                    // 此处因为立即执行此操作时会出现当前数据的children为空数组的情况，
                    // 因此使用setTimeout延迟处理，又因为考虑到可能网络比较差，所以延迟了3秒
                    // 但这个延迟导致loading时间太久，因此使用promise.then()
                    const newData = data.children.find(data => data.name === fullPathList[i])
                    if (newData) {
                        this.handleNodeExpand(newData, this.$refs.treeDataRefs.getNode(newData)).then(() => {
                            i++
                            this.getNodePosition(newData, fullPathList, i)
                        })
                    }
                } else {
                    this.treeLoading = false
                    this.$emit('searchFinish', this.treeLoading)
                    this.defaultExpandKeys = [this.searchNode.id]
                    this.handleNodeClick(data)
                    const currentNode = this.$refs.treeDataRefs.getNode(data)
                    currentNode && (currentNode.isCurrent = true)
                }
            },

            handleNodeClick (data, node) {
                this.currentClickNode = data
                this.$emit('clickNode', data)
                // node.expand()
            },
            handleNodeExpand (data, node) {
                if (data.folder) {
                    // 当点击的节点是文件夹或仓库时才请求获取其子节点
                    return this.getTreeData(data, node)
                } else {
                    // 在当前点击的节点不是文件夹或仓库时不在请求其子级，此时需要返回resolve的promise，否则会导致不能跳出递归
                    return Promise.resolve()
                }
            },

            // 获取树节点的数据
            getTreeData (data, node) {
                // 向当前选择节点添加子节点之前先将之前默认添加的数据清除，否则会出现数据重复或无用数据
                data.children = []
                this.treeLoading = true
                return this.getSubTreeNodes({
                    projectId: data.projectId,
                    repoName: data.repoName,
                    fullPath: data.fullPath
                }).then(res => {
                    if (res.records.length > 0) {
                        const secondChildren = res.records.map((item) => {
                            const node = {
                                id: item.id || item.name,
                                name: item.name,
                                folder: item.folder,
                                fullPath: item.fullPath,
                                type: item.type,
                                projectId: item.projectId,
                                repoName: item.repoName,
                                children: []

                            }
                            if (node.folder) {
                                // 如果当前节点是文件夹，则默认为其添加一个子节点，让其显示左边的展开图标按钮
                                node.children = [{ id: '000000', name: '' }]
                            }
                            return node
                        })

                        this.$refs.treeDataRefs.updateKeyChildren(data.id, secondChildren)
                    }
                }).catch((error) => {
                    this.$bkMessage({
                        message: error.message,
                        theme: 'error'
                    })
                    if (data.folder) {
                        // 在请求报错后为了保持文件夹左侧的图标按钮，需要将之前清空的数组再次默认添加一个数据
                        data.children = [{ id: '000000', name: '' }]
                        // 将当前节点的展开状态改为false，否则会展开当前节点，显示不应该出现的默认节点的无效数据
                        this.$refs.treeDataRefs.getNode(data).expanded = false
                        // 此时默认点击当前需要加载子级的节点
                        if (!lodash.isEmpty(this.searchNode)) {
                            // 调用此方法之后会导致this.searchNode变为空，所以其余地方在操作其对象内部的属性时需要判断是否为空
                            this.$emit('searchFinish', this.treeLoading)
                            //  此时不能在设置默认展开，否则会导致展开当前节点，显示不应该出现的默认节点的无效数据
                            // this.defaultExpandKeys = [data.id]
                            this.handleNodeClick(data)
                            this.$emit('clickNode', data)
                            this.treeLoading = false
                        }
                    }
                }).finally(() => {
                    if (!this.searchNode && !this.searchNode.id) {
                        this.treeLoading = false
                    }
                })
            }

        }
    }
</script>
<style lang="scss" scoped>
.repo-catalog-tree-container{
    width: 100%;
    height: 100%;
}
.repo-catalog-tree{
    width: 100%;
    display: flex;
    align-items: center;
    min-height: 24px;
    cursor: pointer;
}

</style>
