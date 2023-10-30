// 仓库类型
export const repoEnum = [
    'generic',
    'docker',
    'maven',
    'pypi',
    'npm',
    'helm',
    'composer',
    'rpm',
    // 'git',
    'nuget'
]
// 远程及虚拟仓库支持创建的仓库
export const repoSupportEnum = [
    'maven',
    'npm',
    'pypi',
    'nuget',
    'docker'
]

// 文件类型
export const fileType = [
    'apk',
    'babelrc', 'bat',
    'c', 'cpp', 'css',
    'docx',
    'eslintrc', 'exe',
    'h', 'html',
    'ico',
    'js', 'json',
    'log',
    'md', 'mp4', 'mpd',
    'pdf', 'php', 'png', 'pptx', 'py',
    'sh', 'svg',
    'ts', 'txt',
    'vue',
    'xlsx', 'xmind', 'xml',
    'yaml',
    'zip'
]

export function getIconName (name) {
    let type = name.split('.').pop()
    type = {
        gif: 'png',
        jpg: 'png',
        psd: 'png',
        jpge: 'png',
        mov: 'mp4',
        avi: 'mp4',
        asf: 'mp4',
        wmv: 'mp4',
        rmvb: 'mp4',
        rm: 'mp4',
        jar: 'zip',
        rar: 'zip',
        map: 'js',
        pyc: 'py',
        xsd: 'xml'
    }[type] || type
    return fileType.includes(type) ? type : 'file'
}

// 节点类型
export const nodeTypeEnum = {
    CENTER: '中心节点',
    EDGE: '边缘节点',
    STANDALONE: '独立节点',
    REMOTE: '远程节点'
}

// 同步计划执行状态
export const asyncPlanStatusEnum = {
    RUNNING: '进行中',
    SUCCESS: '已完成',
    FAILED: '同步异常'
}

// 制品分发日志详情执行状态
export const planLogDetailStatusEnum = {
    SUCCESS: '同步成功',
    FAILED: '同步失败',
    RUNNING: '运行中'
}

// 扫描方案类型
export const scanTypeEnum = {
    GENERIC: 'Generic制品分析',
    DOCKER: 'Docker制品分析',
    MAVEN: 'Maven制品分析',
    NPM: 'Npm制品分析',
    PYPI: 'Pypi制品分析'
}

export const SCAN_TYPE_SECURITY = 'SECURITY'
export const SCAN_TYPE_LICENSE = 'LICENSE'
export const SCAN_TYPE_SENSITIVE = 'SENSITIVE'
// 扫描类型
export const scanTypes = {
    [SCAN_TYPE_SECURITY]: {
        key: SCAN_TYPE_SECURITY,
        name: '漏洞扫描'
    },
    [SCAN_TYPE_LICENSE]: {
        key: SCAN_TYPE_LICENSE,
        name: '许可证扫描'
    },
    [SCAN_TYPE_SENSITIVE]: {
        key: SCAN_TYPE_SENSITIVE,
        name: '敏感信息扫描'
    }
}

export const genericScanFileTypes = [
    'zip', 'tar', 'tgz', 'jar', 'war', 'exe',
    'apk', 'ear', 'sar', 'nupkg', 'gz', 'bz2',
    'tbz2', 'rpm'
]

// 扫描方案执行状态
export const scanStatusEnum = {
    INIT: '等待扫描',
    RUNNING: '扫描中',
    STOP: '扫描中止',
    SUCCESS: '扫描完成',
    UN_QUALITY: '未设置质量规则',
    QUALITY_PASS: '质量规则通过',
    QUALITY_UNPASS: '质量规则未通过',
    FAILED: '扫描异常'
}

// 漏洞风险等级
export const leakLevelEnum = {
    CRITICAL: '严重',
    HIGH: '高危',
    MEDIUM: '中危',
    LOW: '低危'
    // WHITE: '白名单'
}
// 制品仓库类型(本地/远程/虚拟)
export const storeTypeEnum = [
    {
        id: 'local',
        name: 'localStore',
        icon: 'local-store',
        info: 'localStoreInfo'
    },
    {
        id: 'remote',
        name: 'remoteStore',
        icon: 'remote-store',
        info: 'remoteStoreInfo'
    },
    {
        id: 'virtual',
        name: 'virtualStore',
        icon: 'virtual-store',
        info: 'virtualStoreInfo'
    }
]

export const planLogEnum = {
    total: '同步总数量',
    success: '成功数量',
    fail: '失败数量',
    conflict: '冲突数量'
}

export const conflictStrategyEnum = {
    SKIP: '跳过冲突',
    OVERWRITE: '替换制品',
    FAST_FAIL: '终止同步'
}

/**
 * 制品搜索方式的下拉框
 */
export const repoSearchConditionMap = [
    {
        id: 'version',
        name: 'searchConditionVersion'
    },
    {
        id: 'checkSum',
        name: 'searchConditionCheckSum'
    },
    {
        id: 'metadata',
        name: 'searchConditionMetadata'
    }
]
