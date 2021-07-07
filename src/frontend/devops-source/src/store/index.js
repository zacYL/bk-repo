import Vue from 'vue'
import Vuex from 'vuex'
import actions from './actions'

Vue.use(Vuex)

export default new Vuex.Store({
    state: {
        userList: {},
        repoList: [],
        userInfo: {
            username: '',
            admin: false
        },
        dockerDomain: ''
    },
    getters: {
    },
    mutations: {
        SET_USER_LIST (state, data) {
            state.userList = data
        },
        SET_USER_INFO (state, data) {
            state.userInfo = {
                ...state.userInfo,
                ...data
            }
        },
        SET_DOCKER_DOMAIN (state, data) {
            state.dockerDomain = data
        },
        SET_REPO_LIST (state, data) {
            state.repoList = data
        }
    },
    actions
})
