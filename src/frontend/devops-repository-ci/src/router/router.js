const repoPreview = () => import('@repository/views/preview')
const scanTask = () => import('@repository/views/preview/scanTask')

const repoHome = () => import('@/views')
const repoList = () => import('@/views/repoList')
const repoConfig = () => import('@/views/repoConfig')
const repoAudit = () => import('@repository/views/repoAudit')
const nodeManage = () => import('@repository/views/nodeManage')
const planManage = () => import('@repository/views/planManage')
// const createPlan = () => import('@repository/views/planManage/createPlan')
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
const repoCatalog = () => import('@repository/views/repoCatalog')

const securityConfig = () => import('@repository/views/repoScan/securityConfig')

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
                path: 'repoList/repoConfig/:repoType',
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
                path: 'repoScan/scanReport/:planId',
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
                path: 'repoScan/artiReport/:planId/:recordId',
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
                path: 'repoScan/scanConfig/:planId',
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
                path: 'repoScan/startScan/:planId',
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
                path: 'repoList/generic',
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
                path: 'repoList/:repoType/list',
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
                path: 'repoList/:repoType/package',
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
