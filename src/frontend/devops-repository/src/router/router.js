const repoPreview = () => import('@repository/views/preview')
const scanTask = () => import('@repository/views/preview/scanTask')

const repoHome = () => import('@repository/views')
const repoList = () => import('@repository/views/repoList')
const repoConfig = () => import('@repository/views/repoConfig')
const repoToken = () => import('@repository/views/repoToken')
const userCenter = () => import('@repository/views/userCenter')
const userManage = () => import('@repository/views/userManage')
const repoAudit = () => import('@repository/views/repoAudit')
const projectManage = () => import('@repository/views/projectManage')
const projectConfig = () => import('@repository/views/projectManage/projectConfig')
const nodeManage = () => import('@repository/views/nodeManage')
const planManage = () => import('@repository/views/planManage')
const logDetail = () => import('@repository/views/planManage/logDetail')
const repoScan = () => import('@repository/views/repoScan')
const scanReport = () => import('@repository/views/repoScan/scanReport')
const artiReport = () => import('@repository/views/repoScan/artiReport')
const scanConfig = () => import('@repository/views/repoScan/scanConfig')
const startScan = () => import('@repository/views/repoScan/startScan')
const securityConfig = () => import('@repository/views/repoScan/securityConfig')
const repoGeneric = () => import('@repository/views/repoGeneric')
const commonPackageList = () => import('@repository/views/repoCommon/commonPackageList')
const commonPackageDetail = () => import('@repository/views/repoCommon/commonPackageDetail')
const repoSearch = () => import('@repository/views/repoSearch')
const repoCatalog = () => import('@repository/views/repoCatalog')

// 网络设置
const networkConfig = () => import('@repository/views/networkConfig')

