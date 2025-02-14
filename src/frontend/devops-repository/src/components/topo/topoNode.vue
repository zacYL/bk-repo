<template>
    <div class="topo-node-container flex-align-center">
        <div v-if="!root" class="node-from flex-column">
            <div class="node-from-top"></div>
            <div class="split-line"></div>
            <div class="node-from-bottom"></div>
        </div>
        <node-content v-if="!root" :node="node"></node-content>
        <div v-if="node.children && node.children.length" class="node-to flex-align-center">
            <i :class="`devops-icon icon-${expand ? 'minus' : 'plus'}-circle`" @click.stop="expand = !expand"></i>
            <div v-if="expand" class="split-line"></div>
        </div>
        <div v-if="expand && node.children && node.children.length" class="node-children">
            <topo-node class="child-node"
                v-for="child in node.children" :key="child[ckey]"
                :node="child" :ckey="ckey" :node-content="nodeContent">
            </topo-node>
        </div>
    </div>
</template>
<script>
    export default {
        name: 'topoNode',
        components: {
            nodeContent: {
                render (h) {
                    // parent为当前组件
                    const parent = this.$parent
                    if (!parent.nodeContent) {
                        return h(
                            'span',
                            parent.node[parent.ckey]
                        )
                    }
                    return h(
                        parent.nodeContent,
                        {
                            props: this.$attrs
                        }
                    )
                }
            }
        },
        props: {
            root: Boolean,
            node: {
                type: Object,
                default: () => {}
            },
            ckey: {
                type: String,
                default: 'id'
            },
            nodeContent: Object
        },
        data () {
            return {
                expand: true
            }
        }
    }
</script>
<style lang="scss" scoped>
.topo-node-container {
    position: relative;
    &.child-node {
        padding-left: 30px;
    }
    .split-line {
        width: 30px;
        height: 1px;
        background-color: var(--fontDisableColor);
    }
    .node-from {
        position: absolute;
        height: 100%;
        margin-left: -30px;
        .node-from-top,
        .node-from-bottom {
            flex: 1;
            border-left: 1px solid var(--fontDisableColor);
        }
    }
    .node-to {
        margin-left: -7px;
        .devops-icon {
            padding: 5px;
            margin: -5px;
            font-size: 16px;
            color: var(--primaryColor);
            z-index: 1;
            cursor: pointer;
        }
    }
    .child-node:first-child {
        > .node-from {
            .node-from-top {
                border: 0 none;
            }
        }
    }
    .child-node:last-child {
        > .node-from {
            .node-from-bottom {
                border: 0 none;
            }
        }
    }
}
</style>
