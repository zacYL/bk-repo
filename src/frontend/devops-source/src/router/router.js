const sourceHome = () => import(/* webpackChunkName: "sourceHome" */'@/views')

const sourceOverview = () => import(/* webpackChunkName: "sourceOverview" */'@/views/overview')

const sourceManage = () => import(/* webpackChunkName: "sourceManage" */'@/views/manage')
const repoList = () => import(/* webpackChunkName: "sourceManage" */'@/views/manage/repoList')
const createRepo = () => import(/* webpackChunkName: "sourceManage" */'@/views/manage/createRepo')
const repoConfig = () => import(/* webpackChunkName: "sourceManage" */'@/views/manage/repoConfig')

const sourceSearch = () => import(/* webpackChunkName: "sourceSearch" */'@/views/search')
const searchOverview = () => import(/* webpackChunkName: "sourceSearch" */'@/views/search/overview')
const searchRepoList = () => import(/* webpackChunkName: "sourceSearch" */'@/views/search/repoList')
const searchPackageList = () => import(/* webpackChunkName: "sourceSearch" */'@/views/search/packageList')
const searchPackageDetail = () => import(/* webpackChunkName: "sourceSearch" */'@/views/search/packageDetail')

const sourceAudit = () => import(/* webpackChunkName: "sourceAudit" */'@/views/audit')

const sourceAdmin = () => import(/* webpackChunkName: "sourceAdmin" */'@/views/admin')

const routes = [
    {
        path: '/bksoftware',
        component: sourceHome,
        redirect: {
            name: 'searchOverview'
        },
        children: [
            {
                path: 'overview',
                name: 'overview',
                component: sourceOverview,
                meta: {
                    title: '概览'
                }
            },
            {
                path: 'manage',
                name: 'manage',
                component: sourceManage,
                meta: {
                    title: '仓库管理'
                },
                redirect: {
                    name: 'repoList'
                },
                children: [
                    {
                        path: 'list',
                        name: 'repoList',
                        component: repoList,
                        meta: {
                            title: '仓库列表'
                        }
                    },
                    {
                        path: 'create',
                        name: 'createRepo',
                        component: createRepo,
                        meta: {
                            title: '添加仓库'
                        }
                    },
                    {
                        path: 'config/:repoType/:repoName',
                        name: 'repoConfig',
                        component: repoConfig,
                        meta: {
                            title: '仓库配置'
                        }
                    }
                ]
            },
            {
                path: 'search',
                name: 'search',
                component: sourceSearch,
                meta: {
                    title: '搜索制品'
                },
                redirect: {
                    name: 'searchOverview'
                },
                children: [
                    {
                        path: 'overview',
                        name: 'searchOverview',
                        component: searchOverview,
                        meta: {
                            title: '搜索主页'
                        }
                    },
                    {
                        path: 'repoList',
                        name: 'searchRepoList',
                        component: searchRepoList,
                        meta: {
                            title: '仓库列表'
                        }
                    },
                    {
                        path: 'packageList/:repoType/:repoName',
                        name: 'searchPackageList',
                        component: searchPackageList,
                        meta: {
                            title: '包列表'
                        }
                    },
                    {
                        path: 'packageDetail/:repoType/:repoName',
                        name: 'searchPackageDetail',
                        component: searchPackageDetail,
                        meta: {
                            title: '包详情'
                        }
                    }
                ]
            },
            {
                path: 'audit',
                name: 'audit',
                component: sourceAudit,
                meta: {
                    title: '审计日志'
                }
            },
            {
                path: 'admin',
                name: 'admin',
                component: sourceAdmin,
                meta: {
                    title: '管理员'
                }
            }
        ]
    }
]

export default routes
