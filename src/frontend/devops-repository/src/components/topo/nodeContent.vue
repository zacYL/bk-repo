<template>
    <div class="node-content-container">
        <div class="flex-align-center">
            <span v-if="node.leftTag" class="mr10 repo-tag" :class="node.leftTag.type">{{ node.leftTag.content }}</span>
            <a v-if="node.url" class="title text-overflow" :href="node.url" target="_blank">{{ node.title }}</a>
            <div v-else class="title text-overflow" :title="node.title">{{ node.title }}</div>
        </div>
        <template v-if="node.metadata && node.metadata.length">
            <div class="metadata text-overflow" v-for="item in node.metadata" :key="item" :title="item">{{ item }}</div>
        </template>
    </div>
</template>
<script>
    export default {
        name: 'nodeContent',
        props: {
            node: Object
        }
    }
</script>
<style lang="scss" scoped>
.node-content-container {
    flex: 0;
    max-width: 300px;
    padding: 15px;
    margin: 20px 0;
    border-radius: 2px;
    box-shadow: 0px 0px 8px 0px rgba(58, 132, 255, 0.15);
    user-select: none;
    .title {
        font-size: 14px;
        max-width: 250px;
    }
    .metadata {
        margin-top: 10px;
        color: var(--fontSubsidiaryColor);
        max-width: 250px;
    }
}
</style>
