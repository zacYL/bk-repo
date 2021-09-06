<template>
    <div class="package-card-container flex-align-center">
        <div class="mr20 card-icon flex-center">
            <Icon size="48" :name="cardData.type.toLowerCase()" />
        </div>
        <div class="mr20 package-card-main flex-column">
            <div class="flex-align-center">
                <span class="card-name text-overflow" :title="cardData.name">{{ cardData.name }}</span>
                <span class="ml10 card-tag" v-if="cardData.type === 'MAVEN'">{{ cardData.key.replace(/^.*\/\/(.+):.*$/, '$1') }}</span>
            </div>
            <span class="package-card-description text-overflow" :title="cardData.description">{{ cardData.description }}</span>
            <div class="package-card-data flex-align-center">
                <div class="flex-align-center" :title="`最新版本：${cardData.latest}`"></div>
                <div class="flex-align-center" :title="`最后修改：${formatDate(cardData.lastModifiedDate)}`"></div>
                <div class="flex-align-center" :title="`下载统计：${cardData.downloads}`"></div>
            </div>
        </div>
        <div class="card-operation flex-center">
            <i v-if="!readonly" class="devops-icon icon-delete flex-center package-card-delete" @click.stop="deleteCard"></i>
        </div>
    </div>
</template>
<script>
    import { formatDate } from '@/utils'
    import { mapState } from 'vuex'
    export default {
        name: 'packageCard',
        props: {
            cardData: {
                type: Object,
                default: {}
            },
            readonly: {
                type: Boolean,
                default: false
            }
        },
        computed: {
            ...mapState(['userList'])
        },
        methods: {
            formatDate,
            deleteCard () {
                this.$emit('delete-card')
            }
        }
    }
</script>
<style lang="scss" scoped>
.package-card-container {
    height: 100px;
    padding: 16px 20px;
    border-radius: 5px;
    background-color: var(--bgHoverColor);
    cursor: pointer;
    &:hover {
        background-color: white;
        box-shadow: 0px 0px 8px 0px rgba(217, 217, 217, 0.5);
    }
    .card-icon {
        width: 68px;
        height: 68px;
        background-color: white;
        border: 1px solid var(--borderColor);
        border-radius: 4px;
    }
    .package-card-main {
        flex: 1;
        height: 100%;
        justify-content: space-around;
        overflow: hidden;
        .card-name {
            max-width: 500px;
            color: #222222;
            font-weight: bold;
        }
        .card-tag {
            display: inline-block;
            padding: 0 10px;
            line-height: 22px;
            font-size: 12px;
            white-space: nowrap;
            border-radius: 2px;
            color: var(--borderColor);
            background-color: #91ADD1;
        }
        .package-card-description {
            color: #999;
            font-size: 12px;
        }
        .package-card-data {
            color: var(--fontWeightColor);
            font-size: 12px;
            div {
                width: 300px;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                &:after {
                    content: attr(title);
                }
            }
        }
    }
    .card-operation {
        flex-basis: 50px;
        .package-card-delete {
            width: 24px;
            height: 24px;
            &:hover {
                color: var(--borderColor);
                background-color: var(--dangerColor);
                border-radius: 4px;
            }
        }
    }
}
</style>
