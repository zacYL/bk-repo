const repoHome = () => import('@/views')

const repoList = () => import('@/views/repoList')

const repoGeneric = () => import('@repository/views/repoGeneric')

const commonPackageList = () => import('@repository/views/repoCommon/commonPackageList')
const commonPackageDetail = () => import('@repository/views/repoCommon/commonPackageDetail')

const repoSearch = () => import('@/views/repoSearch')

const userCenter = () => import('@repository/views/userCenter')
const repoToken = () => import('@repository/views/repoToken')

const routes = [
    {
        path: '/software',
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
                path: ':projectId/generic',
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
                path: ':projectId/:repoType/list',
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
                path: ':projectId/:repoType/package',
                name: 'commonPackage',
                component: commonPackageDetail,
                meta: {
                    breadcrumb: [
                        { name: 'repoList', label: 'repoList' },
                        { name: 'commonList', label: '{repoName}', template: 'commonList' },
                        { name: 'commonPackage', label: '{packageKey}', template: 'commonPackage' }
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
            }
        ]
    }
]

export default routes
