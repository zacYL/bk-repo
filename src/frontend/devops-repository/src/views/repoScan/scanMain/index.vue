<template>
    <main style="height: 100%; width: 100%;">
        <bk-tab
            :active.sync="active"
            type="unborder-card"
        >
            <bk-tab-panel
                v-for="(panel, index) in panels"
                v-bind="panel"
                :key="index">
            </bk-tab-panel>
        </bk-tab>
        <div style="display: flex;flex: 1;min-width: 0;min-height: 0;">
            <component :is="active"></component>
        </div>
    </main>
</template>
<script>
    import scanSolution from './components/scanSolution'
    import blackWhiteList from './components/blackWhiteList'
    export default {
        name: 'repo-main',
        components: {
            scanSolution,
            blackWhiteList
        },
        data () {
            return {
                panels: [
                    { name: 'scanSolution', label: this.$t('scanScheme') },
                    { name: 'blackWhiteList', label: this.$t('blackWhiteList') }
                ],
                active: 'scanSolution'
            }
        }
    }
</script>
<style lang="scss" scoped>
main {
    display: flex;
    flex-direction: column;
    background: #fff;
}
::v-deep(.bk-tab-section) {
    display: none;
}
</style>
