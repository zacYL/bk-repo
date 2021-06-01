<template>
    <div :class="isSearching ? 'p20 search-package-container' : 'flex-center flex-column'">
        <search-input @search="searchPackage"></search-input>
        <search-package v-if="isSearching"></search-package>
        <frequent-repo v-else></frequent-repo>
    </div>
</template>
<script>
    import searchInput from './searchInput'
    import frequentRepo from './frequentRepo'
    import searchPackage from './searchPackage'
    export default {
        name: 'searchOverview',
        components: { searchInput, searchPackage, frequentRepo },
        computed: {
            isSearching () {
                return Boolean(this.$route.query.repoType)
            }
        },
        methods: {
            searchPackage ({ type, name }) {
                this.$router.replace({
                    query: {
                        repoType: type,
                        packageName: name
                    }
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
@import '@/scss/conf';
.search-package-container {
    height: 100%;
}
</style>
