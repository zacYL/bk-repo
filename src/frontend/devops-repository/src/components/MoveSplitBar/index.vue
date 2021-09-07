<template>
    <div class="move-split-bar" draggable="false"></div>
</template>
<script>
    export default {
        name: 'moveSplitBar',
        model: {
            prop: 'value',
            event: 'change'
        },
        props: {
            value: {
                type: Number,
                default: 0
            },
            minValue: {
                type: Number,
                default: 0
            }
        },
        data () {
            return {
                startDrag: false
            }
        },
        mounted () {
            this.$el.addEventListener('mousedown', this.dragDown)
            window.addEventListener('mousemove', this.dragMove)
            window.addEventListener('mouseup', this.dragUp)
        },
        beforeDestroy () {
            window.removeEventListener('mousemove', this.dragMove)
            window.removeEventListener('mouseup', this.dragUp)
        },
        methods: {
            dragDown () {
                this.startDrag = true
            },
            dragMove (e) {
                if (!this.startDrag) return
                const clientX = e.clientX - 40
                if (clientX > this.minValue) this.$emit('change', clientX)
            },
            dragUp () {
                this.startDrag = false
            }
        }
    }
</script>
<style lang="scss" scoped>
.move-split-bar {
    width: 10px;
    height: 100%;
    cursor: col-resize;
}
</style>
