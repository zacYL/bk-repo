import Vue from 'vue'

export default {
    state: {},
    getters: {},
    mutations: {},
    actions: {
        // 分页查询仓库列表
        // override
        getRepoList (_, { projectId, current, limit, name, type }) {
            return Vue.prototype.$ajax.get(
                `repository/api/repo/software/page/${current}/${limit}`,
                {
                    params: {
                        projectId: projectId || undefined,
                        name: name || undefined,
                        type: type || undefined
                    }
                }
            )
        }
    }
}
