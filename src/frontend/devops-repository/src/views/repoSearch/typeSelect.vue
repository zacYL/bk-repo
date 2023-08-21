<template>
    <div class="repo-search-container">
        <!-- 制品类型下拉筛选项 -->
        <div class="type-select-container flex-center"
            :class="{ 'active': showDropdown }"
            @click="showDropdown = !showDropdown"
            v-bk-clickoutside="hiddenDropdown">
            <div class="flex-align-center">
                <Icon size="20" :name="repoType" />
                <span class="ml5">{{repoType}}</span>
            </div>
            <i class="ml5 devops-icon" :class="showDropdown ? 'icon-angle-up' : 'icon-angle-down'"></i>
            <div v-show="showDropdown" class="dropdown-list" @click.stop="() => {}">
                <bk-radio-group :value="repoType" class="repo-type-radio-group" @change="changeType">
                    <bk-radio-button v-for="repo in repoList" :key="repo" :value="repo">
                        <div class="flex-align-center repo-type-radio" :class="{ 'checked': repo === repoType }">
                            <Icon size="20" :name="repo" />
                            <span class="ml10">{{repo}}</span>
                        </div>
                    </bk-radio-button>
                </bk-radio-group>
            </div>
        </div>
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
        <bk-input
            class="w480"
            v-model="searchParams"
            size="large"
            :placeholder="$t('pleaseInput') + $t('space') + $t('packageName')">
        </bk-input>
        <bk-popover
            ref="popoverSearchRef"
            placement="bottom-start"
            theme="light"
            :max-width="580"
            :width="580"
            :arrow="false"
            :distance="0"
            offset="[-480, 0]"
            :tippy-options="{ hideOnClick: false,trigger: 'click' }"
            :on-show="onShowPopoverSearch">
            <Icon size="20" :name="showPopover ? 'search-drop-down' : 'search-drop-up'" class="popover-search-icon" @click.native.stop="onClickPopoverIcon" />
            <template #content>
                <div class="search-filter-container">
                    <bk-form ref="searchFilterFormRef" :label-width="110" :model="searchFilterForm" class="search-filter-form">
                        <!-- 搜索方式下拉框 -->
                        <bk-form-item :label="$t('repoSearchMannerInfo')">
                            <div class="flex-between-center">
                                <bk-select
                                    v-model="searchManner"
                                    :clearable="false"
                                    class="w250"
                                    :placeholder="$t('pleaseSelect') + $t('space') + $t('repoSearchMannerInfo')"
                                    @change="onChangeSearchManner">
                                    <bk-option v-for="manner in mannerMap" :key="manner.id" :id="manner.id" :name="$t(manner.name)">
                                    </bk-option>
                                </bk-select>
                                <template v-if="searchManner === 'metadata'">
                                    <div class="flex-center" style="margin-right:40px;">
                                        <Icon name="repoHelp" size="16" v-bk-tooltips=" { content: $t('searchMetadataInfo') }" />
                                        <bk-button class="ml10" icon="plus" theme="primary" @click="onCheckAndAddMetadata">{{ $t('add') }}</bk-button>
                                    </div>
                                </template>
                            </div>
                        </bk-form-item>
                        <!-- 文件名称 -->
                        <template v-if="searchManner === 'fileName'">
                            <bk-form-item :label="$t('searchMannerFileName')">
                                <bk-input
                                    class="w410"
                                    v-model.lazy="searchFileName"
                                    size="large"
                                    :placeholder="$t('searchMannerFileNamePlaceholder')">
                                </bk-input>
                            </bk-form-item>
                        </template>
                        <!-- checkSum -->
                        <template v-if="searchManner === 'checkSum'">
                            <bk-form-item :label="$t('searchMannerCheckSum')">
                                <bk-input
                                    class="w410"
                                    v-model.trim="searchCheckSum"
                                    size="large"
                                    :placeholder="repoType === 'docker' ? $t('searchMannerSHA256Placeholder') : $t('searchMannerChecksumPlaceholder')"
                                    @blur="onVerifyCheckSum">
                                </bk-input>
                            </bk-form-item>
                        </template>
                        <!-- 包版本 -->
                        <template v-if="searchManner === 'packageVersion'">
                            <bk-form-item :label="$t('packageName')">
                                <bk-input
                                    class="w410"
                                    v-model.trim="searchPackageName"
                                    size="large"
                                    :placeholder="$t('pleaseInput') + $t('space') + $t('packageName')">
                                </bk-input>
                            </bk-form-item>
                            <bk-form-item :label="$t('version')">
                                <bk-input
                                    class="w410"
                                    v-model.trim="searchPackageVersion"
                                    size="large"
                                    :disabled="!searchPackageName.length"
                                    :placeholder="$t('pleaseInput') + $t('space') + $t('version')">
                                </bk-input>
                            </bk-form-item>
                        </template>
                        <!-- 元数据 -->
                        <template v-if="searchManner === 'metadata'">
                            <div v-for="(metadata, index) in searchMetadataList" :key="index">
                                <div class="search-metadata-item" :style="{ 'margin-left': (index === 0 && searchMetadataList.length === 1) ? '-40px' : '-18px' }">
                                    <bk-form-item :label="$t('key')">
                                        <bk-input
                                            class="w170"
                                            v-model.trim="metadata.key"
                                            size="large"
                                            :placeholder="$t('pleaseInput') + $t('space') + $t('key')">
                                        </bk-input>
                                    </bk-form-item>
                                    <bk-form-item :label="$t('value')" :label-width="70" class="mt0">
                                        <bk-input
                                            class="w170"
                                            v-model.trim="metadata.value"
                                            size="large"
                                            :placeholder="$t('pleaseInput') + $t('space') + $t('value')">
                                        </bk-input>
                                    </bk-form-item>
                                    <Icon
                                        v-if="searchMetadataList.length > 1"
                                        class="ml10"
                                        name="icon-delete"
                                        size="12"
                                        @click.native.stop="deleteMetadataSearch(index)" />
                                </div>
                            </div>
                        </template>
                        <bk-form-item class="flex-end-center popover-search-footer">
                            <bk-button @click="onHidePopoverSearch">{{$t('cancel')}}</bk-button>
                            <bk-button class="ml10" theme="primary" @click="onSearchArtifact">{{$t('search')}}</bk-button>
                        </bk-form-item>
                    </bk-form>
                </div>
            </template>
        </bk-popover>
        <i class="name-search devops-icon icon-search flex-center" @click.stop="onSearchArtifact"></i>
    </div>
