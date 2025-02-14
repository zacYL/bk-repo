<template>
    <div class="topo-left-node-container flex-align-center">
        <div v-if="expand && node.children && node.children.length" class="node-children">
            <topo-left-node class="child-node"
                v-for="child in node.children" :key="child[ckey]"
                :node="child" :ckey="ckey" :node-content="nodeContent">
            </topo-left-node>
        </div>
        <div v-if="node.children && node.children.length" class="node-to flex-align-center">
            <div v-if="expand" class="split-line"></div>
            <i :class="`devops-icon icon-${expand ? 'minus' : 'plus'}-circle`" @click="expand = !expand"></i>
        </div>
        <node-content v-if="!root"></node-content>
        <div v-if="!root" class="node-from flex-column">
            <div class="node-from-top"></div>
            <div class="split-line"></div>
            <div class="node-from-bottom"></div>
        </div>
    </div>
</template>
<script>
    export default {
        name: 'topoLeftNode',
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
                            props: {
                                node: parent.node
                            }
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
.topo-left-node-container {
    position: relative;
    justify-content: flex-end;
    &.child-node {
        padding-right: 30px;
    }
    .split-line {
        width: 30px;
        height: 1px;
        background-color: var(--fontDisableColor);
    }
    .node-from {
        position: absolute;
        height: 100%;
        margin-right: -30px;
        justify-content: flex-end;
        .node-from-top,
        .node-from-bottom {
            flex: 1;
            border-right: 1px solid var(--fontDisableColor);
        }
    }
    .node-to {
        margin-right: -7px;
        justify-content: flex-end;
        .devops-icon {
            padding: 5px;
            margin: -5px;
            font-size: 16px;
            color: var(--primaryColor);
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
