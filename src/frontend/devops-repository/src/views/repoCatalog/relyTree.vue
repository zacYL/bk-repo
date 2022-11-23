<template>
    <vue-tree-list
        ref="relyTreeRef"
        @click="onClickTreeNode"
        :model="treeData"
        v-bind:default-expanded="false"
        v-bkloading="{ isLoading: treeLoading }"
        class="repo-catalog-tree-container"
    >
        <template v-slot:leafNameDisplay="slotProps">
            <div class="repo-catalog-tree" :style="{ 'background': currentClickNode.id === slotProps.model.id ? 'rgba(58, 132, 255, 0.3)' : '' }">

                <Icon size="14" :name="slotProps.model.type ? slotProps.model.type.toLowerCase() : slotProps.model.folder ? 'folder' : 'file'" class="mr10 ml10" />

                <div :title="slotProps.model.name">
                    {{ slotProps.model.name }}
                </div>
            </div>
        </template>
        <span class="icon" slot="treeNodeIcon">
        </span>
    </vue-tree-list>
</template>
<script>
    import { VueTreeList, Tree, TreeNode } from 'vue-tree-list'

    import { mapActions } from 'vuex'
    export default {
        name: 'relyTree',
        components: {
            VueTreeList
        },
        props: {
            checkType: {
                type: String,
                default: '',
                required: false,
                describe: '当前选择的仓库类型'
            }

        },
        data () {
            return {
                treeData: new Tree([]),
                currentClickNode: '', // 当前点击的节点
                treeLoading: false
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
                            children: [],
                            expandFlag: false,
                            disabled: true,
                            dragDisabled: true,
                            addTreeNodeDisabled: true,
                            addLeafNodeDisabled: true,
                            delNodeDisabled: true,
                            editNodeDisabled: true
                        }
                        return node
                    })
                    this.treeData = new Tree(
                        nodeList
                    )
                    if (this.treeData.children.length > 0) {
                        // 此时需要默认选择第一个仓库
                        this.$emit('clickNode', this.treeData.children[0])
                    }
                }).finally(() => {
                    this.treeLoading = false
                })
            },

            // 获取树节点的数据
            getTreeData (projectId, repoName, fullPath, data, node) {
                if (data.children !== null && (data.children && data.children.length > 0)) {
                    // 注意，此处顺序不能交换，必须先收起当前节点，才能将当前节点置为空，否则会导致下次展开节点时不能展开
                    node.toggle()
                    data.children = []
                    return
                }
                this.getSubTreeNodes({
                    projectId: projectId,
                    repoName: repoName,
                    fullPath: fullPath
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
                                children: [],
                                expandFlag: false,
                                disabled: true,
                                dragDisabled: true,
                                addTreeNodeDisabled: true,
                                addLeafNodeDisabled: true,
                                delNodeDisabled: true,
                                editNodeDisabled: true
                            }
                            return node
                        })

                        for (let i = 0; i < secondChildren.length; i++) {
                            data.addChildren(new TreeNode(secondChildren[i]))
                        }
                        node.toggle()
                    }
                })
            },
            // 获取当前路径在当前层级的位置，只可应用于仓库下的文件夹及文件
            getNodePosition (data, path) {
                return data.children.find(item => item.name === path)
            },
            // 递归，找到当前点击的节点的位置，并调用search接口添加子节点
            getNodeData (data, pathList, i, node, newData) {
                if (i < pathList.length) {
                    newData = this.getNodePosition(data, pathList[i])
                    i++

                    this.getNodeData(newData, pathList, i, node, newData)
                } else {
                    this.getTreeData(node.projectId, node.repoName, node.fullPath, newData, node)
                }
            },

            // 点击树节点
            onClickTreeNode (node) {
                this.currentClickNode = node

                if (node.name === node.repoName) {
                    // 表明此时点击的是仓库，加载树节点
                    this.getTreeData(node.projectId, node.repoName, node.fullPath, this.treeData.children.find(item => item.name === node.repoName), node)
                } else {
                    // 此时表明 点击的是子文件夹
                    const fullPathList = node.fullPath.replace(/^\//, '').split('/')

                    if (fullPathList[0] === '') {
                        fullPathList.splice(0, 1)
                    }
                    let tempData
                    this.getNodeData(this.treeData.children.find(item => item.name === node.repoName), fullPathList, 0, node, tempData)
                }
                this.$emit('clickNode', node)
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
}
::v-deep .vtl-node-content{
    cursor: pointer;
    display: flex;
    align-items: center;
    width: 100%;
}
::v-deep .vtl-node-main .vtl-caret {
    display: none;
    pointer-events: none;
}

</style>
