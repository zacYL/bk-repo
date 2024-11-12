<template>
    <div style="display: flex;">
        <bk-popover ref="recyclingPopover"
            :tippy-options="{
                theme: 'light',
                trigger: 'click',
                hideOnClick: false
            }"
            ext-cls="recycling-popover"
            v-bk-clickoutside="clickoutside">
            <bk-button v-bind="filterParams ? { theme: 'primary',outline: true } : { theme: 'default' }">
                <div
                    class="flex-align-center"
                >
                    <span>{{ $t('filter') }}</span>
                </div>
            </bk-button>
            <div slot="content">
                <main>
                    <bk-form :model="query" :label-width="110" form-type="vertical">
                        <!-- 启动状态 -->
                        <bk-form-item :label="$t('enabledStatus')" property="enable">
                            <bk-select
                                v-model="query.enable"
                                :placeholder="$t('selectEnableStatus')">
                                <bk-option v-for="type in enableEnum" :key="type.value" :id="type.value" :name="type.label">
                                    <div class="flex-align-center">
                                        <span class="ml10 flex-1 text-overflow">{{type.label}}</span>
                                    </div>
                                </bk-option>
                            </bk-select>
                        </bk-form-item>
                        <!-- 制品类型 -->
                        <bk-form-item :label="$t('repoType')" :property="'repoType'">
                            <bk-select
                                v-model="query.repoType"
                                :placeholder="$t('allTypes')">
                                <bk-option v-for="type in repoEnum" :key="type.value" :id="type.value" :name="type.label">
                                    <div class="flex-align-center">
                                        <Icon size="20" :name="type.value" />
                                        <span class="ml10 flex-1 text-overflow">{{type.label}}</span>
                                    </div>
                                </bk-option>
                            </bk-select>
                        </bk-form-item>
                        <!-- 制品名称 -->
                        <bk-form-item :label="$t('artifactName')" :property="'name'">
                            <bk-input
                                v-model="query.name"
                                :placeholder="$t('artifactNamePlaceholder')"
                                max-length="100"
                                clearable
                            />
                        </bk-form-item>
                        <!-- 仓库名称 -->
                        <bk-form-item :label="$t('repoName')" :property="'repoName'">
                            <bk-input
                                v-model="query.repoName"
                                :placeholder="$t('repoNameEnterPlaceholder')"
                                max-length="100"
                                clearable
                            />
                        </bk-form-item>
                        <!-- 版本号 -->
                        <bk-form-item :label="$t('searchConditionVersion')" :property="'version'">
                            <bk-input
                                v-model="query.version"
                                :placeholder="$t('enterVersionNumber')"
                                max-length="100"
                                clearable
                            />
                        </bk-form-item>
                    </bk-form>
                </main>
                <footer class="flex-align-center"
                    style="justify-content: flex-end;">
                    <bk-button @click="reset" class="mr10">
                        {{$t('reset')}}
                    </bk-button>
                    <bk-button theme="primary" @click="confirm">
                        {{$t('search')}}
                    </bk-button>
                </footer>
            </div>
        </bk-popover>
    </div>
</template>
<script>

    import { repoEnum } from '@repository/store/publicEnum'
    export default {
        name: 'filter-condition',
        props: {
            filterParams: {
                type: [Array, null],
                default: null
            }
        },
        data () {
            return {
                repoEnum, // 制品类型
                enableEnum: [{
                    value: true,
                    label: this.$t('enable')
                }, {
                    value: false,
                    label: this.$t('disable')
                }],
                query: {
                    repoType: 'maven',
                    enable: '',
                    repoName: '', // 仓库名称
                    name: '', // 制品名称
                    version: ''
                }
            }
        },
        methods: {
            // 处理popover点击外层是否关闭的逻辑
            clickoutside (e) {
                const tippy = document.querySelectorAll('.tippy-popper')
                const allTippy = [...tippy]
                let isInTippy = false
                allTippy.forEach(item => {
                    if (item.contains(e.target)) {
                        isInTippy = true
                    }
                })
                if (!isInTippy) {
                    this.$refs.recyclingPopover.hideHandler()
                }
            },

            // 重置
            clear () {
                Object.assign(this.query, {
                    repoType: 'maven',
                    enable: '',
                    repoName: '', // 仓库名称
                    name: '', // 制品名称
                    version: ''
                })
            },
            reset () {
                this.clear()
                this.$emit('reset', () => {
                    this.close()
                })
            },
            close () {
                this.$refs.recyclingPopover.hideHandler()
            },
            confirm () {
                const filterList = []

                // 辅助函数，用于添加过滤条件
                const addFilter = (field, value, operation) => {
                    filterList.push({ field, value, operation })
                }

                // 启用状态
                addFilter('startType', this.query.enable !== '' ? this.query.enable : null, this.query.enable !== '' ? 'EQ' : 'NOT_NULL')

                // 其他固定过滤条件
                addFilter('repoType', this.query.repoType, 'EQ')
                this.query.version && addFilter('version', this.query.version, 'MATCH_I')
                this.query.repoName && addFilter('repoName', this.query.repoName, 'MATCH_I')
                this.query.name && addFilter('name', this.query.name, 'MATCH_I')
                this.$emit('confirm', filterList, () => {
                    this.close()
                })
            }
        }
    }
</script>
<style lang="scss">
.recycling-popover {
    .tippy-tooltip {
        padding: 0;
    }
}
.select-popover-custom{
    width: 150px !important;
}
</style>
<style lang="scss" scoped>
main {
    width: 400px;
    padding: 20px 25px;
}
footer {
    border-top: 1px solid #ECF2FB;
    height: 42px;
    padding: 0 20px;
}
</style>
