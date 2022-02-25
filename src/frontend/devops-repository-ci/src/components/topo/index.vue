<template>
    <div class="topo-view" ondragstart="return false" :style="{
        cursor: startMove ? 'grabbing' : 'grab'
    }">
        <div ref="topoMain" class="topo-main flex-center">
            <topo-left-node :node="leftTree" ckey="title" :node-content="nodeContent" root></topo-left-node>
            <node-content style="background-color:var(--bgLightColor);" :node="rootNode"></node-content>
            <topo-node :node="rightTree" ckey="title" :node-content="nodeContent" root></topo-node>
        </div>
    </div>
</template>
<script>
    import topoNode from './topoNode'
    import topoLeftNode from './topoLeftNode'
    import nodeContent from './nodeContent'
    export default {
        name: 'topo',
        components: { topoNode, topoLeftNode, nodeContent },
        props: {
            rootNode: {
                type: Object,
                default: () => {}
            },
            leftTree: {
                type: Object,
                default: () => {}
            },
            rightTree: {
                type: Object,
                default: () => {}
            }
        },
        data () {
            return {
                nodeContent,
                startMove: false,
                startPosition: {
                    left: 0,
                    top: 0
                }
            }
        },
        mounted () {
            const { scrollHeight, clientHeight, scrollWidth, clientWidth } = this.$el
            this.$el.scrollTop = (scrollHeight - clientHeight) / 2
            this.$el.scrollLeft = (scrollWidth - clientWidth) / 2

            this.$el.addEventListener('mousedown', this.moveStart)
            this.$el.addEventListener('mousemove', this.moving)
            window.addEventListener('mouseup', this.moveEnd)
        },
        beforeDestroy () {
            window.removeEventListener('mouseup', this.moveEnd)
        },
        methods: {
            moveStart (e) {
                this.startMove = true
                // 确定起始位置
                const { scrollTop, scrollLeft } = this.$el
                this.startPosition = {
                    scrollTop,
                    scrollLeft,
                    top: e.clientY,
                    left: e.clientX
                }
            },
            moving (e) {
                if (!this.startMove) return
                const { scrollHeight, clientHeight, scrollWidth, clientWidth } = this.$el
                const maxScrollTop = scrollHeight - clientHeight
                const maxScrollLeft = scrollWidth - clientWidth

                const moveX = e.clientX - this.startPosition.left
                const moveY = e.clientY - this.startPosition.top
                const endX = this.startPosition.scrollLeft - moveX
                const endY = this.startPosition.scrollTop - moveY
                this.$el.scrollLeft = moveX < 0 ? Math.min(endX, maxScrollLeft) : Math.max(0, endX)
                this.$el.scrollTop = moveY < 0 ? Math.min(endY, maxScrollTop) : Math.max(0, endY)
            },
            moveEnd () {
                this.startMove = false
            }
        }
    }
</script>
<style lang="scss" scoped>
.topo-view {
    position: relative;
    height: 100%;
    padding: 10px;
    overflow: auto;
    .topo-main {
        position: absolute;
        min-width: 100%;
        min-height: 100%;
    }
}
</style>
