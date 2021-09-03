<template>
    <div class="virtual-tree" @scroll="scrollTree($event)">
        <ul class="repo-tree-list">
            <li class="repo-tree-item" :key="item.roadMap" v-for="item of treeList">
                <div class="repo-tree-title hover-btn"
                    :title="item.name"
                    :class="{ 'selected': selectedNode.roadMap === item.roadMap }"
                    :style="{ 'padding-left': 20 * (computedDepth(item) + 1) + 'px' }"
                    @click.stop="itemClickHandler(item)">
                    <i v-if="item.loading" class="mr5 loading spin-icon"></i>
                    <i v-else class="mr5 devops-icon" @click.stop="iconClickHandler(item)"
                        :class="openList.includes(item.roadMap) ? 'icon-down-shape' : 'icon-right-shape'"></i>
                    <icon class="mr5" size="14" :name="openList.includes(item.roadMap) ? 'folder-open' : 'folder'"></icon>
                    <div class="node-text" :title="item.name" v-html="importantTransform(item.name)"></div>
                </div>
            </li>
        </ul>
        <div class="tree-phantom" :style="`height:${totalHeight}px;`"></div>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import { throttle } from '@/utils'
    export default {
        name: 'repo-tree',
        props: {
            importantSearch: {
                type: String,
                default: ''
            },
            selectedNode: {
                type: Object,
                default: () => {}
            },
            openList: {
                type: Array,
                default: () => []
            },
            sortable: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                resizeFn: null,
                size: 0,
                start: 0
            }
        },
        computed: {
            ...mapState(['genericTree']),
            flattenGenericTree () {
                const flatNodes = []
                const flatten = treeData => {
                    treeData.forEach(treeNode => {
                        flatNodes.push(treeNode)
                        this.openList.includes(treeNode.roadMap) && flatten(treeNode.children || [])
                    })
                }
                flatten(this.genericTree)
                return flatNodes
            },
            treeList () {
                const flattenGenericTree = this.flattenGenericTree.filter(v => v.folder)
                return flattenGenericTree.slice(this.start, this.start + this.size)
            },
            totalHeight () {
                return (this.flattenGenericTree.length + 1) * 30
            }
        },
        mounted () {
            this.resizeFn = throttle(this.computedSize)
            this.computedSize()
            window.addEventListener('resize', this.resizeFn)
        },
        beforeDestroy () {
            window.removeEventListener('resize', this.resizeFn)
        },
        methods: {
            scrollTree (e) {
                this.start = Math.floor(e.target.scrollTop / 30)
            },
            computedSize () {
                const height = this.$el.getBoundingClientRect().height
                this.size = Math.ceil(height / 30)
            },
            computedDepth (node) {
                return node.roadMap.split(',').length - 1
            },
            /**
             *  点击icon的回调函数
             */
            iconClickHandler (item) {
                this.$emit('icon-click', item)
            },
            /**
             *  单击folder的回调函数
             */
            itemClickHandler (item) {
                this.$emit('item-click', item)
            },
            importantTransform (name) {
                if (!this.importantSearch) return name
                const normalText = name.split(this.importantSearch)
                return normalText.reduce((a, b) => {
                    return a + `<em>${this.importantSearch}</em>` + b
                })
            }
        }
    }
</script>

<style lang="scss">
.virtual-tree {
    position: relative;
    display: flex;
    align-items: flex-start;
    height: 100%;
    overflow: auto;
    .repo-tree-list {
        position: sticky;
        top: 0;
        width: 100%;
        height: 100%;
        overflow: hidden;
    }
    &::-webkit-scrollbar {
        width: 12px;
        background-color: var(--bgLightColor);
    }
    &::-webkit-scrollbar-thumb {
        border-radius: initial;
    }
}
.repo-tree-item {
    position: relative;
    color: var(--fontBoldColor);
    font-size: 12px;
    .line-dashed {
        position: absolute;
        border-color: var(--borderLightColor);
        border-style: dashed;
        z-index: 1;
    }
    &:last-child > .line-dashed {
        height: 30px!important;
    }
    .repo-tree-title {
        position: relative;
        height: 30px;
        display: flex;
        align-items: center;
        .loading {
            display: inline-block;
            width: 12px;
            height: 12px;
            border: 1px solid;
            border-right-color: transparent;
            border-radius: 50%;
            z-index: 1;
        }
        .devops-icon {
            color: var(--fontColor);
            z-index: 1;
        }
        .node-text {
            max-width: 150px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            em {
                font-style: normal;
                font-weight: bold;
                background-color: #edf45d;
            }
        }
        &.selected {
            background-color: var(--primaryLightColor);
            color: var(--primaryColor);
        }
    }
}
</style>
