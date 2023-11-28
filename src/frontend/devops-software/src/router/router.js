const repoHome = () => import('@/views')

const repoList = () => import('@/views/repoList')

const repoGeneric = () => import('@repositoryci/views/repoGeneric')

const commonPackageList = () => import('@repository/views/repoCommon/commonPackageList')
const commonPackageDetail = () => import('@repositoryci/views/repoCommon/commonPackageDetail')

const repoSearch = () => import('@/views/repoSearch')

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
            }
        ]
    }
]

export default routes
