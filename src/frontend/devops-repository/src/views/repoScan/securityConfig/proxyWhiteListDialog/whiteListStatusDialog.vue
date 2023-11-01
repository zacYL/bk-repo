<template>
    <canway-dialog
        :value="show"
        :title="$t('proxyWhiteOpenStatus')"
        :height-num="400"
        :width="520"
        @cancel="$emit('close')"
    >
        <bk-table
            height="242"
            :data="tableData"
            :outer-border="false"
            :row-border="false">
            <bk-table-column :label="$t('repoType')" prop="repositoryType"></bk-table-column>
            <bk-table-column :label="$t('proxyWhiteOpenStatus')" prop="status">
                <template slot-scope="props">
                    <div>
                        <bk-switcher
                            :pre-check="changeSwitcher"
                            :true-value="props.row.repositoryType + ',true'"
                            :false-value="props.row.repositoryType + ',false'"
                            size="small"
                            v-model="props.row.status"
                            theme="primary">
                        </bk-switcher>
                    </div>
                </template>
            </bk-table-column>
        </bk-table>
        <template #footer>
            <bk-button theme="default" @click="$emit('close')">{{$t('close')}}</bk-button>
        </template>
    </canway-dialog>
</template>

<script>
    import { mapActions } from 'vuex'
    export default {
        props: {
            show: {
                type: Boolean,
                required: true
            }
        },
        data () {
            return {
                tableData: []
            }
        },
        computed: {
        },
        created () {
            this.init()
        },
        methods: {
            ...mapActions(['getWhiteListSwitchList', 'updateWhiteListSwitchList']),
            init () {
                this.getWhiteListSwitchList().then(res => {
                    const data = res || {}
                    const list = []
                    Object.keys(data).forEach(key => {
                        list.push({
                            repositoryType: key,
                            status: key + ',' + data[key]
                        })
                    })
                    this.tableData = list
                })
            },
            changeSwitcher (value) {
                const RepositoryType = value.split(',')[0]
                const originStatus = value.split(',')[1] === 'false'
                return new Promise((resolve, reject) => {
                    this.updateWhiteListSwitchList({ RepositoryType }).then(res => {
                        res === !originStatus ? resolve() : reject(new Error(this.$t('switchWhiteStatusFail')))
                    })
                })
            }
        }
    }
</script>
