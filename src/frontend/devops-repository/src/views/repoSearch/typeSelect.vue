<template>
    <div class="repo-search-container">
        <bk-form :label-width="100" form-type="inline" :model="repoSearchConditionInfo" ref="repoSearchConditionInfo">
            <bk-form-item :label="$t('repoType')">
                <!-- 制品类型下拉筛选项 -->
                <div class="type-select-container flex-align-center"
                    :class="{ 'active': showDropdown }"
                    @click="showDropdown = !showDropdown"
                    v-bk-clickoutside="hiddenDropdown">
                    <div class="flex-align-center type-select-checked">
                        <Icon size="22" :name="repoType" />
                        <span class="ml5">{{repoType}}</span>
                    </div>
                    <i class="ml5 devops-icon" :class="showDropdown ? 'icon-angle-up' : 'icon-angle-down'"></i>
                    <div v-show="showDropdown" class="dropdown-list" @click.stop="() => {}">
                        <bk-radio-group :value="repoType" class="repo-type-radio-group" @change="changeType">
                            <bk-radio-button v-for="repo in repoList" :key="repo" :value="repo">
                                <div class="flex-align-center repo-type-radio" :class="{ 'checked': repo === repoType }">
                                    <Icon size="22" :name="repo" />
                                    <span class="ml10">{{repo}}</span>
                                </div>
                            </bk-radio-button>
                        </bk-radio-group>
                    </div>
                </div>
            </bk-form-item>
            <bk-form-item v-if="!whetherSoftware" :label="$t('searchRepoArtifact')">
                <!-- 仓库名称选择框，多选 -->
                <bk-select
                    class="w250 search-artifact-select"
                    searchable
                    multiple
                    display-tag
                    :show-select-all="false"
                    v-model="checkedArtifactList"
                    :placeholder="$t('allStore')">
                    <bk-option-group
                        v-for="(artifact, index) in artifactList"
                        :name="artifact.name"
                        :key="index"
                        :show-collapse="true"
                        :is-collapse.sync="artifact.isCollapse">
                        <bk-option v-for="option in artifact.children"
                            :key="option.name"
                            :id="option.name"
                            :name="option.name">
                        </bk-option>
                    </bk-option-group>
                </bk-select>
            </bk-form-item>
            <!-- 软件源模式下因为后端接口效率及结果不准确等问题，需要更换为项目筛选 projectId -->
            <bk-form-item v-if="whetherSoftware" :label="$t('project')">
                <!-- 项目选择框，多选 -->
                <bk-select
                    class="w250 search-artifact-select"
                    searchable
                    multiple
                    display-tag
                    :show-select-all="false"
                    v-model="checkedProjectList"
                    :placeholder="$t('total') + $t('space') + $t('project')">
                    <bk-option
                        v-for="project in projectList"
                        :name="project.name"
                        :id="project.id"
                        :key="project.id">
                    </bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item :label="$t('searchName')">
                <bk-input
                    class="w250 input-common"
                    clearable
                    v-model="repoSearchConditionInfo.name"
                    :placeholder="$t('searchNamePlaceholder')"
                    @clear="onSearchArtifact"
                    @enter="onSearchArtifact">
                </bk-input>
            </bk-form-item>
            <bk-form-item v-if="repoSearchConditionInfo.hasOwnProperty('version')" :label="$t('searchConditionVersion')">
                <bk-input
                    class="w250 input-common condition-item-common"
                    v-model="repoSearchConditionInfo.version"
                    :placeholder="$t('pleaseInput') + $t('space') + $t('searchConditionVersion')"
                    @enter="onSearchArtifact">
                </bk-input>
                <span class="search-close-icon" @click="onClearCondition('version')">
                    <Icon size="8" name="close" />
                </span>
            </bk-form-item>
            <bk-form-item v-if="repoSearchConditionInfo.hasOwnProperty('checkSum')" :label="$t('searchConditionCheckSum')">
                <bk-input
                    class="w250 input-common condition-item-common"
                    v-model="repoSearchConditionInfo.checkSum"
                    :placeholder="$t('searchConditionChecksumPlaceholder')"
                    @blur="onVerifyCheckSum"
                    @enter="onSearchArtifact">
                </bk-input>
                <span class="search-close-icon" @click="onClearCondition('checkSum')">
                    <Icon size="8" name="close" />
                </span>
            </bk-form-item>
            <template v-if="repoSearchConditionInfo.hasOwnProperty('metadata') && repoSearchConditionInfo.metadata.length > 0">
                <bk-form-item v-for="(item,index) in repoSearchConditionInfo.metadata" :label="$t('metadata') + ' ' + (index + 1)" :key="index">
                    <div style="display:flex;position:relative;">
                        <bk-input
                            class="w125 input-common "
                            v-model="item.key"
                            :maxlength="30"
                            :placeholder="$t('key')">
                        </bk-input>
                        <bk-input
                            class="w125 input-common"
                            v-model="item.value"
                            :maxlength="500"
                            :placeholder="$t('value')"
                            @enter="onSearchArtifact">
                        </bk-input>
                    </div>
                    <span class="search-close-icon" @click="onClearCondition('metadata',index)">
                        <Icon size="8" name="close" />
                    </span>
                </bk-form-item>
            </template>
        </bk-form>
        <div class="search-operation">
            <Icon name="repoHelp" size="16" v-bk-tooltips=" { content: $t('searchConditionInfo') }" />
            <bk-button class="ml10" @click="onResetSearchArtifact">{{$t('reset')}}</bk-button>
            <bk-button class="ml10" theme="primary" @click="onSearchArtifact">{{$t('search')}}</bk-button>
            <bk-dropdown-menu trigger="click" :disabled="allConditionWhetherDisabled">
                <div class="dropdown-trigger-text" slot="dropdown-trigger">
                    <bk-button :disabled="allConditionWhetherDisabled" class="ml10" theme="primary" icon="plus" icon-right="icon-angle-down">{{$t('searchCondition')}}</bk-button>
                </div>
                <!-- 使用bk-link替代之前的 a 标签可以设置某个操作的禁用 -->
                <ul class="bk-dropdown-list" slot="dropdown-content">
                    <li v-for="item in conditionMap" :key="item.id">
                        <bk-link theme="default" href="javascript:;"
                            :disabled="conditionWhetherDisabled(item)"
                            @click="onClickSearchOperation(item.id)">
                            <span> {{$t(item.name)}} </span>
                        </bk-link>
                    </li>
                </ul>
            </bk-dropdown-menu>
        </div>
    </div>
