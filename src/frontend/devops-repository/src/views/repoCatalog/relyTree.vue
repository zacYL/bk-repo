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
        @node-click="handleNodeClick"
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
                            children: []
                        }
                        return node
                    })
                    this.treeData = nodeList
                    // if (this.searchNode) {
                    //     console.log('当前选择的是哪个仓库', this.searchNode.repoName)
                    //     this.getTreeData(this.searchNode.projectId, this.searchNode.repoName, this.searchNode.fullPath, this.treeData.find(item => item.name === this.searchNode.repoName))
                    // }
                    if (this.treeData.length > 0) {
                        // 此时需要默认选择第一个仓库
                        this.$emit('clickNode', this.treeData[0])
                    }
                }).finally(() => {
                    this.treeLoading = false
                })
            },
            handleNodeClick (data, node) {
                this.currentClickNode = data
                if (data.folder) {
                    // 当点击的节点是文件夹或仓库时才请求获取其子节点
                    this.getTreeData(data)
                }
                this.$emit('clickNode', data)
                node.expand()
            },

            // 获取树节点的数据
            getTreeData (data) {
                if (data.children !== null && (data.children && data.children.length > 0)) {
                    // 此时是关闭当前展开的节点，需要将当前节点的子节点置为空，否则再次展开时会重新添加子节点导致子节点重复
                    data.children = []
                    return
                }
                this.getSubTreeNodes({
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
                            return node
                        })

                        this.$refs.treeDataRefs.updateKeyChildren(data.id, secondChildren)
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
