<template>
    <div class="security-config-container">
        <bk-tab class="security-config-tab page-tab" type="unborder-card" :active.sync="tabName">
            <bk-tab-panel name="vuldb" :label="$t('bugDatabasePanel')">
                <scanner-manage></scanner-manage>
            </bk-tab-panel>
            <bk-tab-panel render-directive="if" name="license" :label="$t('licenseManagement')">
                <license-manage></license-manage>
            </bk-tab-panel>
            <!-- <bk-tab-panel render-directive="if" name="whitelist" label="漏洞白名单">
                <cve-white-list></cve-white-list>
            </bk-tab-panel> -->
            <bk-tab-panel render-directive="if" name="proxywhitelist" :label="$t('proxyWhiteList')">
                <proxy-white-list></proxy-white-list>
            </bk-tab-panel>
        </bk-tab>
    </div>
</template>
<script>
    import licenseManage from './licenseManage'
    import scannerManage from './scannerManage'
    // import cveWhiteList from './cveWhiteList'
    import proxyWhiteList from './proxyWhiteList'
    export default {
        name: 'securityConfig',
        components: {
            licenseManage,
            scannerManage,
            // cveWhiteList,
            proxyWhiteList
        },
        data () {
            return { tabName: 'vuldb' }
        }
    }
</script>
<style lang="scss" scoped>
.security-config-container {
    height: 100%;
    background-color: white;
    .security-config-tab {
        height: 100%;
        ::v-deep .bk-tab-section {
            height: calc(100% - 60px);
            padding: 0;
            .bk-tab-content {
                height: 100%;
                overflow: hidden;
            }
        }
    }
}
</style>
