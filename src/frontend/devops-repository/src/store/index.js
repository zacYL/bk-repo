import Vue from 'vue'
import Vuex from 'vuex'
import actions from './actions'

Vue.use(Vuex)

export default new Vuex.Store({
    state: {
        showLoginDialog: false,
        breadcrumb: [],
        genericTree: [
            {
                name: '/',
                fullPath: '',
                folder: true,
                children: [],
                roadMap: '0'
            }
        ],
        projectList: [],
        repoListAll: [],
        userList: {
            anonymous: {
                id: 'anonymous',
                name: '--'
            }
        },
        userInfo: {
            username: '',
            name: '',
            email: '',
            phone: '',
            admin: true
        },
        domain: {
            docker: '',
            npm: ''
        },
        clusterList: [{
            'id': '610b53fcfdae6c4586fbdaa6',
            'name': '分节点',
            'status': 'UNHEALTHY',
            'errorReason': null,
            'type': 'STANDALONE',
            'url': 'http://backup.bkrepo.canway.com/',
            'username': 'admin',
            'password': 'bkrepo',
            'certificate': null,
            'createdBy': 'admin',
            'createdDate': '2021-08-05T10:59:08.148',
            'lastModifiedBy': 'system',
            'lastModifiedDate': '2021-08-26T22:58:26.399'
        }, {
            'id': '6103b87bee93c8311740fabe',
            'name': 'bkup',
            'status': 'UNHEALTHY',
            'errorReason': null,
            'type': 'STANDALONE',
            'url': 'http://backup.bkrepo.canway.com/',
            'username': 'admin',
            'password': 'bkrepo',
            'certificate': null,
            'createdBy': 'admin',
            'createdDate': '2021-07-30T16:29:47.065',
            'lastModifiedBy': 'system',
            'lastModifiedDate': '2021-08-26T22:58:26.769'
        }, {
            'id': '60fe77e70c00c57e0ccdf59f',
            'name': 'center',
            'status': 'HEALTHY',
            'errorReason': null,
            'type': 'CENTER',
            'url': 'http://independent.bkrepo.canway.com/',
            'username': 'admin',
            'password': 'bkrepo',
            'certificate': '',
            'createdBy': 'system',
            'createdDate': '2021-07-26T16:52:52.577',
            'lastModifiedBy': 'system',
            'lastModifiedDate': '2021-08-05T22:32:44.062'
        }]
    },
    getters: {
        masterNode (state) {
            return state.clusterList.find(v => v.type === 'CENTER') || { name: '', url: '' }
        }
    },
    mutations: {
        INIT_TREE (state) {
            state.genericTree = [
                {
                    name: '/',
                    fullPath: '',
                    folder: true,
                    children: [],
                    roadMap: '0'
                }
            ]
        },
        UPDATE_TREE (state, { roadMap, list }) {
            let tree = state.genericTree
            roadMap.split(',').forEach(index => {
                if (!tree[index].children) Vue.set(tree[index], 'children', [])
                tree = tree[index].children
            })
            list = list.map(item => {
                const children = (tree.find(oldItem => oldItem.fullPath === item.fullPath) || {}).children || []
                return {
                    ...item,
                    children,
                    name: (item.metadata && item.metadata.displayName) || item.name
                }
            })
            tree.splice(0, tree.length, ...list)
        },
        SET_BREADCRUMB (state, data) {
            state.breadcrumb = data
        },
        SET_USER_LIST (state, data) {
            state.userList = {
                ...data,
                anonymous: {
                    id: 'anonymous',
                    name: '--'
                }
            }
        },
        SET_USER_INFO (state, data) {
            state.userInfo = {
                ...state.userInfo,
                ...data
            }
        },
        SET_DOMAIN (state, { type, domain }) {
            state.domain = {
                ...state.domain,
                [type]: domain
            }
        },
        SET_CLUSTER_LIST (state, data) {
            state.clusterList = data
        },
        SET_PROJECT_LIST (state, data) {
            state.projectList = data
        },
        SET_REPO_LIST_ALL (state, data) {
            state.repoListAll = data
        },
        SHOW_LOGIN_DIALOG (state, show = true) {
            state.showLoginDialog = show
        }
    },
    actions
})