const routes = [
    {
        path: '/ui/:projectId/preview',
        component: repoPreview,
        children: [
            {
                path: 'scanTask/:planId/:taskId',
                component: scanTask
            }
        ]
    },
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
                        { name: 'repoList', label: 'repoList' }
                    ]
                }
            },
            {
                path: 'repoConfig/:repoType',
                name: 'repoConfig',
                component: repoConfig,
                meta: {
                    breadcrumb: [
                        { name: 'repoList', label: 'repoList' },
                        { name: 'repoConfig', label: 'repoConfig' }
                    ]
                }
            },
            {
                path: 'repoCatalog',
                name: 'repoCatalog',
                component: repoCatalog,
                meta: {
                    breadcrumb: [
                        { name: 'repoCatalog', label: 'repoCatalog' }
                    ]
                }
            },
            {
                path: 'repoSearch',
                name: 'repoSearch',
                component: repoSearch,
                meta: {
                    breadcrumb: [
                        { name: 'repoSearch', label: 'repoSearch' }
                    ]
                }
            },
            {
                path: 'projectManage',
                name: 'projectManage',
                component: projectManage,
                meta: {
                    breadcrumb: [
                        { name: 'projectManage', label: 'projectManage' }
                    ]
                }
            },
            {
                path: 'projectConfig',
                name: 'projectConfig',
                component: projectConfig,
                meta: {
                    breadcrumb: [
                        { name: 'projectConfig', label: 'projectConfig' }
                    ]
                }
            },
            {
                path: 'repoToken',
                name: 'repoToken',
                component: repoToken,
                meta: {
                    breadcrumb: [
                        { name: 'repoToken', label: 'repoToken' }
                    ]
                }
            },
            {
                path: 'userCenter',
                name: 'userCenter',
                component: userCenter,
                meta: {
                    breadcrumb: [
                        { name: 'userCenter', label: 'userCenter' }
                    ]
                }
            },
            {
                path: 'userManage',
                name: 'userManage',
                component: userManage,
                meta: {
                    breadcrumb: [
                        { name: 'userManage', label: 'userManage' }
                    ]
                }
            },
            {
                path: 'repoAudit',
                name: 'repoAudit',
                component: repoAudit,
                meta: {
                    breadcrumb: [
                        { name: 'repoAudit', label: 'repoAudit' }
                    ]
                }
            },
            {
                path: 'networkConfig',
                name: 'networkConfig',
                component: networkConfig,
                meta: {
                    breadcrumb: [
                        { name: 'networkConfig', label: 'networkConfig' }
                    ]
                }
            },
            {
                path: 'nodeManage',
                name: 'nodeManage',
                component: nodeManage,
                meta: {
                    breadcrumb: [
                        { name: 'nodeManage', label: 'nodeManage' }
                    ]
                }
            },
            {
                path: 'planManage',
                name: 'planManage',
                component: planManage,
                meta: {
                    breadcrumb: [
                        { name: 'planManage', label: 'planManage' }
                    ]
                }
            },
            {
                path: 'planManage/logDetail/:logId',
                name: 'logDetail',
                component: logDetail,
                meta: {
                    breadcrumb: [
                        { name: 'planManage', label: '{planName}', template: 'planManage' },
                        { name: 'logDetail', label: 'logDetail' }
                    ]
                }
            },
            {
                path: 'repoScan',
                name: 'repoScan',
                component: repoScan,
                meta: {
                    breadcrumb: [
                        { name: 'repoScan', label: 'repoScan' }
                    ]
                }
            },
            {
                path: 'scanReport/:planId',
                name: 'scanReport',
                component: scanReport,
                meta: {
                    breadcrumb: [
                        { name: 'repoScan', label: 'repoScan' },
                        { name: 'scanReport', label: '{scanName}', template: 'scanReport' }
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
                            { name: 'repoScan', label: 'repoScan' },
                            { name: 'scanReport', label: '{scanName}', template: 'scanReport' },
                            { name: 'artiReport', label: '{artiName}', template: 'artiReport' }
                        ]
                    } else if (repoType === 'generic') {
                        to.meta.breadcrumb = [
                            { name: 'repoList', label: 'repoList' },
                            { name: 'repoGeneric', label: '{repoName}', template: 'repoGeneric' },
                            { name: 'artiReport', label: 'artiReport' }
                        ]
                    } else if (repoType) {
                        to.meta.breadcrumb = [
                            { name: 'repoList', label: 'repoList' },
                            { name: 'commonList', label: '{repoName}', template: 'commonList' },
                            { name: 'commonPackage', label: '{packageKey}', template: 'commonPackage' },
                            { name: 'artiReport', label: 'artiReport' }
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
                        { name: 'repoScan', label: 'repoScan' },
                        { name: 'scanReport', label: '{scanName}', template: 'scanReport' },
                        { name: 'scanConfig', label: 'scanConfig' }
                    ]
                }
            },
            {
                path: 'startScan/:planId',
                name: 'startScan',
                component: startScan,
                meta: {
                    breadcrumb: [
                        { name: 'repoScan', label: 'repoScan' },
                        { name: 'scanReport', label: '{scanName}', template: 'scanReport' },
                        { name: 'startScan', label: 'scanImmediately' }
                    ]
                }
            },
            {
                path: 'securityConfig',
                name: 'securityConfig',
                component: securityConfig,
                meta: {
                    breadcrumb: [
                        { name: 'securityConfig', label: 'securityConfig' }
                    ]
                }
            },
            {
                path: 'generic',
                name: 'repoGeneric',
                component: repoGeneric,
                meta: {
                    breadcrumb: [
                        { name: 'repoList', label: 'repoList' },
                        { name: 'repoGeneric', label: '{repoName}', template: 'repoGeneric' }
                    ]
                }
            },
            {
                path: ':repoType/list',
                name: 'commonList',
                component: commonPackageList,
                meta: {
                    breadcrumb: [
                        { name: 'repoList', label: 'repoList' },
                        { name: 'commonList', label: '{repoName}', template: 'commonList' }
                    ]
                }
            },
            {
                path: ':repoType/package',
                name: 'commonPackage',
                component: commonPackageDetail,
                meta: {
                    breadcrumb: [
                        { name: 'repoList', label: 'repoList' },
                        { name: 'commonList', label: '{repoName}', template: 'commonList' },
                        { name: 'commonPackage', label: '{packageKey}', template: 'commonPackage' }
                    ]
                }
            }
        ]
    }
]

export default routes
