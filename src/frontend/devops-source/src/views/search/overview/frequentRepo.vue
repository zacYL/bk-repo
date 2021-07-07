<template>
    <div class="frequent-repo" v-if="frequentRepoList.length">
        <header class="frequent-repo-header flex-align-center">
            <span>常用仓库</span>
            <router-link class="repo-list-link" :to="{ name: 'searchRepoList' }">更多<i class="devops-icon icon-angle-double-right"></i></router-link>
        </header>
        <div class="mt10 frequent-repo-list">
            <div
                class="repo-item flex-center flex-column"
                v-for="repo in frequentRepoList"
                :key="repo.name"
                :title="repo.name"
                @click="toRepoDetail(repo)">
                <Icon size="48" :name="repo.type.toLowerCase()" />
                <span class="repo-name text-overflow">{{ repo.name }}</span>
            </div>
        </div>
    </div>
</template>
<script>
    import { mapState, mapActions } from 'vuex'
    export default {
        name: 'frequentRepo',
        computed: {
            ...mapState(['repoList']),
            frequentRepoList () {
                const frequencyRepoList = localStorage.getItem('_frequencyRepo') ? JSON.parse(localStorage.getItem('_frequencyRepo')) : []
                return (frequencyRepoList.length ? frequencyRepoList : this.repoList).slice(0, 10)
            }
        },
        methods: {
            ...mapActions([
                'getRepoList'
            ]),
            toRepoDetail ({ type, name }) {
                this.$router.push({
                    name: 'searchPackageList',
                    params: {
                        repoType: type.toLowerCase(),
                        repoName: name
                    }
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
@import '@/scss/conf';
.frequent-repo {
    width: 560px;
    margin-top: 75px;
    .frequent-repo-header {
        justify-content: space-between;
        .repo-list-link {
            color: $primaryColor;
        }
    }
    .frequent-repo-list {
        display: grid;
        grid-template: repeat(2, 85px) / repeat(5, 100px);
        grid-gap: 15px;
        width: 560px;
        .repo-item {
            border: 1px solid $borderColor;
            cursor: pointer;
            &:hover {
                border-color: $primaryColor;
            }
            .repo-name {
                max-width: 90px;
            }
        }
    }
}
</style>