</template>
<script>
    import { repoSearchConditionMap, repoEnum } from '@repository/store/publicEnum'
    // 自定义Error，防止直接使用 new Error 导致把文件源码暴露
    function CustomError (message) {
        this.name = 'CustomError'
        this.message = message || 'Default error message'
        this.stack = (new Error()).stack
    }
    CustomError.prototype = Object.create(Error.prototype)
    CustomError.prototype.constructor = CustomError
    export default {
        name: 'typeSelect',
        props: {
            // 制品类型集合
            repoList: {
                type: Array,
                default: () => []
            },
            repoType: {
                type: String,
                default: 'generic'
            },
            // 仓库名集合
            artifactOriginalList: {
                type: Array,
                default: () => []
            },
            // 搜索方式下拉框中可选择项
            conditionList: {
                type: Array,
                default () {
                    return repoSearchConditionMap || []
                }
            },
            projectList: {
                type: Array,
                default: () => []
            }
        },
        data () {
            return {
                showDropdown: false,
                repoEnum,
                // 搜索条件所在表单
                repoSearchConditionInfo: {
                    name: this.$route.query.name || ''
                },
                // 当前类型的仓库列表，不分页
                artifactList: [
                    {
                        id: 'local',
                        name: this.$t('localStore'),
                        // 该分组是否收起
                        isCollapse: false,
                        children: []
                    },
                    {
                        id: 'remote',
                        name: this.$t('remoteStore'),
                        isCollapse: true,
                        children: []
                    },
                    {
                        id: 'composite',
                        name: this.$t('compositeStore'),
                        isCollapse: true,
                        children: []
                    }
                ],
                checkedArtifactList: [],
                checkedProjectList: []
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            // generic仓库不能使用包版本搜素
            conditionMap () {
                return this.repoType === 'generic'
                    ? this.conditionList.filter(item => item.id !== 'version')
                    : this.conditionList
            },
            // 添加搜索条件按钮是否整体被禁用
            allConditionWhetherDisabled () {
                const conditionIdMap = this.conditionMap.map(item => item.id)
                const checkSumFlag = conditionIdMap.includes('checkSum') ? 'checkSum' in this.repoSearchConditionInfo : true
                const versionFlag = conditionIdMap.includes('version') ? 'version' in this.repoSearchConditionInfo : true
                return checkSumFlag && versionFlag && this.repoSearchConditionInfo?.metadata?.length === 4
            },
            // 是否是 软件源模式
            whetherSoftware () {
                return this.$route.path.startsWith('/software')
            }
        },
        watch: {
            artifactOriginalList: {
                deep: true,
                handler () {
                    this.constructRepoArtifactList()
                }
            }
        },
        created () {
            // 回显checkSum，因为可能之前没有添加这个搜索条件，所以需要先判断在route.query中是否存在checkSum相关的
            if (!this.whetherSoftware && (this.$route.query.sha256 || this.$route.query.md5)) {
                this.repoSearchConditionInfo.checkSum = this.$route.query.sha256 || this.$route.query.md5
            }
            // 回显 版本号
            if (this.$route.query.version) {
                this.repoSearchConditionInfo.version = this.$route.query.version
            }
            // 回显当前选择的仓库
            this.checkedArtifactList = Object.keys(this.$route.query)
                .filter(key => key.startsWith('artifactProperties'))
                .map(key => this.$route.query[key])
            
            // 软件源模式下回显当前选择的项目
            if (this.whetherSoftware) {
                this.checkedProjectList = Object.keys(this.$route.query)
                    .filter(key => key.startsWith('projectProperties'))
                    .map(key => this.$route.query[key])
            }
            
            // 回显元数据
            const metadataKeys = Object.keys(this.$route.query)
                .filter(key => key.startsWith('metadataProperties['))
            if (metadataKeys?.length) {
                const metadataList = metadataKeys
                    .map(key => {
                        const matchKey = key.match(/metadataProperties\[(.+?)\]/)
                        return {
                            key: matchKey ? matchKey[1] : '',
                            value: this.$route.query[key]
                        }
                    })
                this.$set(this.repoSearchConditionInfo, 'metadata', [...metadataList])
            }
            this.$nextTick(() => {
                // 此时需要再次触发一下搜索，初始化时子组件直接触发父组件的搜索
                this.onSearchArtifact()
            })
        },
        methods: {
            onClickSearchOperation (type) {
                if (type === 'metadata' && !('metadata' in this.repoSearchConditionInfo)) {
                    // 第一次点击添加元数据
                    this.$set(this.repoSearchConditionInfo, type, [])
                } else if (type !== 'metadata') {
                    this.$set(this.repoSearchConditionInfo, type, '')
                }
                if (type === 'metadata' && this.repoSearchConditionInfo.metadata?.length < 4) {
                    // 不是第一次且不是第五次点击添加元数据
                    this.onCheckMetadataKeyInput().then(() => {
                        this.repoSearchConditionInfo.metadata.push({
                            key: '',
                            value: ''
                        })
                    })
                }
            },
            // 判断某些搜索条件是否禁用
            conditionWhetherDisabled (item) {
                return (item.id === 'checkSum' && 'checkSum' in this.repoSearchConditionInfo)
                    || (item.id === 'version' && 'version' in this.repoSearchConditionInfo)
                    || (item.id === 'metadata' && (this.repoSearchConditionInfo?.metadata?.length || 0) >= 4)
            },
            // 构造当前制品类型的全部仓库列表，并根据远程本地区分
            constructRepoArtifactList () {
                const localArtifactList = []
                const remoteArtifactList = []
                const compositeArtifactList = []
                this.artifactOriginalList.forEach((item) => {
                    switch (item.category) {
                        case 'LOCAL':
                            localArtifactList.push(item)
                            break
                        case 'REMOTE':
                            remoteArtifactList.push(item)
                            break
                        case 'COMPOSITE':
                            compositeArtifactList.push(item)
                            break
                            // 搜索不支持虚拟仓库，虚拟仓库本身不存储任何制品
                    }
                })
                this.artifactList = this.artifactList.map((item) => {
                    switch (item.id) {
                        case 'local':
                            item.children = localArtifactList
                            break
                        case 'remote':
                            item.children = remoteArtifactList
                            break
                        default:
                            item.children = compositeArtifactList
                            break
                    }
                    return item
                })
            },
            // 隐藏制品类型的下拉选择框
            hiddenDropdown () {
                this.showDropdown = false
            },
            // 改变制品类型
            changeType (type) {
                this.$emit('change', type)
                this.hiddenDropdown()
                this.onResetSearchArtifact()
            },
            // 校验元数据的输入是否符合规则
            onCheckMetadataKeyInput () {
                // 没有添加元数据时直接返回成功
                if (!('metadata' in this.repoSearchConditionInfo)) {
                    return Promise.resolve()
                }
                const keySet = {}
                for (const item of this.repoSearchConditionInfo.metadata) {
                    const { key, value } = item
                    if (!key && !value) {
                        continue // 如果 key 和 value 都为空，则继续下一个元素的检查
                    }
                    if (!key) {
                        this.$bkMessage({
                            theme: 'warning',
                            limit: 3,
                            message: this.$t('searchMetadataKeyEmptyTips')
                        })
                        return Promise.reject(new CustomError(this.$t('searchMetadataKeyEmptyTips')))
                    }
                    if (!value) {
                        this.$bkMessage({
                            theme: 'warning',
                            limit: 3,
                            message: this.$t('searchMetadataValueEmptyTips')
                        })
                        return Promise.reject(new CustomError(this.$t('searchMetadataValueEmptyTips')))
                    }
                    if (keySet[key]) {
                        this.$bkMessage({
                            theme: 'warning',
                            limit: 3,
                            message: this.$t('searchMetadataRepeatKeyTips')
                        })
                        return Promise.reject(new CustomError(this.$t('searchMetadataRepeatKeyTips')))
                    }
                    keySet[key] = key
                }
                // 校验通过
                return Promise.resolve()
            },
            // checksum 输入框的校验事件
            onVerifyCheckSum () {
                if (!this.whetherSoftware && this.repoSearchConditionInfo.checkSum && this.repoSearchConditionInfo.checkSum?.length !== 64 && this.repoSearchConditionInfo.checkSum?.length !== 32) {
                    this.$bkMessage({
                        theme: 'warning',
                        limit: 3,
                        message: this.$t('searchConditionChecksumErrorTips')
                    })
                    return Promise.reject(new CustomError(this.$t('searchConditionChecksumErrorTips')))
                }
                return Promise.resolve()
            },
            // 组装数据
            constructionSearchData () {
                const { name, checkSum, version, metadata } = this.repoSearchConditionInfo
                const backData = { name }
                // 如果不存在 checkSum 相关的参数时子组件传参中不能携带 checkSum 这个参数
                if (!this.whetherSoftware && checkSum) {
                    backData[checkSum.length === 32 ? 'md5' : 'sha256'] = checkSum
                }
                // 如果不存在version的参数时子组件传参中不能携带version这个参数
                if (version) {
                    backData.version = version
                }
                if (metadata) {
                    backData.metadataList = metadata
                }
                return backData
            },
            // 清除搜索条件
            onClearCondition (type, index) {
                if (type === 'metadata') {
                    this.repoSearchConditionInfo.metadata.splice(index, 1)
                } else {
                    this.$delete(this.repoSearchConditionInfo, type)
                }
            },
            // 搜索操作
            onSearchArtifact () {
                // 此时需要校验下用户
                Promise.all([this.onVerifyCheckSum(), this.onCheckMetadataKeyInput()]).then(() => {
                    const params = this.constructionSearchData()
                    // 非软件源才能出现仓库下拉选择框
                    if (!this.whetherSoftware && this.checkedArtifactList?.length) {
                        params.artifactList = this.checkedArtifactList
                    }
                    // 软件源模式下才存在项目下拉选择框
                    if (this.whetherSoftware && this.checkedProjectList?.length) {
                        params.projectList = this.checkedProjectList
                    }
                    this.$emit('search-artifact', params)
                })
            },
            // 重置搜索条件
            onResetSearchArtifact () {
                this.repoSearchConditionInfo = {
                    name: ''
                }
                this.checkedArtifactList = []
                this.checkedProjectList = []
                this.onSearchArtifact()
            }
        }
    }
</script>
<style lang="scss" scoped>
.repo-search-container{
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    width: 100%;
    padding: 0 0 10px 0;
    border-bottom: 1px solid var(--borderColor);
    .search-operation {
        display: flex;
        align-items: center;
        height: 32px;
    }
    .type-select-container {
        position: relative;
        width: 250px;
        height: 32px;
        margin-right: -1px;
        border-radius: 2px 0 0 2px;
        border: 1px solid var(--borderWeightColor);
        cursor: pointer;
        &.active {
            color: var(--primaryColor);
            border-color: var(--primaryColor);
            z-index: 1;
        }
        .icon-angle-up,
        .icon-angle-down {
            font-size: 12px;
            font-weight: bold;
            color: var(--fontSubsidiaryColor);
            transform: scale(0.8)
        }
        .type-select-checked{
            width:224px;
            padding:0 30px 0 10px;
        }
        .dropdown-list {
            position: absolute;
            top: calc(100% + 10px);
            left: 0;
            width: 250px;
            max-height: 216px;
            overflow-y: auto;
            padding: 10px;
            background-color: white;
            box-shadow: 0px 0px 6px 0px rgba(167, 167, 167, 0.5);
            z-index: 1;
            cursor: default;
            .repo-type-radio-group {
                display: grid;
                grid-template: auto / repeat(1, 50px);
                gap: 10px;
                justify-items: start;
                ::v-deep .bk-form-radio-button {
                    .bk-radio-button-text {
                        height: 100%;
                        line-height: initial;
                        padding: 0;
                        border: none;
                    }
                }
                .repo-type-radio {
                    position: relative;
                    padding: 0 10px;
                    width: 220px;
                    height: 30px;
                    &.checked {
                        height: 30px;
                        background-color: var(--bgHoverLighterColor);
                        color: var(--primaryColor) ;
                    }
                }
            }
        }
    }
    .name-search {
        width: 50px;
        height: 32px;
        margin-left: -1px;
        color: white;
        font-size: 16px;
        font-weight: bold;
        background-color: var(--primaryColor);
        border-radius: 0 2px 2px 0;
        cursor: pointer;
    }
}
 .sort-order {
    width: 30px;
    height: 30px;
    border: 1px solid var(--borderWeightColor);
    border-radius: 2px;
    cursor: pointer;
    &:hover {
        color: var(--primaryColor);
        border-color: currentColor;
        background-color: var(--bgHoverLighterColor);
    }
}
.search-close-icon{
    position: absolute;
    top: -6px;
    left: 242px;
    display: none;
    cursor: pointer;
    background: var(--boxShadowColor);
    height: 14px;
    border-radius: 50%;
    width: 14px;
}
.input-common{
    height: 32px;
}
.condition-item-common{
    position: relative;
}
// 解决bk-button中右侧的图标上方有间距导致右侧图标下沉的问题
::v-deep button.bk-button .bk-icon{
    top: 0;
}
// 不这样会因为换行等字符导致出现幽灵空白节点，导致元素错位
::v-deep .bk-form.bk-inline-form{
    display: flex;
    flex-flow: wrap;
}
::v-deep .bk-form.bk-inline-form .bk-form-item .bk-label{
    width: 94px !important;
}
::v-deep .bk-form.bk-inline-form .bk-form-item:first-child{
    margin: 10px 0 0 8px;
}
::v-deep .bk-form.bk-inline-form .bk-form-item+.bk-form-item {
    margin-top: 10px;
}
.w125 {
    width: 125px !important;
}
::v-deep .bk-form.bk-inline-form .bk-form-item:hover .search-close-icon{
    display: flex;
    align-items: center;
    justify-content: center;
}
// 只取消第二个input输入框的左边框
::v-deep .bk-form-control + .bk-form-control .bk-form-input {
    border-left: none;
}
</style>
