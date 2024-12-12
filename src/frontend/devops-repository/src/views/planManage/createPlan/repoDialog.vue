<!--
 * @Author: xiaoshanwen
 * @Date: 2024-08-16 17:30:26
 * @LastEditTime: 2024-12-12 18:30:57
 * @Description:
 * @FilePath: /artifact/src/frontend/devops-repository/src/views/planManage/createPlan/repoDialog.vue
-->
<template>
    <canway-dialog
        :value="show"
        width="800"
        height-num="561"
        :title="$t('addRepo')"
        @cancel="$emit('cancel')"
        @confirm="confirmPackageData">
        <bk-transfer
            :title="[$t('repositoryList'), $t('selectedRepo')]"
            :source-list="repoList"
            :target-list="targetList"
            display-key="name"
            setting-key="fid"
            searchable
            show-overflow-tips
            @change="changeSelect">
            <template #source-option="{ name, type }">
                <div class="flex-align-center flex-1">
                    <Icon size="16" :name="type.toLowerCase()" />
                    <span class="ml10 flex-1 text-overflow" :title="name">{{ name }}</span>
                    <i class="bk-icon icon-arrows-right"></i>
                </div>
            </template>
            <template #target-option="{ name, type }">
                <div class="flex-align-center flex-1">
                    <Icon size="16" :name="type.toLowerCase()" />
                    <span class="ml10 flex-1 text-overflow" :title="name">{{ name }}</span>
                    <i class="bk-icon icon-close"></i>
                </div>
            </template>
        </bk-transfer>
    </canway-dialog>
</template>
<script>
    import { mapState } from 'vuex'
    export default {
        name: 'repoDialog',
        props: {
            show: Boolean,
            replicaTaskObjects: Array,
            insertFilterRepoList: {
                type: Array,
                default: () => []
            }
        },
        data () {
            return {
                checkedRepository: []
            }
        },
        computed: {
            ...mapState(['repoReadListAll']),
            repoList () {
                return this.repoReadListAll
                    .filter(r => {
                        return (this.insertFilterRepoList.length
                            ? this.insertFilterRepoList
                            : ['DOCKER', 'MAVEN', 'NPM', 'GENERIC', 'GO'])
                            .includes(r.type)
                            && r.name !== 'pipeline'
                            && r.name !== 'report'
                            && r.category !== 'REMOTE'
                            && r.category !== 'VIRTUAL'
                    })
                    .map(repo => ({ ...repo, fid: repo.projectId + repo.name }))
                    .sort((a, b) => {
                        return Boolean(a.type > b.type) || -1
                    })
            },
            targetList () {
                return this.replicaTaskObjects.map(v => v.fid)
            }
        },
        methods: {
            changeSelect (sourceList, targetList) {
                this.checkedRepository = targetList
            },
            async confirmPackageData () {
                this.$emit('confirm', this.checkedRepository)
                this.$emit('cancel')
            }
        }
    }
</script>
