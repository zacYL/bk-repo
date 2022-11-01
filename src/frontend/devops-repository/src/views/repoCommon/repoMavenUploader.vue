<template>
    <bk-sideslider
        :is-show.sync="isVisible"
        :quick-close="true"
        :width="600"
        @hidden="handleClickClose"
    >
        <div slot="header">{{ customSettings.title }}</div>
        <div class="p20" slot="content">
            自定义内容
        </div>
    </bk-sideslider>
</template>

<script>
    export default {
        model: {
            prop: 'isVisible',
            event: 'update'
        },
        props: {
            isVisible: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                customSettings: {
                    isShow: false,
                    title: '更多参数配置'
                }
            }
        },
        methods: {
            handleClickClose () {
                this.$emit('update', false)
            }
        }
    }
</script>

<style lang="scss" scoped>

</style>