</template>
<script>
    import { mapActions } from 'vuex'
    import { repoSearchMannerMap, repoEnum } from '@repository/store/publicEnum'
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
            repoList: {
                type: Array,
                default: () => []
            },
            repoType: {
                type: String,
                default: 'generic'
            }
        },
        data () {
            return {
                showDropdown: false,
                repoEnum,
                repoSearchMannerMap,
                searchParams: '',
                searchManner: '',
                searchFileName: '',
                searchCheckSum: '',
                searchPackageName: '',
                searchPackageVersion: '',
                searchMetadataList: [],
                showPopover: false,
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
                checkedArtifactList: []
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            // generic仓库不能使用包版本搜素，依赖源仓库不能使用文件名称搜索
            mannerMap () {
                return this.repoType === 'generic'
                    ? this.repoSearchMannerMap.filter(item => item.id !== 'packageVersion')
                    : this.repoSearchMannerMap.filter(item => item.id !== 'fileName')
            },
            // 搜索方式支持选择的id，用于下方判断
            mannerIdMap () {
                return this.mannerMap.map(item => item.id)
            },
            // 根据下拉框中输入框同步修改非元数据的值回显
            composedSearchParamsWithoutMetadata () {
                const mannerData = `${this.searchManner ? 'manner:' + this.searchManner + ';' : ''}`
                const fileNameData = `${this.searchFileName ? 'fileName:' + this.searchFileName : ''}`
                const checkSumData = `${this.searchCheckSum ? 'checkSum:' + this.searchCheckSum : ''}`
                const packageNameData = `${this.searchPackageName ? 'packageName:' + this.searchPackageName + ';' : ''}`
                const packageVersionData = `${this.searchPackageVersion ? 'packageVersion:' + this.searchPackageVersion : ''}`
                return mannerData + fileNameData + checkSumData + packageNameData + packageVersionData
            },
            // 根据下方输入框同步修改元数据值的回显
            composedMetadataSearchParams () {
                const mannerData = this.searchManner ? 'manner:' + this.searchManner + ';' : ''
                const metadataData = this.searchMetadataList
                    .filter(item => item.key && item.value)
                    .map(item => `${item.key}=${item.value}`)
                    .join(',')
                const metadataPrefix = this.searchMetadataList.length > 0 ? 'metadata:' : ''
                return mannerData + metadataPrefix + metadataData
            }
        },
        watch: {
            // 监视 searchParams 值的改变，同步修改下方下拉框中数据值
            searchParams () {
                // 只有搜索的下拉打开的时候才更新下拉框中的数据
                if (this.showPopover) {
                    this.accordingParamsSetData()
                }
            },
            // 当搜索方式被改变时，同步修改上方回显值
            searchManner (value) {
                // 当用户手动删除了 searchManner时，需要将输入框清空，并关闭popover
                if (!value && this.showPopover) {
                    this.closePopoverSearch()
                }
                this.updateSearchParams()
            },
            // 当文件名称被改变时，同步修改上方回显值
            searchFileName () {
                this.updateSearchParams()
            },
            searchCheckSum () {
                this.updateSearchParams()
            },
            searchPackageName () {
                this.updateSearchParams()
            },
            searchPackageVersion () {
                this.updateSearchParams()
            }
        },
        created () {
            this.commonInitData()
            this.getRepoArtifactList()
        },
        methods: {
            ...mapActions(['getRepoListAll']),
            // 获取当前类型的全部仓库列表，不分页
            getRepoArtifactList () {
                this.getRepoListAll({ projectId: this.projectId, type: this.repoType, searchFlag: true }).then((res) => {
                    const localArtifactList = []
                    const remoteArtifactList = []
                    const compositeArtifactList = []
                    res.forEach((item) => {
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
                })
            },
            // 更新上方输入框
            updateSearchParams () {
                const hasMetadata = this.searchMetadataList.some(item => item.key && item.value)
                if (hasMetadata) {
                    this.searchParams = this.composedMetadataSearchParams
                } else {
                    this.searchParams = this.composedSearchParamsWithoutMetadata
                }
            },
            // 更新非元数据的输入框数据，根据上方输入框输入的值，双向绑定
            extractValue (paramsArray, key) {
                const param = paramsArray.find(param => param.startsWith(`${key}:`))
                return param ? param.split(`${key}:`)[1] : ''
            },
            // 更新元数据输入框数据，根据上方输入框输入的值，双向绑定
            extractMetadata (paramsArray) {
                const metadataParams = paramsArray.filter(param => param.startsWith('metadata:'))
                if (metadataParams.length === 0) {
                    return [{ key: '', value: '' }]
                } else {
                    const metadataValue = metadataParams[0].split('metadata:')[1]
                    return metadataValue.split(',').map(param => {
                        const [key, value] = param.split('=')
                        return { key, value }
                    })
                }
            },
            // 根据上方输入框中的数据(searchParams)同步下方搜索方式及输入框数据
            accordingParamsSetData () {
                const paramsArray = this.searchParams.split(';')
                // 此时需要防止用户在popover关闭的时候手动修改 manner的值，导致再次展开popover时 赋值给 searchManner时是数组中不存在的值
                const mannerParam = this.extractValue(paramsArray, 'manner')
                this.searchManner = this.mannerIdMap.includes(mannerParam) ? mannerParam : ''
                this.searchFileName = this.extractValue(paramsArray, 'fileName')
                this.searchCheckSum = this.extractValue(paramsArray, 'checkSum')
                this.searchPackageName = this.extractValue(paramsArray, 'packageName')
                this.searchPackageVersion = this.extractValue(paramsArray, 'packageVersion')
                this.searchMetadataList = this.extractMetadata(paramsArray)
            },
            // 公共代码，用于还原输入框的数据及初始化数据
            commonInitData () {
                this.searchFileName = ''
                this.searchCheckSum = ''
                this.searchPackageName = ''
                this.searchPackageVersion = ''
                this.searchMetadataList = [
                    {
                        key: '',
                        value: ''
                    }
                ]
            },
            // 抽离公共代码，用于初始化搜索方式
            commonInitManner () {
                this.searchManner = this.repoType === 'generic' ? 'fileName' : 'packageVersion'
            },
            // 隐藏制品类型的下拉选择框
            hiddenDropdown () {
                this.showDropdown = false
            },
            // 改变制品类型
            changeType (type) {
                this.$emit('change', type)
                this.hiddenDropdown()
                this.onHidePopoverSearch()
                this.$nextTick(() => {
                    this.getRepoArtifactList()
                })
            },
            // popover 显示触发事件
            onShowPopoverSearch () {
                // 如果用户在关闭popover之后手动修改了searchParams的值，此时在重新打开时需要将下方的搜索方式及输入框重新赋值
                this.accordingParamsSetData()
                !this.searchManner && this.commonInitManner()
                this.showPopover = true
            },
            // 点击取消按钮隐藏popover的搜索
            onHidePopoverSearch () {
                this.commonInitData()
                this.searchManner = ''
                this.closePopoverSearch()
            },
            // 手动调用组件关闭popover
            closePopoverSearch () {
                this.$refs.popoverSearchRef.hideHandler()
                this.showPopover = false
            },
            // 点击icon可以打开下拉框，当下拉框已经被打开的情况下再次点击可以收起下拉框
            onClickPopoverIcon () {
                this.showPopover ? this.closePopoverSearch() : this.$refs.popoverSearchRef.showHandler()
            },
            // 改变了搜索方式，此时需要将各个输入框数据重置
            onChangeSearchManner () {
                this.commonInitData()
            },
            // 增加一条元数据记录
            addMetadataSearch () {
                this.searchMetadataList.push({
                    key: '',
                    value: ''
                })
            },
            // 删除元数据列表中的某一条记录
            deleteMetadataSearch (index) {
                this.searchMetadataList.splice(index, 1)
            },
            onCheckMetadataKeyInput () {
                // 当下拉框没有展开，但是搜索方式是元数据，此时需要看用户输入是否是以metadata:开头的，不是的话，直接清空searchParams并返回成功
                if (!this.showPopover) {
                    const paramArr = this.searchParams.split(';')
                    const metadataList = paramArr.filter(param => param.startsWith('metadata:'))
                    if (metadataList.length === 0) {
                        this.searchParams = ''
                        return Promise.resolve()
                    }
                }
                // 获取所有 key 值
                const keys = this.searchMetadataList.map(obj => {
                    // key不允许为空
                    if (!obj.key) {
                        this.$bkMessage({
                            theme: 'warning',
                            limit: 3,
                            message: this.$t('searchMetadataKeyEmptyTips')
                        })
                        throw new CustomError(this.$t('searchMetadataKeyEmptyTips'))
                    } else {
                        return obj.key
                    }
                })
                this.searchMetadataList.map(obj => {
                    // value不允许为空
                    if (!obj.value) {
                        this.$bkMessage({
                            theme: 'warning',
                            limit: 3,
                            message: this.$t('searchMetadataValueEmptyTips')
                        })
                        throw new CustomError(this.$t('searchMetadataValueEmptyTips'))
                    } else {
                        return obj.value
                    }
                })
                // 检查 key 值是否重复
                const isKeyDuplicate = keys.some((key, index) => keys.indexOf(key) !== index)
                if (isKeyDuplicate) {
                    this.$bkMessage({
                        theme: 'warning',
                        limit: 3,
                        message: this.$t('searchMetadataRepeatKeyTips')
                    })
                    return Promise.reject(new CustomError(this.$t('searchMetadataRepeatKeyTips')))
                }
                return Promise.resolve()
            },
            // 先校验元数据的输入是否符合规则，在符合规范之后添加一条新的元数据记录
            onCheckAndAddMetadata () {
                this.onCheckMetadataKeyInput().then(() => {
                    // 此时表明key及value都不为空，且key不重复
                    this.updateSearchParams()
                    // 此时不能继续直接添加一条记录，会导致不生效
                    this.$nextTick(() => {
                        this.addMetadataSearch()
                    })
                })
            },
            // checksum 输入框的校验事件
            onVerifyCheckSum () {
                // docker 只有sha256的搜索，即只能输入64位
                if (this.repoType === 'docker') {
                    if (this.searchCheckSum && this.searchCheckSum.length !== 64) {
                        this.$bkMessage({
                            theme: 'warning',
                            limit: 3,
                            message: this.$t('searchMannerSHA256ErrorTips')
                        })
                        return Promise.reject(new CustomError(this.$t('searchMannerSHA256ErrorTips')))
                    }
                } else {
                    if (this.searchCheckSum && (this.searchCheckSum.length !== 64 && this.searchCheckSum.length !== 32)) {
                        this.$bkMessage({
                            theme: 'warning',
                            limit: 3,
                            message: this.$t('searchMannerChecksumErrorTips')
                        })
                        return Promise.reject(new CustomError(this.$t('searchMannerChecksumErrorTips')))
                    }
                }
                return Promise.resolve()
            },
            // 组装数据
            constructionSearchData () {
                const backData = {
                    manner: this.searchManner
                }
                switch (this.searchManner) {
                    case 'fileName':
                        backData.name = this.searchFileName
                        break
                    case 'checkSum':
                        backData[this.searchCheckSum.length === 32 ? 'md5' : 'sha256'] = this.searchCheckSum
                        break
                    case 'packageVersion':
                        backData.name = this.searchPackageName
                        if (this.searchPackageVersion) {
                            backData.version = this.searchPackageVersion
                        }
                        break
                    case 'metadata':
                        backData.metadataList = this.searchMetadataList.map(item => {
                            // 展开下拉框选择了元数据之后，直接关闭下拉框，然后在上面输入框中输入数据，此时直接触发元数据的校验不合适，
                            // 但是不做下面的判断会导致接口传参添加了field: 'metadata.' 的对象，会导致搜索结果为空
                            if (item.key && item.value) {
                                return {
                                    field: 'metadata.' + item.key,
                                    value: item.value,
                                    operation: 'EQ'
                                }
                            } else {
                                return ''
                            }
                        }).filter(Boolean)
                        break
                    default:
                        backData.name = this.searchParams
                }
                return backData
            },
            // 校验输入数据是否合法
            onCheckParamsValidate () {
                // 当用户直接在上方的输入框中输入数据时需要先对输入框数据拆解赋值给下拉框中的输入框，然后再进行校验数据是否合法
                !this.showPopover && this.accordingParamsSetData()
                let promise
                if (this.searchManner === 'checkSum') {
                    promise = this.onVerifyCheckSum()
                } else if (this.searchManner === 'metadata') {
                    promise = this.onCheckMetadataKeyInput().then(() => {
                        this.updateSearchParams()
                    })
                } else {
                    //  其余的 searchManner 值
                    promise = Promise.resolve()
                }
                return promise
            },
            // 搜索操作
            onSearchArtifact () {
                // 此时需要校验下用户
                this.onCheckParamsValidate().then(() => {
                    // 展开下拉框输入数据后，关闭下拉框，然后修改上方输入框的值(不改变前面的manner:xxxx，直接删除后面的参数设置或修改metadata:之类的，导致参数不能配置)，
                    // 此时搜索不带条件(是对的)，但是输入框依旧保留了搜索方式的回显，不太合适，此时相当于是没有添加任何搜索条件的，需要重置searchParams为空字符串
                    const isEmptySearch = this.mannerIdMap.includes(this.searchManner)
                        && this.searchFileName === ''
                        && this.searchCheckSum === ''
                        && this.searchMetadataList[0].key === ''
                        && this.searchPackageName === ''
                        && this.searchPackageVersion === ''
                    if (isEmptySearch) {
                        this.searchParams = ''
                    }
                    const params = this.constructionSearchData()
                    params.artifactList = this.checkedArtifactList
                    this.$emit('search-artifact', params)
                    this.showPopover && this.closePopoverSearch()
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.repo-search-container{
    display: flex;
    .type-select-container {
        position: relative;
        width: 120px;
        height: 48px;
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
        .dropdown-list {
            position: absolute;
            top: calc(100% + 10px);
            left: 0;
            width: 120px;
            max-height: 216px;
            overflow-y: auto;
            padding: 5px 10px;
            background-color: white;
            box-shadow: 0px 0px 6px 0px rgba(167, 167, 167, 0.5);
            z-index: 1;
            cursor: default;
            .repo-type-radio-group {
                display: grid;
                grid-template: auto / repeat(1, 50px);
                gap: 6px;
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
                    padding: 0 0 0 5px;
                    width: 100px;
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
        height: 48px;
        margin-left: -1px;
        color: white;
        font-size: 16px;
        font-weight: bold;
        background-color: var(--primaryColor);
        border-radius: 0 2px 2px 0;
        cursor: pointer;
    }
   
    ::v-deep .bk-input-large {
        height: 48px;
        line-height: 48px;
    }
    ::v-deep .bk-tooltip-ref{
        height: 48px;
        border-top: 1px solid #cbd5e0;
        border-bottom: 1px solid #cbd5e0;
        display: flex;
        align-items: center;
        justify-content: center;
    }
}
.popover-search-icon{
    width: 50px;
    cursor: pointer;
}
.search-filter-container{
    width: 580px;
    max-height: 370px;
    overflow-y: auto;
    // bk-popover 有一个 上 10px 左14px的偏移
    margin: 0 0 0 -14px;
}
.search-filter-form{
    width: calc(100% - 20px);
    margin: 10px;
}
.search-metadata-item{
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 20px 0 0 -14px;
}
.w170{
    width: 170px;
}
.w410{
    width: 410px;
}
.popover-search-footer{
    margin: 20px 40px 0 0;
}
.search-artifact-select{
    border: none;
    border-left:  1px solid var(--borderWeightColor);
}
::v-deep .bk-select {
    line-height: 48px;
}
::v-deep .bk-select .bk-select-angle {
    top: 0;
    height: 48px;
    line-height: 48px;
}

</style>
