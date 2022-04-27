const repoHome = () => import('@/views')

const repoList = () => import('@/views/repoList')
const repoConfig = () => import('@/views/repoConfig')
const repoToken = () => import('@repository/views/repoToken')
const repoAudit = () => import('@repository/views/repoAudit')
const nodeManage = () => import('@repository/views/nodeManage')
const planManage = () => import('@repository/views/planManage')
const createPlan = () => import('@repository/views/planManage/createPlan')
const logDetail = () => import('@repository/views/planManage/logDetail')

const repoGeneric = () => import('@/views/repoGeneric')

const commonPackageList = () => import('@repository/views/repoCommon/commonPackageList')
const commonPackageDetail = () => import('@/views/repoCommon/commonPackageDetail')

const repoSearch = () => import('@repository/views/repoSearch')

const repoScan = () => import('@repository/views/repoScan')
const scanReport = () => import('@repository/views/repoScan/scanReport')
const artiReport = () => import('@repository/views/repoScan/artiReport')
const scanConfig = () => import('@repository/views/repoScan/scanConfig')
const startScan = () => import('@repository/views/repoScan/startScan')

const routes = [
    {
        path: '/ui/:projectId',
        component: repoHome,
        redirect: { name: 'repoList' },
        children: [
            {
                path: 'repoList',
                name: 'repoList',
                component: repoList,
                meta: {
                    breadcrumb: [
                        { name: 'repoList', label: '仓库列表' }
                    ]
                }
            },
            {
                path: 'repoConfig/:repoType',
                name: 'repoConfig',
                component: repoConfig,
                meta: {
                    breadcrumb: [
                        { name: 'repoList', label: '仓库列表' },
                        { name: 'repoConfig', label: '仓库配置' }
                    ]
                }
            },
            {
                path: 'repoSearch',
                name: 'repoSearch',
                component: repoSearch,
                meta: {
                    breadcrumb: [
                        { name: 'repoSearch', label: '制品搜索' }
                    ]
                }
            },
            {
                path: 'repoToken',
                name: 'repoToken',
                component: repoToken,
                meta: {
                    breadcrumb: [
                        { name: 'repoToken', label: '访问令牌' }
                    ]
                }
            },
            {
                path: 'repoAudit',
                name: 'repoAudit',
                component: repoAudit,
                meta: {
                    breadcrumb: [
                        { name: 'repoAudit', label: '审计日志' }
                    ]
                }
            },
            {
                path: 'nodeManage',
                name: 'nodeManage',
                component: nodeManage,
                meta: {
                    breadcrumb: [
                        { name: 'nodeManage', label: '节点管理' }
                    ]
                }
            },
            {
                path: 'planManage',
                name: 'planManage',
                component: planManage,
                meta: {
                    breadcrumb: [
                        { name: 'planManage', label: '分发计划' }
                    ]
                }
            },
            {
                path: 'planManage/createPlan',
                name: 'createPlan',
                component: createPlan,
                meta: {
                    breadcrumb: [
                        { name: 'planManage', label: '分发计划' },
                        { name: 'createPlan', label: '创建计划' }
                    ]
                }
            },
            {
                path: 'planManage/editPlan/:planId',
                name: 'editPlan',
                component: createPlan,
                meta: {
                    breadcrumb: [
                        { name: 'planManage', label: '{planName}', template: '分发计划' },
                        { name: 'createPlan', label: '编辑计划' }
                    ]
                }
            },
            {
                path: 'planManage/planDetail/:planId',
                name: 'planDetail',
                component: createPlan,
                meta: {
                    breadcrumb: [
                        { name: 'planManage', label: '{planName}', template: '分发计划' },
                        { name: 'createPlan', label: '计划详情' }
                    ]
                }
            },
            {
                path: 'planManage/logDetail/:logId',
                name: 'logDetail',
                component: logDetail,
                meta: {
                    breadcrumb: [
                        { name: 'planManage', label: '{planName}', template: '分发计划' },
                        { name: 'logDetail', label: '日志详情' }
                    ]
                }
            },
            {
                path: 'repoScan',
                name: 'repoScan',
                component: repoScan,
                meta: {
                    breadcrumb: [
                        { name: 'repoScan', label: '制品扫描' }
                    ]
                }
            },
            {
                path: 'scanReport/:planId',
                name: 'scanReport',
                component: scanReport,
                meta: {
                    breadcrumb: [
                        { name: 'repoScan', label: '制品扫描' },
                        { name: 'scanReport', label: '{scanName}', template: '扫描报告' }
                    ]
                }
            },
            {
                path: 'artiReport/:planId/:recordId',
                name: 'artiReport',
                component: artiReport,
                beforeEnter: (to, from, next) => {
                    const repoType = to.query.repoType
                    if (to.query.scanName) {
                        to.meta.breadcrumb = [
                            { name: 'repoScan', label: '制品扫描' },
                            { name: 'scanReport', label: '{scanName}', template: '扫描报告' },
                            { name: 'artiReport', label: '{artiName}', template: '制品扫描结果' }
                        ]
                    } else if (repoType === 'generic') {
                        to.meta.breadcrumb = [
                            { name: 'repoList', label: '仓库列表' },
                            { name: 'commonList', label: '{repoName}', template: '依赖仓库' },
                            { name: 'commonPackage', label: '{packageKey}', template: '制品详情' },
                            { name: 'artiReport', label: '制品扫描结果' }
                        ]
                    } else if (repoType) {
                        to.meta.breadcrumb = [
                            { name: 'repoList', label: '仓库列表' },
                            { name: 'repoGeneric', label: '{repoName}', template: '二进制仓库' },
                            { name: 'artiReport', label: '制品扫描结果' }
                        ]
                    }
                    next()
                }
            },
            {
                path: 'scanConfig/:planId',
                name: 'scanConfig',
                component: scanConfig,
                meta: {
                    breadcrumb: [
                        { name: 'repoScan', label: '制品扫描' },
                        { name: 'scanConfig', label: '{scanName}', template: '方案设置' }
                    ]
                }
            },
            {
                path: 'startScan/:planId',
                name: 'startScan',
                component: startScan,
                meta: {
                    breadcrumb: [
                        { name: 'repoScan', label: '制品扫描' },
                        { name: 'scanReport', label: '{scanName}', template: '扫描报告' },
                        { name: 'startScan', label: '立即扫描' }
                    ]
                }
            },
            {
                path: 'generic',
                name: 'repoGeneric',
                component: repoGeneric,
                meta: {
                    breadcrumb: [
                        { name: 'repoList', label: '仓库列表' },
                        { name: 'repoGeneric', label: '{repoName}', template: '二进制仓库' }
                    ]
                }
            },
            {
                path: ':repoType/list',
                name: 'commonList',
                component: commonPackageList,
                meta: {
                    breadcrumb: [
                        { name: 'repoList', label: '仓库列表' },
                        { name: 'commonList', label: '{repoName}', template: '依赖仓库' }
                    ]
                }
            },
            {
                path: ':repoType/package',
                name: 'commonPackage',
                component: commonPackageDetail,
                meta: {
                    breadcrumb: [
                        { name: 'repoList', label: '仓库列表' },
                        { name: 'commonList', label: '{repoName}', template: '依赖仓库' },
                        { name: 'commonPackage', label: '{packageKey}', template: '制品详情' }
                    ]
                }
            }
        ]
    }
]

export default routes
