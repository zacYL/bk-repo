<template>
    <div class="topo-container">
        <i class="devops-icon icon-full-screen hover-btn topo-full-screen" @click.stop="fullScreen"></i>
        <div class="topo-view" ondragstart="return false" :style="{
            cursor: startMove ? 'grabbing' : 'grab'
        }">
            <div class="topo-main flex-center">
                <topo-left-node :node="leftTree" ckey="title" :node-content="nodeContent" root></topo-left-node>
                <node-content style="background-color:var(--bgLightColor);" :node="rootNode"></node-content>
                <topo-node :node="rightTree" ckey="title" :node-content="nodeContent" root></topo-node>
            </div>
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
                },
                isFullScreen: false
            }
        },
        mounted () {
            const dom = this.$el.querySelector('.topo-view')
            const { scrollHeight, clientHeight, scrollWidth, clientWidth } = dom
            dom.scrollTop = (scrollHeight - clientHeight) / 2
            dom.scrollLeft = (scrollWidth - clientWidth) / 2

            dom.addEventListener('mousedown', this.moveStart)
            dom.addEventListener('mousemove', this.moving)
            window.addEventListener('mouseup', this.moveEnd)
        },
        beforeDestroy () {
            window.removeEventListener('mouseup', this.moveEnd)
        },
        methods: {
            moveStart (e) {
                this.startMove = true
                // 确定起始位置
                const { scrollTop, scrollLeft } = this.$el.querySelector('.topo-view')
                this.startPosition = {
                    scrollTop,
                    scrollLeft,
                    top: e.clientY,
                    left: e.clientX
                }
            },
            moving (e) {
                if (!this.startMove) return
                const dom = this.$el.querySelector('.topo-view')
                const { scrollHeight, clientHeight, scrollWidth, clientWidth } = dom
                const maxScrollTop = scrollHeight - clientHeight
                const maxScrollLeft = scrollWidth - clientWidth

                const moveX = e.clientX - this.startPosition.left
                const moveY = e.clientY - this.startPosition.top
                const endX = this.startPosition.scrollLeft - moveX
                const endY = this.startPosition.scrollTop - moveY
                dom.scrollLeft = moveX < 0 ? Math.min(endX, maxScrollLeft) : Math.max(0, endX)
                dom.scrollTop = moveY < 0 ? Math.min(endY, maxScrollTop) : Math.max(0, endY)
            },
            moveEnd () {
                this.startMove = false
            },
            fullScreen () {
                this.isFullScreen = !this.isFullScreen
                if (this.isFullScreen) {
                    this.$el.requestFullscreen()
                } else {
                    document.exitFullscreen()
                }
            }
        }
    }
</script>
<style lang="scss" scoped>
.topo-container {
    position: relative;
    height: 100%;
    background-color: white;
    .topo-full-screen {
        position: absolute;
        z-index: 1;
        top: 0px;
        right: 0px;
        padding: 10px;
        cursor: pointer;
        &:hover {
            background-color: var(--bgHoverLighterColor);
        }
    }
    .topo-view {
        position: relative;
        height: 100%;
        overflow: auto;
        .topo-main {
            position: absolute;
            min-width: 100%;
            min-height: 100%;
            padding: 10px;
        }
    }
}
</style>
