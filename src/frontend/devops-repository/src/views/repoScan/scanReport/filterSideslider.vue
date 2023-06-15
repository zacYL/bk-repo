<template>
    <bk-sideslider
        :is-show.sync="showSideslider"
        title="筛选"
        @click.native.stop="() => {}"
        :quick-close="true">
        <template #content>
            <div class="sideslider-content flex-column">
                <bk-form class="p20 flex-1" form-type="vertical">
                    <bk-form-item label="制品名称">
                        <bk-input v-model="filter.name"></bk-input>
                    </bk-form-item>
                    <bk-form-item label="所属仓库">
                        <bk-select
                            v-model="filter.repoName"
                            searchable>
                            <bk-option-group
                                v-for="(list, type) in repoGroupList"
                                :name="type.toLowerCase()"
                                :key="type"
                                show-collapse>
                                <bk-option v-for="option in list"
                                    :key="option.name"
                                    :id="option.name"
                                    :name="option.name">
                                </bk-option>
                            </bk-option-group>
                        </bk-select>
                    </bk-form-item>
                    <bk-form-item v-if="!scanType.includes('LICENSE')" label="风险等级">
                        <bk-select
                            v-model="filter.highestLeakLevel">
                            <bk-option v-for="[id, name] in Object.entries(leakLevelEnum)" :key="id" :id="id" :name="name"></bk-option>
                        </bk-select>
                    </bk-form-item>
                    <bk-form-item label="扫描状态">
                        <bk-select
                            v-model="filter.status">
                            <bk-option v-for="[id, name] in Object.entries(scanStatusEnum)" :key="id" :id="id" :name="name"></bk-option>
                        </bk-select>
                    </bk-form-item>
                </bk-form>
                <div class="pr30 sideslider-footer flex-end-center">
                    <bk-button class="mr10" theme="default" @click="reset">重置</bk-button>
                    <bk-button theme="primary" @click="filterHandler">筛选</bk-button>
                </div>
            </div>
        </template>
    </bk-sideslider>
</template>
<script>
    import { mapState, mapActions } from 'vuex'
    import { scanStatusEnum, leakLevelEnum } from '@repository/store/publicEnum'
    export default {
        props: {
            scanType: String
        },
        data () {
            return {
                showSideslider: false,
                scanStatusEnum,
                leakLevelEnum,
                filter: {
                    name: '',
                    repoName: '',
                    highestLeakLevel: '',
                    status: ''
                }
            }
        },
        computed: {
            ...mapState(['repoListAll']),
            repoGroupList () {
                const repoTypeLimit = [this.scanType.replace(/^([A-Z]+).*$/, '$1')]
                return this.repoListAll
                    .filter(r => repoTypeLimit.includes(r.type) && r.category !== 'VIRTUAL')
                    .reduce((target, repo) => {
                        if (!target[repo.type]) target[repo.type] = []
                        target[repo.type].push(repo)
                        return target
                    }, {})
            }
        },
        created () {
            // 需要根据VueRouter中的参数设置筛选框的默认值
            this.filter = {
                name: this.$route.query?.name || '',
                repoName: this.$route.query?.repoName || '',
                highestLeakLevel: this.$route.query?.highestLeakLevel || '',
                status: this.$route.query?.status || ''
            }
            // 设置了默认值后需要将参数同步父组件，否则会导致面包屑回退后筛选参数会是之前设置的值
            // 初始化设置筛选状态时需要告知父组件，让父组件保留扫描记录列表的页码及每页大小等参数
            this.filterHandler('initFlag')
            this.getRepoListAll({ projectId: this.$route.params.projectId })
        },
        methods: {
            ...mapActions([
                'getRepoListAll'
            ]),
            show () {
                this.showSideslider = true
            },
            filterHandler (flag) {
                this.showSideslider = false
                const filter = Object.keys(this.filter).reduce((target, key) => {
                    this.filter[key].toString() && (target[key] = this.filter[key])
                    return target
                }, {})
                const backFilter = {
                    ...filter,
                    flag
                }
                this.$emit('filter', backFilter)
            },
            reset () {
                this.filter = {
                    name: '',
                    repoName: '',
                    highestLeakLevel: '',
                    status: ''
                }
                this.showSideslider = false
                // 此时只能向父组件返回一个空对象，不能将上面的属性值都为空的对象返回，会导致关闭弹窗后请求携带了这些空值的参数，导致返回数据为空数组
                this.$emit('filter', {})
            }
        }
    }
</script>
<style lang="scss" scoped>
.sideslider-content {
    height: 100%;
    .sideslider-footer {
        height: 60px;
        background-color: var(--bgColor);
        border-top: 1px solid var(--borderColor);
    }
}
</style>
